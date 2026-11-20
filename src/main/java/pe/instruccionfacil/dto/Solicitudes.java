package pe.instruccionfacil.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.instruccionfacil.entity.Modelo.*;

public final class Solicitudes {
    private Solicitudes() {}

    public record Registro(@NotBlank @Size(max=120) String nombre,
                           @Email @Size(max=254) String email,
                           @Pattern(regexp="\\+?[0-9]{9,15}") String telefono,
                           @NotBlank @Size(min=10, max=72) String password) {}
    public record Login(@NotBlank @Size(max=254) String usuario, @NotBlank @Size(max=72) String password) {}
    public record Personal(@NotNull Rol rol, @NotBlank @Size(max=120) String nombre,
                           @NotBlank @Email @Size(max=254) String email,
                           @NotBlank @Size(min=10,max=72) String password) {}
    public record Perfil(@NotBlank @Size(max=120) String nombre,
                         @Email @Size(max=254) String email,
                         @Pattern(regexp="\\+?[0-9]{9,15}") String telefono,
                         @NotBlank @Pattern(regexp="[0-9]{8}") String dni,
                         @NotNull @Past LocalDate nacimiento,
                         @NotBlank @Size(max=240) String direccion) {}
    public record Tipo(@NotBlank @Size(max=100) String nombre, @NotNull @Min(15) @Max(480) Integer minutos,
                       @NotNull @DecimalMin("0.01") @Digits(integer=8, fraction=2) BigDecimal tarifa,
                       @NotNull Boolean activo) {}
    public record Flota(@NotBlank @Pattern(regexp="[A-Za-z0-9-]{5,12}") String placa,
                        @NotBlank @Size(max=140) String descripcion,
                        @NotNull Transmision transmision, @NotNull Boolean activo) {}
    public record Ventana(@NotNull LocalDateTime inicio, @NotNull LocalDateTime fin) {}
    public record BloqueoVehiculo(@NotNull LocalDateTime inicio, @NotNull LocalDateTime fin,
                                  @NotBlank @Size(max=200) String motivo) {}
    public record Reserva(@Positive Long alumnoId, @NotNull @Positive Long instructorId,
                          @NotNull @Positive Long tipoClaseId, @NotNull LocalDateTime inicio,
                          @NotNull Transmision transmision, Canal canal, @NotNull Boolean vehiculoPropio,
                          @Pattern(regexp="[A-Za-z0-9-]{5,12}") String placaPropia,
                          @Size(max=70) String marcaPropia, @Size(max=70) String modeloPropio) {}
    public record Reprogramacion(@NotNull @Positive Long instructorId, @NotNull LocalDateTime inicio) {}
    public record CambioEstado(@NotNull EstadoCita estado, @Size(max=240) String motivo) {}
    public record Revision(@NotNull Boolean aprobar, @Size(max=240) String motivo) {}
}
