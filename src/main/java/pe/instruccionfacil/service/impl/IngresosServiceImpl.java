package pe.instruccionfacil.service.impl;

import pe.instruccionfacil.service.service.*;
import pe.instruccionfacil.exception.ApiException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.enums.MedioPago;
import pe.instruccionfacil.repository.CheckoutRepository;
import pe.instruccionfacil.repository.PagoRepository;

@Service
public class IngresosServiceImpl implements IngresosService {
    private record Resumen(BigDecimal monto, long pagos) {
        Resumen sumar(Resumen otro) { return new Resumen(monto.add(otro.monto), pagos + otro.pagos); }
    }

    private final PagoRepository pagos;
    private final CheckoutRepository checkout;

    public IngresosServiceImpl(PagoRepository pagos, CheckoutRepository checkout) {
        this.pagos = pagos;
        this.checkout = checkout;
    }

    @Transactional(readOnly = true)
    public IngresosResponseDTO consultar(LocalDate desde, LocalDate hasta) {
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
        return new IngresosResponseDTO(total.monto(), total.pagos(),
            dias.entrySet().stream().map(e -> new IngresoDiarioResponseDTO(e.getKey(), e.getValue().monto(), e.getValue().pagos())).toList(),
            medios.entrySet().stream().map(e -> new IngresoMedioResponseDTO(e.getKey(), e.getValue().monto(), e.getValue().pagos())).toList(),
            checkout.incidencias().stream().map(i ->
                new IncidenciaPagoResponseDTO(i.citaId(), i.ordenExterna(), i.incidencia(), i.actualizado())).toList());
    }
}
