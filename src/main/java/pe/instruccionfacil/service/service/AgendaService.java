package pe.instruccionfacil.service.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import pe.instruccionfacil.dto.DisponibilidadResponseDTO;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.enums.Canal;
import pe.instruccionfacil.enums.EstadoCita;
import pe.instruccionfacil.enums.MedioPago;
import pe.instruccionfacil.enums.EstadoPago;
import pe.instruccionfacil.entity.*;
import pe.instruccionfacil.enums.*;
import pe.instruccionfacil.enums.Rol;
import pe.instruccionfacil.enums.Transmision;

public interface AgendaService {
    List<DisponibilidadResponseDTO> disponibilidad(long instructor, long claseId, LocalDate dia, Transmision transmision,
                                              boolean vehiculoPropio);

    List<CitaResponseDTO> listar(long usuario, LocalDate desde, LocalDate hasta);

    CitaResponseDTO consultar(long usuario, long id);

    ContactoInstructorResponseDTO contactoInstructor(long usuario, long id);

    Cita autorizada(long usuario, long id);

    CitaResponseDTO reservar(long actorId, ReservaRequestDTO datos);

    CitaResponseDTO reprogramar(long actor, long id, ReprogramacionRequestDTO datos);

    CitaResponseDTO cambiarEstado(long actor, long id, CambioEstadoRequestDTO datos);

    void vencerPendientes();
}
