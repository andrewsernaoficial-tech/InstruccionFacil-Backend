package pe.instruccionfacil.service.impl;

import pe.instruccionfacil.service.service.*;
import pe.instruccionfacil.exception.ApiException;
import pe.instruccionfacil.mapper.MapeoRespuestas;

import java.time.Clock;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.entity.*;
import pe.instruccionfacil.enums.*;
import pe.instruccionfacil.enums.Rol;
import pe.instruccionfacil.repository.*;

@Service
public class CatalogoServiceImpl implements CatalogoService {
    private final CatalogoRepository catalogo;
    private final AgendaRepository agenda;
    private final UsuarioRepository usuarios;
    private final UsuarioService perfiles;
    private final AuditoriaRepository auditoria;
    private final Clock reloj;

    public CatalogoServiceImpl(CatalogoRepository catalogo, AgendaRepository agenda, UsuarioRepository usuarios,
                           UsuarioService perfiles, AuditoriaRepository auditoria, Clock reloj) {
        this.catalogo = catalogo;
        this.agenda = agenda;
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public List<TipoClaseResponseDTO> clases(boolean incluirInactivas) {
        return catalogo.clases(incluirInactivas).stream().map(MapeoRespuestas::tipoClase).toList();
    }

    public List<EventoResponseDTO> eventosRecientes() {
        return auditoria.recientes().stream().map(MapeoRespuestas::evento).toList();
    }

    public TipoClase clase(long id) {
        return catalogo.clase(id).orElseThrow(() -> ApiException.noEncontrado("Tipo de clase no encontrado"));
    }

    @Transactional
    public TipoClaseResponseDTO guardarClase(long actor, Long id, TipoRequestDTO datos) {
        agenda.bloquearCambios();
        if (id == null) {
            long nuevo = catalogo.crearClase(datos.nombre().trim(), datos.minutos(), datos.tarifa(), datos.activo());
            auditoria.registrar(actor, "TIPO_CLASE", nuevo, "CREADO", null);
            return MapeoRespuestas.tipoClase(clase(nuevo));
        }
        clase(id);
        catalogo.editarClase(id, datos.nombre().trim(), datos.minutos(), datos.tarifa(), datos.activo());
        auditoria.registrar(actor, "TIPO_CLASE", id, "ACTUALIZADO", null);
        return MapeoRespuestas.tipoClase(clase(id));
    }

    @Transactional
    public void borrarClase(long actor, long id) {
        agenda.bloquearCambios();
        clase(id);
        boolean archivada = catalogo.borrarClase(id);
        auditoria.registrar(actor, "TIPO_CLASE", id, "ELIMINADO", archivada ? "Con historial de citas" : null);
    }

    public List<VehiculoResponseDTO> flota() {
        return catalogo.flota().stream().map(MapeoRespuestas::vehiculo).toList();
    }

    private Vehiculo vehiculo(long id) {
        return catalogo.vehiculo(id).orElseThrow(() -> ApiException.noEncontrado("Vehículo no encontrado"));
    }

    @Transactional
    public VehiculoResponseDTO guardarVehiculo(long actor, Long id, FlotaRequestDTO datos) {
        agenda.bloquearCambios();
        String placa = datos.placa().toUpperCase(Locale.ROOT);
        var ahora = LocalDateTime.now(reloj);
        if (id == null) {
            validarPlacaFlota(placa, ahora);
            long nuevo = catalogo.crearVehiculo(placa, datos.descripcion().trim(), datos.transmision(), datos.activo());
            auditoria.registrar(actor, "VEHICULO", nuevo, "CREADO", null);
            return MapeoRespuestas.vehiculo(vehiculo(nuevo));
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
        catalogo.editarVehiculo(id, placa, datos.descripcion().trim(), datos.transmision(), datos.activo());
        auditoria.registrar(actor, "VEHICULO", id, "ACTUALIZADO", null);
        return MapeoRespuestas.vehiculo(vehiculo(id));
    }

    @Transactional
    public void retirarVehiculo(long actor, long id) {
        agenda.bloquearCambios();
        vehiculo(id);
        if (agenda.vehiculoTieneCitas(id, LocalDateTime.now(reloj))) {
            throw ApiException.conflicto("El vehículo tiene citas activas. Reprograma esas citas antes de retirarlo");
        }
        catalogo.retirarVehiculo(id);
        auditoria.registrar(actor, "VEHICULO", id, "RETIRADO", null);
    }

    private void validarPlacaFlota(String placa, LocalDateTime ahora) {
        if (agenda.placaPropiaOcupada(placa, ahora, LocalDateTime.of(9999, 1, 1, 0, 0), ahora, 0)) {
            throw ApiException.conflicto("La placa está registrada en una cita activa como vehículo propio");
        }
    }

    public List<InstructorResponseDTO> instructores() {
        return usuarios.instructores().stream().map(MapeoRespuestas::instructor).toList();
    }

    public void validarInstructor(long id) {
        if (perfiles.obtener(id).rol() != Rol.INSTRUCTOR || !usuarios.estaHabilitado(id)) {
            throw ApiException.invalido("Selecciona una cuenta con rol INSTRUCTOR");
        }
    }

    public List<HorarioResponseDTO> horarios(long instructor, LocalDate dia) {
        return ventanasInstructor(instructor, dia).stream().map(MapeoRespuestas::horario).toList();
    }

    public List<Horario> ventanasInstructor(long instructor, LocalDate dia) {
        validarInstructor(instructor);
        return catalogo.horarios(instructor, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }

    public List<Horario> franjasContinuas(long instructor, LocalDateTime inicio, LocalDateTime fin) {
        var franjas = new ArrayList<Horario>();
        for (var horario : catalogo.horarios(instructor, inicio, fin)) {
            if (franjas.isEmpty() || horario.inicio().isAfter(franjas.getLast().fin())) {
                franjas.add(horario);
            } else if (horario.fin().isAfter(franjas.getLast().fin())) {
                var anterior = franjas.removeLast();
                franjas.add(new Horario(anterior.id(), instructor, anterior.inicio(), horario.fin()));
            }
        }
        return franjas;
    }

    @Transactional
    public HorarioResponseDTO crearHorario(long instructor, VentanaRequestDTO datos) {
        agenda.bloquearCambios();
        validarInstructor(instructor);
        intervalo(datos.inicio(), datos.fin());
        if (!datos.inicio().toLocalDate().equals(datos.fin().minusNanos(1).toLocalDate())) {
            throw ApiException.invalido("Registra cada horario dentro de un solo día");
        }
        if (!catalogo.horarios(instructor, datos.inicio(), datos.fin()).isEmpty()) {
            throw ApiException.conflicto("El instructor ya tiene un horario en ese intervalo");
        }
        long id = catalogo.crearHorario(instructor, datos.inicio(), datos.fin());
        auditoria.registrar(instructor, "HORARIO", id, "CREADO", null);
        return MapeoRespuestas.horario(catalogo.horario(id).orElseThrow());
    }

    @Transactional
    public void borrarHorario(long actor, long id) {
        agenda.bloquearCambios();
        var horario = catalogo.horario(id).orElseThrow(() -> ApiException.noEncontrado("Horario no encontrado"));
        if (horario.instructorId() != actor) {
            throw ApiException.noEncontrado("Horario no encontrado");
        }
        if (agenda.instructorOcupado(horario.instructorId(), horario.inicio(), horario.fin(),
            LocalDateTime.now(reloj), 0)) {
            throw ApiException.conflicto("El horario ya tiene citas activas");
        }
        catalogo.borrarHorario(id);
        auditoria.registrar(actor, "HORARIO", id, "ELIMINADO", null);
    }

    public List<BloqueoResponseDTO> bloqueos(long vehiculo) {
        vehiculo(vehiculo);
        return catalogo.bloqueos(vehiculo, LocalDateTime.now(reloj)).stream().map(MapeoRespuestas::bloqueo).toList();
    }

    @Transactional
    public BloqueoResponseDTO crearBloqueo(long actor, long vehiculo, BloqueoVehiculoRequestDTO datos) {
        agenda.bloquearCambios();
        vehiculo(vehiculo);
        intervalo(datos.inicio(), datos.fin());
        if (agenda.vehiculoOcupado(vehiculo, datos.inicio(), datos.fin(), LocalDateTime.now(reloj))) {
            throw ApiException.conflicto("El vehículo ya tiene citas en ese intervalo");
        }
        String motivo = datos.motivo().trim();
        long id = catalogo.crearBloqueo(vehiculo, datos.inicio(), datos.fin(), motivo);
        auditoria.registrar(actor, "BLOQUEO_VEHICULO", id, "CREADO", motivo);
        return MapeoRespuestas.bloqueo(catalogo.bloqueo(id).orElseThrow());
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
