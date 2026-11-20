package pe.instruccionfacil.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.exception.ApiException;
import pe.instruccionfacil.repository.*;

@Service
public class CatalogoService {
    private final CatalogoRepository catalogo;
    private final AgendaRepository agenda;
    private final UsuarioRepository usuarios;
    private final UsuarioService perfiles;
    private final AuditoriaRepository auditoria;
    private final Clock reloj;

    public CatalogoService(CatalogoRepository catalogo, AgendaRepository agenda, UsuarioRepository usuarios,
                           UsuarioService perfiles, AuditoriaRepository auditoria, Clock reloj) {
        this.catalogo = catalogo;
        this.agenda = agenda;
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public List<TipoClase> clases(boolean incluirInactivas) {
        return catalogo.clases(incluirInactivas);
    }

    public TipoClase clase(long id) {
        return catalogo.clase(id).orElseThrow(() -> ApiException.noEncontrado("Tipo de clase no encontrado"));
    }

    @Transactional
    public TipoClase guardarClase(long actor, Long id, Solicitudes.Tipo datos) {
        agenda.bloquearCambios();
        if (id == null) {
            long nuevo = catalogo.crearClase(datos);
            auditoria.registrar(actor, "TIPO_CLASE", nuevo, "CREADO", null);
            return clase(nuevo);
        }
        clase(id);
        catalogo.editarClase(id, datos);
        auditoria.registrar(actor, "TIPO_CLASE", id, "ACTUALIZADO", null);
        return clase(id);
    }

    public List<Vehiculo> flota() {
        return catalogo.flota();
    }

    private Vehiculo vehiculo(long id) {
        return catalogo.vehiculo(id).orElseThrow(() -> ApiException.noEncontrado("Vehículo no encontrado"));
    }

    @Transactional
    public Vehiculo guardarVehiculo(long actor, Long id, Solicitudes.Flota datos) {
        agenda.bloquearCambios();
        String placa = datos.placa().toUpperCase(Locale.ROOT);
        var ahora = LocalDateTime.now(reloj);
        if (id == null) {
            validarPlacaFlota(placa, ahora);
            long nuevo = catalogo.crearVehiculo(placa, datos);
            auditoria.registrar(actor, "VEHICULO", nuevo, "CREADO", null);
            return vehiculo(nuevo);
        }
        Vehiculo actual = vehiculo(id);
        boolean cambioPlaca = !placa.equals(actual.placa());
        if ((cambioPlaca || !datos.activo() || datos.transmision() != actual.transmision())
            && agenda.vehiculoTieneCitas(id, ahora)) {
            throw ApiException.conflicto("El vehículo tiene citas activas");
        }
        if (cambioPlaca) {
            validarPlacaFlota(placa, ahora);
        }
        catalogo.editarVehiculo(id, placa, datos);
        auditoria.registrar(actor, "VEHICULO", id, "ACTUALIZADO", null);
        return vehiculo(id);
    }

    private void validarPlacaFlota(String placa, LocalDateTime ahora) {
        if (agenda.placaPropiaOcupada(placa, ahora, LocalDateTime.of(9999, 1, 1, 0, 0), ahora, 0)) {
            throw ApiException.conflicto("La placa está registrada en una cita activa como vehículo propio");
        }
    }

    public List<Respuestas.Instructor> instructores() {
        return usuarios.instructores();
    }

    public void validarInstructor(long id) {
        if (perfiles.obtener(id).rol() != Rol.INSTRUCTOR) {
            throw ApiException.invalido("Selecciona una cuenta con rol INSTRUCTOR");
        }
    }

    public List<Horario> horarios(long instructor, LocalDate dia) {
        validarInstructor(instructor);
        return catalogo.horarios(instructor, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }

    @Transactional
    public Horario crearHorario(long actor, long instructor, Solicitudes.Ventana datos) {
        agenda.bloquearCambios();
        validarInstructor(instructor);
        intervalo(datos.inicio(), datos.fin());
        if (!datos.inicio().toLocalDate().equals(datos.fin().minusNanos(1).toLocalDate())) {
            throw ApiException.invalido("Registra cada horario dentro de un solo día");
        }
        if (!catalogo.horarios(instructor, datos.inicio(), datos.fin()).isEmpty()) {
            throw ApiException.conflicto("El instructor ya tiene un horario en ese intervalo");
        }
        long id = catalogo.crearHorario(instructor, datos);
        auditoria.registrar(actor, "HORARIO", id, "CREADO", null);
        return catalogo.horario(id).orElseThrow();
    }

    @Transactional
    public void borrarHorario(long actor, long id) {
        agenda.bloquearCambios();
        var horario = catalogo.horario(id).orElseThrow(() -> ApiException.noEncontrado("Horario no encontrado"));
        if (agenda.instructorOcupado(horario.instructorId(), horario.inicio(), horario.fin(),
            LocalDateTime.now(reloj), 0)) {
            throw ApiException.conflicto("El horario ya tiene citas activas");
        }
        catalogo.borrarHorario(id);
        auditoria.registrar(actor, "HORARIO", id, "ELIMINADO", null);
    }

    public List<Bloqueo> bloqueos(long vehiculo) {
        vehiculo(vehiculo);
        return catalogo.bloqueos(vehiculo, LocalDateTime.now(reloj));
    }

    @Transactional
    public Bloqueo crearBloqueo(long actor, long vehiculo, Solicitudes.BloqueoVehiculo datos) {
        agenda.bloquearCambios();
        vehiculo(vehiculo);
        intervalo(datos.inicio(), datos.fin());
        if (agenda.vehiculoOcupado(vehiculo, datos.inicio(), datos.fin(), LocalDateTime.now(reloj))) {
            throw ApiException.conflicto("El vehículo ya tiene citas en ese intervalo");
        }
        long id = catalogo.crearBloqueo(vehiculo, datos);
        auditoria.registrar(actor, "BLOQUEO_VEHICULO", id, "CREADO", datos.motivo().trim());
        return catalogo.bloqueo(id).orElseThrow();
    }

    @Transactional
    public void borrarBloqueo(long actor, long id) {
        agenda.bloquearCambios();
        catalogo.bloqueo(id).orElseThrow(() -> ApiException.noEncontrado("Bloqueo no encontrado"));
        catalogo.borrarBloqueo(id);
        auditoria.registrar(actor, "BLOQUEO_VEHICULO", id, "ELIMINADO", null);
    }

    private void intervalo(LocalDateTime inicio, LocalDateTime fin) {
        if (fin.isBefore(LocalDateTime.now(reloj)) || !inicio.isBefore(fin)) {
            throw ApiException.invalido("Indica un intervalo vigente con un fin posterior al inicio");
        }
    }
}
