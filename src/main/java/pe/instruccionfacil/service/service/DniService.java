package pe.instruccionfacil.service.service;

import java.text.Normalizer;
import java.util.Locale;

public interface DniService {
    void verificar(String dni, String nombreCompleto);
}
