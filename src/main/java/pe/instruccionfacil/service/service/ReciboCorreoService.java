package pe.instruccionfacil.service.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.enums.EstadoPago;
import pe.instruccionfacil.entity.Cita;
import pe.instruccionfacil.entity.Pago;

public interface ReciboCorreoService {
    boolean habilitado();

    void programar(Pago pago, Cita cita);

    EnvioCorreoResponseDTO solicitar(long actor, long pagoId);
    void entregarPendientes();
}
