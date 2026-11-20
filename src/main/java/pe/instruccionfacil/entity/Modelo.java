package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Modelo {
    private Modelo() {}

    public enum Rol { CLIENTE, INSTRUCTOR, RECEPCION, ADMIN }
    public enum Transmision { MECANICA, AUTOMATICA }
    public enum Canal { WEB, PRESENCIAL, WHATSAPP, FACEBOOK, LLAMADA }
    public enum EstadoCita { PENDIENTE_PAGO, CONFIRMADA, EN_CURSO, COMPLETADA, CANCELADA, NO_ASISTIO, VENCIDA }
    public enum MedioPago { YAPE, PLIN, TRANSFERENCIA }
    public enum EstadoPago { EN_REVISION, APROBADO, RECHAZADO }

    public record Usuario(long id, String nombre, String email, String telefono, Rol rol,
                          String passwordHash, int intentos, LocalDateTime bloqueadoHasta,
                          String dni, LocalDate nacimiento, String direccion) {}
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
    public record Comprobante(byte[] contenido, String mime) {}
    public record Evento(long id, Long actorId, String entidad, long entidadId, String accion,
                         String detalle, LocalDateTime fecha) {}
}
