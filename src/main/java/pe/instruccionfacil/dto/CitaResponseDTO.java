package pe.instruccionfacil.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import pe.instruccionfacil.enums.Canal;
import pe.instruccionfacil.enums.EstadoCita;
import pe.instruccionfacil.enums.EstadoPago;
import pe.instruccionfacil.enums.MedioPago;
import pe.instruccionfacil.enums.Rol;
import pe.instruccionfacil.enums.Transmision;

public record CitaResponseDTO(long id, long alumnoId, long instructorId, long tipoClaseId, Long vehiculoId,
                   String placaPropia, String marcaPropia, String modeloPropio, Transmision transmision, Canal canal,
                   LocalDateTime inicio, LocalDateTime fin, LocalDateTime vence, BigDecimal monto,
                   EstadoCita estado, String motivo, String nombreClase, String nombreInstructor) {}
