package pe.instruccionfacil.service.service;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.enums.EstadoCita;
import pe.instruccionfacil.enums.EstadoPago;
import pe.instruccionfacil.enums.MedioPago;
import pe.instruccionfacil.entity.Cita;

public interface PasarelaService {
    boolean habilitada();

    boolean modoPrueba();

    boolean enCurso(long citaId, LocalDateTime ahora);

    EnlaceCheckoutResponseDTO abrir(long actor, long citaId);

    VerificacionCheckoutResponseDTO estadoCheckout(long actor, long citaId);

    VerificacionCheckoutResponseDTO cancelarCheckout(long actor, long citaId);

    void notificacion(String id, String requestId, String firma);

    VerificacionCheckoutResponseDTO verificarPagoPrueba(long actor, long citaId);
}
