package pe.instruccionfacil.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VencimientoReservas {
    private final AgendaService agenda;

    public VencimientoReservas(AgendaService agenda) {
        this.agenda = agenda;
    }

    @Scheduled(fixedDelay = 30000)
    void actualizar() {
        agenda.vencerPendientes();
    }
}
