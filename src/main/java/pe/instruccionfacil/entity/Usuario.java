package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record Usuario(long id, String nombre, String email, String telefono, Rol rol,
                      String passwordHash, int intentos, LocalDateTime bloqueadoHasta,
                      String dni, LocalDate nacimiento, String direccion) {}
