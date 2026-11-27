package pe.instruccionfacil.service.impl;

import pe.instruccionfacil.service.service.*;
import pe.instruccionfacil.exception.ApiException;
import pe.instruccionfacil.mapper.MapeoRespuestas;
import pe.instruccionfacil.util.ReciboPdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.enums.EstadoCita;
import pe.instruccionfacil.enums.EstadoPago;
import pe.instruccionfacil.enums.MedioPago;
import pe.instruccionfacil.entity.*;
import pe.instruccionfacil.enums.*;
import pe.instruccionfacil.repository.*;

@Service
public class PagoServiceImpl implements PagoService {
    private final AgendaService citas;
    private final AgendaRepository agenda;
    private final PagoRepository pagos;
    private final AuditoriaRepository auditoria;
    private final Clock reloj;
    private final UsuarioService usuarios;
    private final List<MedioDisponibleResponseDTO> medios;
    private final PasarelaService pasarela;
    private final ReciboCorreoService correo;

    public PagoServiceImpl(AgendaService citas, AgendaRepository agenda, PagoRepository pagos,
                       AuditoriaRepository auditoria, Clock reloj, UsuarioService usuarios, PasarelaService pasarela, ReciboCorreoService correo,
                       @Value("${app.pago-plin:}") String plin,
                       @Value("${app.pago-transferencia:}") String transferencia) {
        this.citas = citas;
        this.agenda = agenda;
        this.pagos = pagos;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.usuarios = usuarios;
        this.pasarela = pasarela;
        this.correo = correo;
        var configurados = new ArrayList<MedioDisponibleResponseDTO>();
        if (!plin.isBlank()) {
            configurados.add(new MedioDisponibleResponseDTO(MedioPago.PLIN, plin));
        }
        if (!transferencia.isBlank()) {
            configurados.add(new MedioDisponibleResponseDTO(MedioPago.TRANSFERENCIA, transferencia));
        }
        this.medios = List.copyOf(configurados);
    }

    public List<MedioDisponibleResponseDTO> medios() {
        var configurados = new ArrayList<>(medios);
        if (configurados.isEmpty()) {
            configurados.add(new MedioDisponibleResponseDTO(MedioPago.PLIN, ""));
            configurados.add(new MedioDisponibleResponseDTO(MedioPago.TRANSFERENCIA, ""));
        }
        configurados.add(new MedioDisponibleResponseDTO(MedioPago.EFECTIVO, "Paga al llegar a la escuela."));
        if (pasarela.habilitada()) {
            boolean prueba = pasarela.modoPrueba();
            String datos = prueba ? "Usa una tarjeta de prueba. Luego vuelve a la agenda y verifica el pago."
                : "Paga en el sitio de Mercado Pago.";
            configurados.addFirst(new MedioDisponibleResponseDTO(MedioPago.MERCADO_PAGO, datos, prueba));
        }
        return configurados;
    }

    public List<PagoResponseDTO> historial(long actor, long cita) {
        citas.autorizada(actor, cita);
        return pagos.deCita(cita).stream().map(MapeoRespuestas::pago).toList();
    }

    public List<PagoResponseDTO> pendientes() {
        return pagos.pendientes(LocalDateTime.now(reloj)).stream().map(MapeoRespuestas::pago).toList();
    }

    @Transactional
    public PagoResponseDTO subir(long actor, long citaId, CargaComprobanteRequestDTO datos) {
        MedioPago medio = datos.medio();
        if (medio == MedioPago.MERCADO_PAGO) throw ApiException.invalido("Inicia el pago desde el checkout");
        if (medios().stream().noneMatch(configurado -> configurado.medio() == medio)) {
            throw ApiException.invalido("El medio de pago no está habilitado");
        }
        boolean efectivo = medio == MedioPago.EFECTIVO;
        String referencia = efectivo ? "CAJA-" + java.util.UUID.randomUUID()
            : datos.operacion() == null ? "" : datos.operacion().trim().toUpperCase(Locale.ROOT);
        if (referencia.isEmpty() || referencia.length() > 60 || !referencia.matches("[A-Za-z0-9._/-]+")) {
            throw ApiException.invalido("Ingresa un número de operación válido de hasta 60 caracteres");
        }
        var comprobante = efectivo ? null : validar(datos.archivo());
        agenda.bloquearCambios();
        var cita = citas.autorizada(actor, citaId);
        var ahora = LocalDateTime.now(reloj);
        if (!AgendaServiceImpl.pendienteVigente(cita, ahora) || !cita.inicio().isAfter(ahora)) {
            throw ApiException.conflicto("La cita no admite comprobantes o venció el plazo de pago");
        }
        if (pasarela.enCurso(citaId, ahora)) {
            throw ApiException.conflicto("La cita tiene un pago de Mercado Pago en curso");
        }
        if (pagos.hayPagoEnRevision(citaId)) {
            throw ApiException.conflicto("Esta cita ya tiene un pago en revisión");
        }
        var pago = pagos.crear(cita, medio, referencia, comprobante, ahora);
        if (efectivo) {
            agenda.cambiarVencimiento(citaId, cita.fin());
        }
        auditoria.registrar(actor, "PAGO", pago.id(), "EN_REVISION", null);
        return MapeoRespuestas.pago(pago);
    }

    @Transactional
    public PagoResponseDTO revisar(long actor, long id, RevisionRequestDTO datos) {
        agenda.bloquearCambios();
        var pago = pago(id);
        var cita = citas.autorizada(actor, pago.citaId());
        var ahora = LocalDateTime.now(reloj);
        if (pago.estado() != EstadoPago.EN_REVISION || !AgendaServiceImpl.pendienteVigente(cita, ahora)) {
            throw ApiException.conflicto("El comprobante ya fue revisado o la reserva dejó de estar vigente");
        }
        String motivo = datos.motivo() == null || datos.motivo().isBlank() ? null : datos.motivo().trim();
        if (!datos.aprobar() && motivo == null) {
            throw ApiException.invalido("Indica el motivo de rechazo");
        }
        EstadoPago estado = datos.aprobar() ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
        pagos.revisar(id, estado, actor, motivo, ahora);
        auditoria.registrar(actor, "PAGO", id, estado.name(), motivo);
        if (!datos.aprobar() && pago.medio() == MedioPago.EFECTIVO) {
            agenda.cambiarVencimiento(cita.id(), cita.inicio());
        }
        if (datos.aprobar()) {
            agenda.cambiarEstado(cita.id(), EstadoCita.CONFIRMADA, null);
            auditoria.registrar(actor, "CITA", cita.id(), "CONFIRMADA", null);
            correo.programar(pago, cita);
        }
        return MapeoRespuestas.pago(pago(id));
    }

    public ArchivoComprobanteResponseDTO descargar(long actor, long id) {
        citas.autorizada(actor, pago(id).citaId());
        return pagos.comprobante(id).map(MapeoRespuestas::comprobante)
            .orElseThrow(() -> ApiException.noEncontrado("Comprobante no encontrado"));
    }

    public byte[] recibo(long actor, long id) {
        var pago = pago(id);
        var cita = citas.autorizada(actor, pago.citaId());
        if (pago.estado() != EstadoPago.APROBADO) {
            throw ApiException.conflicto("El recibo estará disponible cuando se apruebe el pago");
        }
        var alumno = usuarios.obtener(cita.alumnoId());
        return ReciboPdf.crear(pago, alumno.nombre());
    }

    private Pago pago(long id) {
        return pagos.buscar(id).orElseThrow(() -> ApiException.noEncontrado("Pago no encontrado"));
    }

    private static Comprobante validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > 5L * 1024 * 1024) {
            throw ApiException.invalido("Carga un comprobante de hasta 5 MB");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw ApiException.invalido("No se pudo leer el comprobante");
        }
        String mime;
        if (comienzaCon(contenido, new byte[]{(byte)255, (byte)216, (byte)255})) {
            mime = "image/jpeg";
        } else if (comienzaCon(contenido, new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10})) {
            mime = "image/png";
        } else if (comienzaCon(contenido, "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
            mime = "application/pdf";
        } else {
            throw ApiException.invalido("El comprobante debe ser JPG, PNG o PDF");
        }
        return new Comprobante(contenido, mime);
    }

    private static boolean comienzaCon(byte[] contenido, byte[] firma) {
        return contenido.length >= firma.length
            && java.util.Arrays.equals(contenido, 0, firma.length, firma, 0, firma.length);
    }
}
