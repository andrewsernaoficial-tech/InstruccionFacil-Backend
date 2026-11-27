package pe.instruccionfacil.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;
import pe.instruccionfacil.enums.Canal;
import pe.instruccionfacil.enums.EstadoCita;
import pe.instruccionfacil.enums.MedioPago;
import pe.instruccionfacil.enums.Rol;
import pe.instruccionfacil.enums.Transmision;

public record EdicionPersonalRequestDTO(@NotBlank @Size(max=120) String nombre,
                              @NotBlank @Email @Size(max=254) String email,
                              @Pattern(regexp="\\+?[0-9]{9,15}") String telefono,
                              @Pattern(regexp="[0-9]{8}") String dni,
                              @Past LocalDate nacimiento, @Size(max=240) String direccion) {}
