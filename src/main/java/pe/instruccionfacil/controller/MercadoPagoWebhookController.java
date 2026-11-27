package pe.instruccionfacil.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.instruccionfacil.service.service.PasarelaService;

@RestController
public class MercadoPagoWebhookController {
    private final PasarelaService pasarela;

    public MercadoPagoWebhookController(PasarelaService pasarela) { this.pasarela = pasarela; }

    @PostMapping("/api/v1/webhooks/mercado-pago")
    void recibir(@RequestParam(name = "data.id", required = false) String id,
                 @RequestHeader(name = "x-request-id", required = false) String requestId,
                 @RequestHeader(name = "x-signature", required = false) String firma) {
        pasarela.notificacion(id, requestId, firma);
    }
}
