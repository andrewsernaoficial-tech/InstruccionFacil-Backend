package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record TipoClase(long id, String nombre, int minutos, BigDecimal tarifa, boolean activo) {}
