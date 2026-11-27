package pe.instruccionfacil.service.service;

import java.time.Clock;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.entity.*;
import pe.instruccionfacil.enums.*;
import pe.instruccionfacil.enums.Rol;

public interface CatalogoService {
    List<TipoClaseResponseDTO> clases(boolean incluirInactivas);

    List<EventoResponseDTO> eventosRecientes();

    TipoClase clase(long id);

    TipoClaseResponseDTO guardarClase(long actor, Long id, TipoRequestDTO datos);

    void borrarClase(long actor, long id);

    List<VehiculoResponseDTO> flota();

    VehiculoResponseDTO guardarVehiculo(long actor, Long id, FlotaRequestDTO datos);

    void retirarVehiculo(long actor, long id);

    List<InstructorResponseDTO> instructores();

    void validarInstructor(long id);

    List<HorarioResponseDTO> horarios(long instructor, LocalDate dia);

    List<Horario> ventanasInstructor(long instructor, LocalDate dia);

    List<Horario> franjasContinuas(long instructor, LocalDateTime inicio, LocalDateTime fin);

    HorarioResponseDTO crearHorario(long instructor, VentanaRequestDTO datos);

    void borrarHorario(long actor, long id);

    List<BloqueoResponseDTO> bloqueos(long vehiculo);

    BloqueoResponseDTO crearBloqueo(long actor, long vehiculo, BloqueoVehiculoRequestDTO datos);

    void borrarBloqueo(long actor, long id);
}
