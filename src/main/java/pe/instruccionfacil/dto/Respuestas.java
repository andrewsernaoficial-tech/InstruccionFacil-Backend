package pe.instruccionfacil.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import pe.instruccionfacil.domain.Canal;
import pe.instruccionfacil.domain.EstadoCita;
import pe.instruccionfacil.domain.EstadoPago;
import pe.instruccionfacil.domain.MedioPago;
import pe.instruccionfacil.domain.Rol;
import pe.instruccionfacil.domain.Transmision;

public final class Respuestas {
    private Respuestas() {}

    public record Usuario(long id, String nombre, String email, String telefono, Rol rol,
                          String dni, LocalDate nacimiento, String direccion) {}
    public record Sesion(String accessToken, String tokenType, long expiresIn, Usuario usuario) {}
    public record Cliente(long id, String nombre) {}
    public record Instructor(long id, String nombre) {}
    public record Disponibilidad(long instructorId, LocalDateTime inicio, LocalDateTime fin, boolean flotaDisponible) {}

    public record TipoClase(long id, String nombre, int minutos, BigDecimal tarifa, boolean activo) {}
    public record Vehiculo(long id, String placa, String descripcion, Transmision transmision, boolean activo) {}
    public record Horario(long id, long instructorId, LocalDateTime inicio, LocalDateTime fin) {}
    public record Bloqueo(long id, long vehiculoId, LocalDateTime inicio, LocalDateTime fin, String motivo) {}
    public record Cita(long id, long alumnoId, long instructorId, long tipoClaseId, Long vehiculoId,
                       String placaPropia, String marcaPropia, String modeloPropio, Transmision transmision, Canal canal,
                       LocalDateTime inicio, LocalDateTime fin, LocalDateTime vence, BigDecimal monto,
                       EstadoCita estado, String motivo) {}
    public record Pago(long id, long citaId, MedioPago medio, String operacion, BigDecimal monto,
                       EstadoPago estado, LocalDateTime creado, LocalDateTime revisado, Long revisorId, String motivo) {}
    public record MedioDisponible(MedioPago medio, String datos) {}
    public record ArchivoComprobante(byte[] contenido, String mime) {}
    public record Evento(long id, Long actorId, String entidad, long entidadId, String accion,
                         String detalle, LocalDateTime fecha) {}
    public record EstadoSalud(String status) {}
    public record ErrorApi(String mensaje, List<String> campos) {}
}
