package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record Cita(long id, long alumnoId, long instructorId, long tipoClaseId, Long vehiculoId,
                   String placaPropia, String marcaPropia, String modeloPropio, Transmision transmision, Canal canal,
                   LocalDateTime inicio, LocalDateTime fin, LocalDateTime vence, BigDecimal monto,
                   EstadoCita estado, String motivo, String nombreClase, String nombreInstructor) {}
