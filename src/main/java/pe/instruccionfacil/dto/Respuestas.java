package pe.instruccionfacil.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.instruccionfacil.entity.Modelo.Rol;

public final class Respuestas {
    private Respuestas() {}

    public record Usuario(long id, String nombre, String email, String telefono, Rol rol,
                          String dni, LocalDate nacimiento, String direccion) {}
    public record Sesion(String accessToken, String tokenType, long expiresIn, Usuario usuario) {}
    public record Cliente(long id, String nombre) {}
    public record Instructor(long id, String nombre) {}
    public record Disponibilidad(long instructorId, LocalDateTime inicio, LocalDateTime fin, boolean flotaDisponible) {}
}
