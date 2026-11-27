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

public record ReprogramacionRequestDTO(@NotNull @Positive Long instructorId, @NotNull LocalDateTime inicio) {}
