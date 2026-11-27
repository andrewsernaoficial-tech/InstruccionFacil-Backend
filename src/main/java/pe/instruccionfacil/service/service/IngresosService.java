package pe.instruccionfacil.service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.enums.MedioPago;

public interface IngresosService {
    IngresosResponseDTO consultar(LocalDate desde, LocalDate hasta);
}
