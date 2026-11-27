package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record Vehiculo(long id, String placa, String descripcion, Transmision transmision, boolean activo) {}
