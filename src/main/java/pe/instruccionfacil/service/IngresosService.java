package pe.instruccionfacil.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.entity.MedioPago;
import pe.instruccionfacil.repository.CheckoutRepository;
import pe.instruccionfacil.repository.PagoRepository;

@Service
public class IngresosService {
    private record Resumen(BigDecimal monto, long pagos) {
        Resumen sumar(Resumen otro) { return new Resumen(monto.add(otro.monto), pagos + otro.pagos); }
    }

    private final PagoRepository pagos;
    private final CheckoutRepository checkout;

    public IngresosService(PagoRepository pagos, CheckoutRepository checkout) {
        this.pagos = pagos;
        this.checkout = checkout;
    }

    @Transactional(readOnly = true)
    public Respuestas.Ingresos consultar(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde) || ChronoUnit.DAYS.between(desde, hasta) > 365) {
            throw ApiException.invalido("Selecciona un rango de hasta 366 días");
        }
        var cero = new Resumen(BigDecimal.ZERO, 0);
        Map<LocalDate, Resumen> dias = new TreeMap<>();
        desde.datesUntil(hasta.plusDays(1)).forEach(dia -> dias.put(dia, cero));
        Map<MedioPago, Resumen> medios = new EnumMap<>(MedioPago.class);
        var total = cero;
        for (var grupo : pagos.ingresos(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())) {
            var monto = new Resumen(grupo.monto(), grupo.pagos());
            dias.merge(grupo.fecha(), monto, Resumen::sumar);
            medios.merge(grupo.medio(), monto, Resumen::sumar);
            total = total.sumar(monto);
        }
        return new Respuestas.Ingresos(total.monto(), total.pagos(),
            dias.entrySet().stream().map(e -> new Respuestas.IngresoDiario(e.getKey(), e.getValue().monto(), e.getValue().pagos())).toList(),
            medios.entrySet().stream().map(e -> new Respuestas.IngresoMedio(e.getKey(), e.getValue().monto(), e.getValue().pagos())).toList(),
            checkout.incidencias().stream().map(i ->
                new Respuestas.IncidenciaPago(i.citaId(), i.ordenExterna(), i.incidencia(), i.actualizado())).toList());
    }
}
