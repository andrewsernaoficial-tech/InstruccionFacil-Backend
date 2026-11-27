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

public record ReservaRequestDTO(@Positive Long alumnoId, @NotNull @Positive Long instructorId,
                      @NotNull @Positive Long tipoClaseId, @NotNull LocalDateTime inicio,
                      @NotNull Transmision transmision, Canal canal, @NotNull Boolean vehiculoPropio,
                      @Pattern(regexp="[A-Za-z0-9-]{5,12}") String placaPropia,
                      @Size(max=70) String marcaPropia, @Size(max=70) String modeloPropio) {}
