package pe.instruccionfacil.service;

import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.entity.Modelo;

final class MapeoRespuestas {
    private MapeoRespuestas() {}

    static Respuestas.Usuario usuario(Modelo.Usuario usuario) {
        return new Respuestas.Usuario(usuario.id(), usuario.nombre(), usuario.email(), usuario.telefono(), usuario.rol(),
            usuario.dni(), usuario.nacimiento(), usuario.direccion());
    }

    static Respuestas.Instructor instructor(Modelo.Instructor instructor) {
        return new Respuestas.Instructor(instructor.id(), instructor.nombre());
    }

    static Respuestas.TipoClase tipoClase(Modelo.TipoClase clase) {
        return new Respuestas.TipoClase(clase.id(), clase.nombre(), clase.minutos(), clase.tarifa(), clase.activo());
    }

    static Respuestas.Vehiculo vehiculo(Modelo.Vehiculo vehiculo) {
        return new Respuestas.Vehiculo(vehiculo.id(), vehiculo.placa(), vehiculo.descripcion(), vehiculo.transmision(),
            vehiculo.activo());
    }

    static Respuestas.Horario horario(Modelo.Horario horario) {
        return new Respuestas.Horario(horario.id(), horario.instructorId(), horario.inicio(), horario.fin());
    }

    static Respuestas.Bloqueo bloqueo(Modelo.Bloqueo bloqueo) {
        return new Respuestas.Bloqueo(bloqueo.id(), bloqueo.vehiculoId(), bloqueo.inicio(), bloqueo.fin(), bloqueo.motivo());
    }

    static Respuestas.Cita cita(Modelo.Cita cita) {
        return new Respuestas.Cita(cita.id(), cita.alumnoId(), cita.instructorId(), cita.tipoClaseId(), cita.vehiculoId(),
            cita.placaPropia(), cita.marcaPropia(), cita.modeloPropio(), cita.transmision(), cita.canal(),
            cita.inicio(), cita.fin(), cita.vence(), cita.monto(), cita.estado(), cita.motivo());
    }

    static Respuestas.Pago pago(Modelo.Pago pago) {
        return new Respuestas.Pago(pago.id(), pago.citaId(), pago.medio(), pago.operacion(), pago.monto(),
            pago.estado(), pago.creado(), pago.revisado(), pago.revisorId(), pago.motivo());
    }

    static Respuestas.ArchivoComprobante comprobante(Modelo.Comprobante comprobante) {
        return new Respuestas.ArchivoComprobante(comprobante.contenido(), comprobante.mime());
    }

    static Respuestas.Evento evento(Modelo.Evento evento) {
        return new Respuestas.Evento(evento.id(), evento.actorId(), evento.entidad(), evento.entidadId(),
            evento.accion(), evento.detalle(), evento.fecha());
    }
}
