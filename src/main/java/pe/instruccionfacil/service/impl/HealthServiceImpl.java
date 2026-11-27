package pe.instruccionfacil.service.impl;

import pe.instruccionfacil.service.service.*;

import org.springframework.stereotype.Service;
import pe.instruccionfacil.repository.HealthRepository;

@Service
public class HealthServiceImpl implements HealthService {
    private final HealthRepository salud;

    public HealthServiceImpl(HealthRepository salud) {
        this.salud = salud;
    }

    public void verificarConexion() {
        salud.verificarConexion();
    }
}
