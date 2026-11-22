package pe.instruccionfacil.service;

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
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.entity.EstadoCita;
import pe.instruccionfacil.entity.EstadoPago;
import pe.instruccionfacil.entity.MedioPago;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.repository.*;

@Service
public class PagoService {
    private final AgendaService citas;
    private final AgendaRepository agenda;
    private final PagoRepository pagos;
    private final AuditoriaRepository auditoria;
    private final Clock reloj;
    private final UsuarioService usuarios;
    private final List<Respuestas.MedioDisponible> medios;

    public PagoService(AgendaService citas, AgendaRepository agenda, PagoRepository pagos,
                       AuditoriaRepository auditoria, Clock reloj, UsuarioService usuarios,
                       @Value("${app.pago-yape:}") String yape,
                       @Value("${app.pago-plin:}") String plin,
                       @Value("${app.pago-transferencia:}") String transferencia) {
        this.citas = citas;
        this.agenda = agenda;
        this.pagos = pagos;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.usuarios = usuarios;
        var configurados = new ArrayList<Respuestas.MedioDisponible>();
        if (!yape.isBlank()) {
            configurados.add(new Respuestas.MedioDisponible(MedioPago.YAPE, yape, false));
        }
        if (!plin.isBlank()) {
            configurados.add(new Respuestas.MedioDisponible(MedioPago.PLIN, plin, false));
        }
        if (!transferencia.isBlank()) {
            configurados.add(new Respuestas.MedioDisponible(MedioPago.TRANSFERENCIA, transferencia, false));
        }
        this.medios = List.copyOf(configurados);
    }

    public List<Respuestas.MedioDisponible> medios() {
        var configurados = new ArrayList<>(medios);
        var yape = yape();
        if (yape.numero() != null) {
            configurados.removeIf(item -> item.medio() == MedioPago.YAPE);
            configurados.addFirst(new Respuestas.MedioDisponible(MedioPago.YAPE, yape.numero(), yape.tieneQr()));
        }
        if (configurados.isEmpty()) {
            for (var medio : List.of(MedioPago.YAPE, MedioPago.PLIN, MedioPago.TRANSFERENCIA)) {
                configurados.add(new Respuestas.MedioDisponible(medio, "", false));
            }
        }
        configurados.add(new Respuestas.MedioDisponible(MedioPago.EFECTIVO, "Paga al llegar a la escuela.", false));
        return configurados;
    }

    public Respuestas.Yape yape() {
        var configuracion = pagos.configuracionYape().orElseThrow();
        return new Respuestas.Yape(configuracion.numero(), configuracion.tieneQr());
    }

    @Transactional
    public Respuestas.Yape guardarYape(String numero, MultipartFile qr) {
        if (numero == null || !numero.matches("9[0-9]{8}")) {
            throw ApiException.invalido("Ingresa un número Yape de 9 dígitos que empiece por 9");
        }
        var imagen = qr == null ? null : validar(qr);
        if (imagen != null && !imagen.mime().startsWith("image/")) {
            throw ApiException.invalido("Sube el QR de Yape como imagen JPG o PNG");
        }
        pagos.guardarYape(numero, imagen);
        return yape();
    }

    public Respuestas.ArchivoComprobante qrYape() {
        return pagos.qrYape().map(MapeoRespuestas::comprobante)
            .orElseThrow(() -> ApiException.noEncontrado("QR de Yape no configurado"));
    }

    public List<Respuestas.Pago> historial(long actor, long cita) {
        citas.autorizada(actor, cita);
        return pagos.deCita(cita).stream().map(MapeoRespuestas::pago).toList();
    }

    public List<Respuestas.Pago> pendientes() {
        return pagos.pendientes(LocalDateTime.now(reloj)).stream().map(MapeoRespuestas::pago).toList();
    }

    @Transactional
    public Respuestas.Pago subir(long actor, long citaId, Solicitudes.CargaComprobante datos) {
        MedioPago medio = datos.medio();
        boolean efectivo = medio == MedioPago.EFECTIVO;
        String referencia = efectivo ? "CAJA-" + java.util.UUID.randomUUID()
            : datos.operacion() == null ? "" : datos.operacion().trim().toUpperCase(Locale.ROOT);
        if (referencia.isEmpty() || referencia.length() > 60 || !referencia.matches("[A-Za-z0-9._/-]+")) {
            throw ApiException.invalido("Ingresa un número de operación válido de hasta 60 caracteres");
        }
        if (medios().stream().noneMatch(configurado -> configurado.medio() == medio)) {
            throw ApiException.invalido("El medio de pago no está habilitado");
        }
        var comprobante = efectivo ? null : validar(datos.archivo());
        agenda.bloquearCambios();
        var cita = citas.autorizada(actor, citaId);
        var ahora = LocalDateTime.now(reloj);
        if (!AgendaService.pendienteVigente(cita, ahora) || !cita.inicio().isAfter(ahora)) {
            throw ApiException.conflicto("La cita no admite comprobantes o venció el plazo de pago");
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
    public Respuestas.Pago revisar(long actor, long id, Solicitudes.Revision datos) {
        agenda.bloquearCambios();
        var pago = pago(id);
        var cita = citas.autorizada(actor, pago.citaId());
        var ahora = LocalDateTime.now(reloj);
        if (pago.estado() != EstadoPago.EN_REVISION || !AgendaService.pendienteVigente(cita, ahora)) {
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
        }
        return MapeoRespuestas.pago(pago(id));
    }

    public Respuestas.ArchivoComprobante descargar(long actor, long id) {
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
