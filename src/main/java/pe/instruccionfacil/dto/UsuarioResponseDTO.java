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

public record UsuarioResponseDTO(long id, String nombre, String email, String telefono, Rol rol,
                      String dni, LocalDate nacimiento, String direccion) {}
