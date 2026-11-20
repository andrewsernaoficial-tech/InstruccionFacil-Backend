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
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.exception.ApiException;
import pe.instruccionfacil.repository.*;

@Service
public class PagoService {
    public record MedioDisponible(MedioPago medio, String datos) {}

    private final AgendaService citas;
    private final AgendaRepository agenda;
    private final PagoRepository pagos;
    private final AuditoriaRepository auditoria;
    private final Clock reloj;
    private final List<MedioDisponible> medios;

    public PagoService(AgendaService citas, AgendaRepository agenda, PagoRepository pagos,
                       AuditoriaRepository auditoria, Clock reloj,
                       @Value("${app.pago-yape:}") String yape,
                       @Value("${app.pago-plin:}") String plin,
                       @Value("${app.pago-transferencia:}") String transferencia) {
        this.citas = citas;
        this.agenda = agenda;
        this.pagos = pagos;
        this.auditoria = auditoria;
        this.reloj = reloj;
        var configurados = new ArrayList<MedioDisponible>();
        if (!yape.isBlank()) {
            configurados.add(new MedioDisponible(MedioPago.YAPE, yape));
        }
        if (!plin.isBlank()) {
            configurados.add(new MedioDisponible(MedioPago.PLIN, plin));
        }
        if (!transferencia.isBlank()) {
            configurados.add(new MedioDisponible(MedioPago.TRANSFERENCIA, transferencia));
        }
        this.medios = List.copyOf(configurados);
    }

    public List<MedioDisponible> medios() {
        return medios;
    }

    public List<Pago> historial(long actor, long cita) {
        citas.autorizada(actor, cita);
        return pagos.deCita(cita);
    }

    public List<Pago> pendientes() {
        return pagos.pendientes(LocalDateTime.now(reloj));
    }

    @Transactional
    public Pago subir(long actor, long citaId, MedioPago medio, String operacion, MultipartFile archivo) {
        String referencia = operacion.trim().toUpperCase(Locale.ROOT);
        if (referencia.isEmpty() || referencia.length() > 60 || !referencia.matches("[A-Z0-9._/-]+")) {
            throw ApiException.invalido("Ingresa un número de operación válido de hasta 60 caracteres");
        }
        if (!medios.isEmpty() && medios.stream().noneMatch(configurado -> configurado.medio() == medio)) {
            throw ApiException.invalido("El medio de pago no está habilitado");
        }
        var comprobante = validar(archivo);
        agenda.bloquearCambios();
        var cita = citas.autorizada(actor, citaId);
        var ahora = LocalDateTime.now(reloj);
        if (!AgendaService.pendienteVigente(cita, ahora)) {
            throw ApiException.conflicto("La cita no admite comprobantes o venció el plazo de pago");
        }
        if (pagos.hayPagoEnRevision(citaId)) {
            throw ApiException.conflicto("Esta cita ya tiene un comprobante en revisión");
        }
        var pago = pagos.crear(cita, medio, referencia, comprobante, ahora);
        auditoria.registrar(actor, "PAGO", pago.id(), "EN_REVISION", null);
        return pago;
    }

    @Transactional
    public Pago revisar(long actor, long id, Solicitudes.Revision datos) {
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
        if (datos.aprobar()) {
            agenda.cambiarEstado(cita.id(), EstadoCita.CONFIRMADA, null);
            auditoria.registrar(actor, "CITA", cita.id(), "CONFIRMADA", null);
        }
        return pago(id);
    }

    public Comprobante descargar(long actor, long id) {
        citas.autorizada(actor, pago(id).citaId());
        return pagos.comprobante(id).orElseThrow(() -> ApiException.noEncontrado("Comprobante no encontrado"));
    }

    private Pago pago(long id) {
        return pagos.buscar(id).orElseThrow(() -> ApiException.noEncontrado("Pago no encontrado"));
    }

    private static Comprobante validar(MultipartFile archivo) {
        if (archivo.isEmpty() || archivo.getSize() > 5L * 1024 * 1024) {
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
