package pe.instruccionfacil.mapper;

import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.entity.*;
import pe.instruccionfacil.enums.*;

public final class MapeoRespuestas {
    private MapeoRespuestas() {}

    public static UsuarioResponseDTO usuario(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.id(), usuario.nombre(), usuario.email(), usuario.telefono(), usuario.rol(),
            usuario.dni(), usuario.nacimiento(), usuario.direccion());
    }

    public static InstructorResponseDTO instructor(Instructor instructor) {
        return new InstructorResponseDTO(instructor.id(), instructor.nombre());
    }

    public static TipoClaseResponseDTO tipoClase(TipoClase clase) {
        return new TipoClaseResponseDTO(clase.id(), clase.nombre(), clase.minutos(), clase.tarifa(), clase.activo());
    }

    public static VehiculoResponseDTO vehiculo(Vehiculo vehiculo) {
        return new VehiculoResponseDTO(vehiculo.id(), vehiculo.placa(), vehiculo.descripcion(), vehiculo.transmision(),
            vehiculo.activo());
    }

    public static HorarioResponseDTO horario(Horario horario) {
        return new HorarioResponseDTO(horario.id(), horario.instructorId(), horario.inicio(), horario.fin());
    }

    public static BloqueoResponseDTO bloqueo(Bloqueo bloqueo) {
        return new BloqueoResponseDTO(bloqueo.id(), bloqueo.vehiculoId(), bloqueo.inicio(), bloqueo.fin(), bloqueo.motivo());
    }

    public static CitaResponseDTO cita(Cita cita) {
        return new CitaResponseDTO(cita.id(), cita.alumnoId(), cita.instructorId(), cita.tipoClaseId(), cita.vehiculoId(),
            cita.placaPropia(), cita.marcaPropia(), cita.modeloPropio(), cita.transmision(), cita.canal(),
            cita.inicio(), cita.fin(), cita.vence(), cita.monto(), cita.estado(), cita.motivo(), cita.nombreClase(), cita.nombreInstructor());
    }

    public static PagoResponseDTO pago(Pago pago) {
        return new PagoResponseDTO(pago.id(), pago.citaId(), pago.medio(), pago.operacion(), pago.monto(),
            pago.estado(), pago.creado(), pago.revisado(), pago.revisorId(), pago.motivo());
    }

    public static ArchivoComprobanteResponseDTO comprobante(Comprobante comprobante) {
        return new ArchivoComprobanteResponseDTO(comprobante.contenido(), comprobante.mime());
    }

    public static EventoResponseDTO evento(Evento evento) {
        return new EventoResponseDTO(evento.id(), evento.actorId(), evento.entidad(), evento.entidadId(),
            evento.accion(), evento.detalle(), evento.fecha());
    }
}
