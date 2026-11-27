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

public record TipoRequestDTO(@NotBlank @Size(max=100) String nombre, @NotNull @Min(15) @Max(480) Integer minutos,
                   @NotNull @DecimalMin("0.01") @Digits(integer=8, fraction=2) BigDecimal tarifa,
                   @NotNull Boolean activo) {}
