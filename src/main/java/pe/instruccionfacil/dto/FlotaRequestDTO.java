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

public record FlotaRequestDTO(@NotBlank @Pattern(regexp="[A-Za-z0-9-]{5,12}") String placa,
                    @NotBlank @Size(max=140) String descripcion,
                    @NotNull Transmision transmision, @NotNull Boolean activo) {}
