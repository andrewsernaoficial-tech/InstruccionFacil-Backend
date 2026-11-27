package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record Pago(long id, long citaId, MedioPago medio, String operacion, BigDecimal monto,
                   EstadoPago estado, LocalDateTime creado, LocalDateTime revisado, Long revisorId, String motivo) {}
