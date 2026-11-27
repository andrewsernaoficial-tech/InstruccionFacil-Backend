package pe.instruccionfacil.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.instruccionfacil.enums.*;

public record Evento(long id, Long actorId, String entidad, long entidadId, String accion,
                     String detalle, LocalDateTime fecha) {}
