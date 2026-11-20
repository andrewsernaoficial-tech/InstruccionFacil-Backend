package pe.instruccionfacil.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.dto.Respuestas.Disponibilidad;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.exception.ApiException;
import pe.instruccionfacil.repository.*;

@Service
public class AgendaService {
    private final AgendaRepository agenda;
    private final CatalogoRepository catalogo;
    private final CatalogoService configuracion;
    private final UsuarioService usuarios;
    private final PagoRepository pagos;
    private final AuditoriaRepository auditoria;
    private final Clock reloj;
    private final int vigencia;

    public AgendaService(AgendaRepository agenda, CatalogoRepository catalogo, CatalogoService configuracion,
                         UsuarioService usuarios, PagoRepository pagos, AuditoriaRepository auditoria, Clock reloj,
                         @Value("${app.reservation-minutes}") int vigencia) {
        if (vigencia < 1 || vigencia > 1440) {
            throw new IllegalArgumentException("APP_RESERVATION_MINUTES debe estar entre 1 y 1440");
        }
        this.agenda = agenda;
        this.catalogo = catalogo;
        this.configuracion = configuracion;
        this.usuarios = usuarios;
        this.pagos = pagos;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.vigencia = vigencia;
    }

    @Transactional(readOnly = true)
    public List<Disponibilidad> disponibilidad(long instructor, long claseId, LocalDate dia, Transmision transmision,
                                              boolean vehiculoPropio) {
        var clase = configuracion.clase(claseId);
        if (!clase.activo()) {
            throw ApiException.conflicto("El tipo de clase está inactivo");
        }
        var ahora = LocalDateTime.now(reloj);
        var disponibles = new ArrayList<Disponibilidad>();
        for (var ventana : configuracion.horarios(instructor, dia)) {
            LocalDateTime inicio = ventana.inicio().truncatedTo(ChronoUnit.HOURS)
                .plusMinutes((ventana.inicio().getMinute() / 15) * 15L);
            if (inicio.isBefore(ventana.inicio())) {
                inicio = inicio.plusMinutes(15);
            }
            for (; !inicio.plusMinutes(clase.minutos()).isAfter(ventana.fin()); inicio = inicio.plusMinutes(15)) {
                var fin = inicio.plusMinutes(clase.minutos());
                if (!inicio.isAfter(ahora) || agenda.instructorOcupado(instructor, inicio, fin, ahora, 0)) {
                    continue;
                }
                boolean flotaDisponible = agenda.vehiculoLibre(transmision, inicio, fin, ahora, 0).isPresent();
                if (vehiculoPropio || flotaDisponible) {
                    disponibles.add(new Disponibilidad(instructor, inicio, fin, flotaDisponible));
                }
            }
        }
        return disponibles;
    }

    public List<Cita> listar(long usuario, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde) || ChronoUnit.DAYS.between(desde, hasta) > 92) {
            throw ApiException.invalido("Selecciona un rango de hasta 93 días");
        }
        return agenda.listar(usuarios.obtener(usuario), desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    public Cita autorizada(long usuario, long id) {
        var persona = usuarios.obtener(usuario);
        var cita = agenda.buscar(id).orElseThrow(() -> ApiException.noEncontrado("Cita no encontrada"));
        boolean visible = switch (persona.rol()) {
            case CLIENTE -> cita.alumnoId() == usuario;
            case INSTRUCTOR -> cita.instructorId() == usuario;
            case RECEPCION, ADMIN -> true;
        };
        if (!visible) {
            throw ApiException.noEncontrado("Cita no encontrada");
        }
        return cita;
    }

    @Transactional
    public Cita reservar(long actorId, Solicitudes.Reserva datos) {
        agenda.bloquearCambios();
        var actor = usuarios.obtener(actorId);
        if (actor.rol() == Rol.CLIENTE && datos.alumnoId() != null && datos.alumnoId() != actorId) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes reservar a tu nombre");
        }
        long alumno = datos.alumnoId() == null ? actorId : datos.alumnoId();
        var perfil = usuarios.obtener(alumno);
        if (perfil.rol() != Rol.CLIENTE) {
            throw ApiException.invalido("La cita debe estar a nombre de un cliente registrado");
        }

        var clase = configuracion.clase(datos.tipoClaseId());
        if (!clase.activo()) {
            throw ApiException.conflicto("El tipo de clase está inactivo");
        }
        String placa = datos.placaPropia() == null ? null : datos.placaPropia().toUpperCase(Locale.ROOT);
        String marca = datos.marcaPropia() == null ? null : datos.marcaPropia().trim();
        String modelo = datos.modeloPropio() == null ? null : datos.modeloPropio().trim();
        if (datos.vehiculoPropio() && (placa == null || marca == null || modelo == null || marca.isBlank() || modelo.isBlank())) {
            throw ApiException.invalido("Para vehículo propio, indica placa, marca y modelo");
        }
        if (!datos.vehiculoPropio() && (placa != null || marca != null || modelo != null)) {
            throw ApiException.invalido("Para usar un vehículo de la escuela, deja vacíos los datos del vehículo propio");
        }

        var ahora = LocalDateTime.now(reloj);
        var fin = datos.inicio().plusMinutes(clase.minutos());
        var vehiculo = verificarRecursos(alumno, datos.instructorId(), placa, datos.transmision(),
            datos.inicio(), fin, ahora, 0);
        Canal canal = actor.rol() == Rol.CLIENTE || datos.canal() == null ? Canal.WEB : datos.canal();
        var vencimiento = ahora.plusMinutes(vigencia);
        if (datos.inicio().isBefore(vencimiento)) {
            vencimiento = datos.inicio();
        }
        var cita = agenda.crear(alumno, datos.instructorId(), clase, vehiculo, placa, marca, modelo, datos.transmision(),
            canal, datos.inicio(), fin, vencimiento);
        auditoria.registrar(actorId, "CITA", cita.id(), "PENDIENTE_PAGO", null);
        return cita;
    }

    @Transactional
    public Cita reprogramar(long actor, long id, Solicitudes.Reprogramacion datos) {
        agenda.bloquearCambios();
        var cita = autorizada(actor, id);
        var ahora = LocalDateTime.now(reloj);
        if (!cita.inicio().isAfter(ahora)
            || (cita.estado() != EstadoCita.CONFIRMADA && !pendienteVigente(cita, ahora))) {
            throw ApiException.conflicto("Solo puedes reprogramar citas vigentes que aún no comienzan");
        }
        if (cita.estado() == EstadoCita.PENDIENTE_PAGO && datos.inicio().isBefore(cita.vence())) {
            throw ApiException.conflicto("El nuevo horario debe empezar después del plazo de pago");
        }
        var fin = datos.inicio().plus(Duration.between(cita.inicio(), cita.fin()));
        var vehiculo = verificarRecursos(cita.alumnoId(), datos.instructorId(), cita.placaPropia(),
            cita.transmision(), datos.inicio(), fin, ahora, cita.id());
        agenda.reprogramar(id, datos.instructorId(), vehiculo, datos.inicio(), fin);
        auditoria.registrar(actor, "CITA", id, "REPROGRAMADA", cita.inicio() + " → " + datos.inicio());
        return agenda.buscar(id).orElseThrow();
    }

    private Long verificarRecursos(long alumno, long instructor, String placa, Transmision transmision,
                                   LocalDateTime inicio, LocalDateTime fin, LocalDateTime ahora, long excluirCita) {
        if (!inicio.isAfter(ahora) || inicio.getMinute() % 15 != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw ApiException.invalido("Selecciona un inicio futuro en intervalos de 15 minutos");
        }
        configuracion.validarInstructor(instructor);
        if (!catalogo.cubreHorario(instructor, inicio, fin) || agenda.instructorOcupado(instructor, inicio, fin, ahora, excluirCita)) {
            throw ApiException.conflicto("El instructor no está disponible en ese horario");
        }
        if (agenda.alumnoOcupado(alumno, inicio, fin, ahora, excluirCita)) {
            throw ApiException.conflicto("El alumno ya tiene una cita en ese horario");
        }
        if (placa != null) {
            if (catalogo.flota().stream().anyMatch(v -> v.placa().equals(placa))) {
                throw ApiException.invalido("Esa placa pertenece a la flota de la escuela");
            }
            if (agenda.placaPropiaOcupada(placa, inicio, fin, ahora, excluirCita)) {
                throw ApiException.conflicto("La placa ya está asignada en ese horario");
            }
            return null;
        }
        return agenda.vehiculoLibre(transmision, inicio, fin, ahora, excluirCita)
            .orElseThrow(() -> ApiException.conflicto("No hay un vehículo de esa transmisión disponible"));
    }

    @Transactional
    public Cita cambiarEstado(long actor, long id, Solicitudes.CambioEstado datos) {
        agenda.bloquearCambios();
        var cita = autorizada(actor, id);
        var usuario = usuarios.obtener(actor);
        var ahora = LocalDateTime.now(reloj);
        String motivo = datos.motivo() == null || datos.motivo().isBlank() ? null : datos.motivo().trim();

        if (datos.estado() == EstadoCita.CANCELADA) {
            if (usuario.rol() == Rol.INSTRUCTOR) {
                throw new ApiException(HttpStatus.FORBIDDEN, "El instructor no puede cancelar citas");
            }
            if (motivo == null) {
                throw ApiException.invalido("Indica el motivo de cancelación");
            }
            if (!cita.inicio().isAfter(ahora)
                || (cita.estado() != EstadoCita.CONFIRMADA && !pendienteVigente(cita, ahora))) {
                throw ApiException.conflicto("La cita ya no se puede cancelar");
            }
            pagos.cerrarPendientes(id, actor, "Cita cancelada", ahora);
        } else {
            if (usuario.rol() == Rol.CLIENTE) {
                throw new ApiException(HttpStatus.FORBIDDEN, "El cliente solo puede cancelar una cita");
            }
            boolean permitido = switch (datos.estado()) {
                case EN_CURSO, NO_ASISTIO -> cita.estado() == EstadoCita.CONFIRMADA && !ahora.isBefore(cita.inicio());
                case COMPLETADA -> cita.estado() == EstadoCita.EN_CURSO;
                default -> false;
            };
            if (!permitido) {
                throw ApiException.conflicto("Transición no permitida para el estado y horario de esta cita");
            }
        }
        agenda.cambiarEstado(id, datos.estado(), motivo);
        auditoria.registrar(actor, "CITA", id, datos.estado().name(), motivo);
        return agenda.buscar(id).orElseThrow();
    }

    public static boolean pendienteVigente(Cita cita, LocalDateTime ahora) {
        return cita.estado() == EstadoCita.PENDIENTE_PAGO && cita.vence().isAfter(ahora);
    }

    @Transactional
    public void vencerPendientes() {
        agenda.bloquearCambios();
        var ahora = LocalDateTime.now(reloj);
        for (long id : agenda.vencidas(ahora)) {
            agenda.cambiarEstado(id, EstadoCita.VENCIDA, "Plazo de pago vencido");
            pagos.cerrarPendientes(id, null, "Plazo de pago vencido", ahora);
            auditoria.registrar(null, "CITA", id, "VENCIDA", null);
        }
    }
}
