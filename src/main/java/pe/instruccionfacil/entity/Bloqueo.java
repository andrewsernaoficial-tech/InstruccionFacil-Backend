package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record Bloqueo(long id, long vehiculoId, LocalDateTime inicio, LocalDateTime fin, String motivo) {}
