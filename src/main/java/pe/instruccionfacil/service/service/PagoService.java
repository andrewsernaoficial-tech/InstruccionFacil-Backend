package pe.instruccionfacil.service.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.enums.EstadoCita;
import pe.instruccionfacil.enums.EstadoPago;
import pe.instruccionfacil.enums.MedioPago;
import pe.instruccionfacil.entity.*;
import pe.instruccionfacil.enums.*;

public interface PagoService {
    List<MedioDisponibleResponseDTO> medios();

    List<PagoResponseDTO> historial(long actor, long cita);

    List<PagoResponseDTO> pendientes();

    PagoResponseDTO subir(long actor, long citaId, CargaComprobanteRequestDTO datos);

    PagoResponseDTO revisar(long actor, long id, RevisionRequestDTO datos);

    ArchivoComprobanteResponseDTO descargar(long actor, long id);

    byte[] recibo(long actor, long id);
}
