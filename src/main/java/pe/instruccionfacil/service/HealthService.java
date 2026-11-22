package pe.instruccionfacil.service;

import org.springframework.stereotype.Service;
import pe.instruccionfacil.repository.HealthRepository;

@Service
public class HealthService {
    private final HealthRepository salud;

    public HealthService(HealthRepository salud) {
        this.salud = salud;
    }

    public void verificarConexion() {
        salud.verificarConexion();
    }
}
