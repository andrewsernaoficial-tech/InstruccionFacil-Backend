package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record Horario(long id, long instructorId, LocalDateTime inicio, LocalDateTime fin) {}
