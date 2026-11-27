package pe.instruccionfacil.service;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.entity.EstadoCita;
import pe.instruccionfacil.entity.EstadoPago;
import pe.instruccionfacil.entity.MedioPago;
import pe.instruccionfacil.entity.Modelo.Cita;
import pe.instruccionfacil.repository.AgendaRepository;
import pe.instruccionfacil.repository.AuditoriaRepository;
import pe.instruccionfacil.repository.CheckoutRepository;
import pe.instruccionfacil.repository.CheckoutRepository.Checkout;
import pe.instruccionfacil.repository.PagoRepository;
import tools.jackson.databind.JsonNode;

@Service
public class PasarelaService {
    private final RestClient http;
    private final AgendaRepository agenda;
    private final AgendaService citas;
    private final PagoRepository pagos;
    private final CheckoutRepository checkout;
    private final ReciboCorreoService correo;
    private final AuditoriaRepository auditoria;
    private final Clock reloj;
    private final TransactionTemplate transaccion;
    private final String apiUrl;
    private final String token;
    private final String secreto;
    private final String frontend;
    private final boolean modoPrueba;

    public PasarelaService(RestClient http, AgendaRepository agenda, AgendaService citas, PagoRepository pagos,
            CheckoutRepository checkout, ReciboCorreoService correo, AuditoriaRepository auditoria, Clock reloj,
            PlatformTransactionManager tx, @Value("${app.mp-api-url}") String apiUrl,
            @Value("${app.mp-token}") String token, @Value("${app.mp-webhook-secret}") String secreto,
            @Value("${app.mp-frontend-url}") String frontend, @Value("${app.mp-modo-prueba}") boolean modoPrueba) {
        this.http = http;
        this.agenda = agenda;
        this.citas = citas;
        this.pagos = pagos;
        this.checkout = checkout;
        this.correo = correo;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.transaccion = new TransactionTemplate(tx);
        this.apiUrl = apiUrl;
        this.token = token;
        this.secreto = secreto;
        this.frontend = frontend.replaceFirst("/+$", "");
        this.modoPrueba = modoPrueba;
    }

    public boolean habilitada() {
        return !token.isBlank() && (modoPrueba || !secreto.isBlank() && frontend.startsWith("https://"));
    }

    public boolean modoPrueba() { return modoPrueba; }

    public boolean enCurso(long citaId, LocalDateTime ahora) {
        return checkout.porCita(citaId, false).filter(item -> item.abierto(ahora)).isPresent();
    }

    public Respuestas.EnlaceCheckout abrir(long actor, long citaId) {
        if (!habilitada()) throw noDisponible();
        var cita = transaccion.execute(tx -> {
            agenda.bloquearCambios();
            Cita actual = citas.autorizada(actor, citaId);
            var ahora = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
            if (!AgendaService.pendienteVigente(actual, ahora) || !actual.inicio().isAfter(ahora)) {
                throw ApiException.conflicto("La reserva ya no admite pagos");
            }
            if (pagos.hayPagoEnRevision(citaId)) throw ApiException.conflicto("Ya hay un pago manual en revisión");
            if (checkout.porCita(citaId, false).isEmpty()) {
                checkout.crear(citaId, UUID.randomUUID().toString(), ahora, actual.vence().truncatedTo(ChronoUnit.SECONDS));
            }
            return actual;
        });
        return transaccion.execute(tx -> {
            agenda.bloquearCambios();
            var intento = checkout.porCita(citaId, true).orElseThrow();
            if (!intento.abierto(LocalDateTime.now(reloj))) {
                throw ApiException.conflicto("El intento de Mercado Pago terminó. Elige otro medio si la reserva sigue vigente");
            }
            if (intento.enlace() != null) {
                comprobarEntorno(intento.ordenExterna());
                return new Respuestas.EnlaceCheckout(intento.enlace());
            }
            var creado = recuperarOrden(cita, intento);
            return new Respuestas.EnlaceCheckout(creado.enlace());
        });
    }

    // Reutiliza la clave original incluso si la respuesta inicial se perdió.
    private Checkout recuperarOrden(Cita cita, Checkout intento) {
        var orden = crearOrden(cita, intento);
        String id = orden.path("id").asString("");
        comprobarEntorno(id);
        String url = orden.path("checkout_url").asString("");
        if (!id.matches("[A-Za-z0-9]{5,60}") || !enlaceValido(url)
                || !intento.referencia().equals(orden.path("external_reference").asString(""))
                || !"PEN".equals(orden.path("currency").asString(""))
                || !mismoMonto(cita.monto(), orden, "total_amount")) throw noDisponible();
        checkout.guardarOrden(cita.id(), id, url);
        return checkout.porCita(cita.id(), false).orElseThrow();
    }

    public Respuestas.VerificacionCheckout estadoCheckout(long actor, long citaId) {
        citas.autorizada(actor, citaId);
        return checkout.porCita(citaId, false)
            .map(this::resumenCheckout)
            .orElse(new Respuestas.VerificacionCheckout("SIN_ORDEN", null));
    }

    private Respuestas.VerificacionCheckout resumenCheckout(Checkout intento) {
        return new Respuestas.VerificacionCheckout(intento.estado(), intento.incidencia());
    }

    public Respuestas.VerificacionCheckout cancelarCheckout(long actor, long citaId) {
        return transaccion.execute(tx -> {
            // Mismo orden de bloqueos que la acreditación: agenda, después checkout.
            agenda.bloquearCambios();
            Cita cita = citas.autorizada(actor, citaId);
            var intento = checkout.porCita(citaId, true).orElse(null);
            if (intento == null) return new Respuestas.VerificacionCheckout("SIN_ORDEN", null);
            if ("CERRADO".equals(intento.estado()) || "ACREDITADO".equals(intento.estado())
                    || "INCIDENCIA".equals(intento.estado())) return resumenCheckout(intento);
            if (!habilitada()) throw noDisponible();

            // Un timeout al crear no significa que la orden externa no exista.
            if (intento.ordenExterna() == null) intento = recuperarOrden(cita, intento);
            comprobarEntorno(intento.ordenExterna());
            var orden = consultarOrden(intento.ordenExterna());
            validarReferencia(intento, orden);
            String estado = orden.path("status").asString("");
            if ("created".equals(estado) || "action_required".equals(estado)) {
                String clave = UUID.nameUUIDFromBytes(("cancelar:" + intento.referencia())
                    .getBytes(StandardCharsets.UTF_8)).toString();
                try {
                    http.post().uri(apiUrl + "/v1/orders/{id}/cancel", intento.ordenExterna())
                        .headers(headers -> headers.setBearerAuth(token))
                        .header("X-Idempotency-Key", clave).retrieve().toBodilessEntity();
                } catch (RestClientException ex) {
                    // La orden puede haber cambiado o el proveedor haber recibido la petición.
                    // Solo se cierra localmente cuando la consulta confirma el estado externo.
                }
                orden = consultarOrden(intento.ordenExterna());
                validarReferencia(intento, orden);
                estado = orden.path("status").asString("");
                if ("created".equals(estado) || "action_required".equals(estado)) throw noDisponible();
            }
            aplicarOrden(intento, orden);
            var actualizado = checkout.porCita(citaId, false).orElseThrow();
            if ("CERRADO".equals(actualizado.estado())) {
                auditoria.registrar(actor, "CITA", citaId, "CHECKOUT_CERRADO", "Orden Mercado Pago cerrada");
            }
            return resumenCheckout(actualizado);
        });
    }

    private static void validarReferencia(Checkout intento, JsonNode orden) {
        if (!intento.referencia().equals(orden.path("external_reference").asString(""))) throw noDisponible();
    }

    private JsonNode crearOrden(Cita cita, Checkout intento) {
        var monto = cita.monto().setScale(2).toPlainString();
        var payload = new HashMap<String, Object>(Map.of("type", "online", "processing_mode", "manual",
            "total_amount", monto, "external_reference", intento.referencia(),
            "expiration_time", Duration.between(intento.creado(), intento.vence()).toString(),
            "items", List.of(Map.of("title", "Reserva de clase #" + cita.id(), "unit_price", monto, "quantity", 1))));
        if (!modoPrueba) {
            String retorno = frontend + "/agenda?id=" + cita.id();
            payload.put("capture_mode", "automatic");
            payload.put("config", Map.of("online", Map.of("success_url", retorno, "pending_url", retorno,
                "failure_url", retorno, "auto_return", "all")));
        }
        try {
            var orden = http.post().uri(apiUrl + "/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(token)).header("X-Idempotency-Key", intento.referencia())
                .body(payload).retrieve().body(JsonNode.class);
            if (orden == null) throw noDisponible();
            return orden;
        } catch (RestClientException ex) {
            throw noDisponible();
        }
    }

    public void notificacion(String id, String requestId, String firma) {
        if (modoPrueba || !habilitada()) throw noDisponible();
        verificarFirma(id, requestId, firma);
        JsonNode orden = consultarOrden(id);
        String referencia = orden.path("external_reference").asString("");
        if (!referencia.matches("[a-f0-9-]{36}")) return;
        transaccion.executeWithoutResult(tx -> {
            agenda.bloquearCambios();
            var intento = checkout.porReferencia(referencia).orElse(null);
            if (intento == null) return;
            intento = checkout.porCita(intento.citaId(), true).orElseThrow();
            if (intento.ordenExterna() == null) {
                // Mercado Pago puede notificar antes de que se guarde la respuesta de creación.
                throw noDisponible();
            }
            if (!id.equalsIgnoreCase(intento.ordenExterna())) return;
            aplicarOrden(intento, orden);
        });
    }

    public Respuestas.VerificacionCheckout verificarPagoPrueba(long actor, long citaId) {
        if (!modoPrueba || !habilitada()) throw noDisponible();
        citas.autorizada(actor, citaId);
        var intento = checkout.porCita(citaId, false).filter(item -> item.ordenExterna() != null)
            .orElseThrow(() -> ApiException.conflicto("Primero abre el checkout de prueba de esta reserva"));
        comprobarEntorno(intento.ordenExterna());
        var orden = consultarOrden(intento.ordenExterna());
        if (!intento.referencia().equals(orden.path("external_reference").asString(""))) throw noDisponible();

        return transaccion.execute(tx -> {
            agenda.bloquearCambios();
            var actual = checkout.porCita(citaId, true).orElseThrow();
            if (!intento.ordenExterna().equals(actual.ordenExterna())) throw noDisponible();
            aplicarOrden(actual, orden);
            var actualizado = checkout.porCita(citaId, false).orElseThrow();
            return new Respuestas.VerificacionCheckout(actualizado.estado(), actualizado.incidencia());
        });
    }

    private JsonNode consultarOrden(String id) {
        try {
            var orden = http.get().uri(apiUrl + "/v1/orders/{id}", id).headers(headers -> headers.setBearerAuth(token))
                .retrieve().body(JsonNode.class);
            if (orden == null || !id.equalsIgnoreCase(orden.path("id").asString(""))) throw noDisponible();
            return orden;
        } catch (RestClientException ex) {
            throw noDisponible();
        }
    }

    private void comprobarEntorno(String id) {
        if (modoPrueba && (id == null || !id.matches("ORDTST[A-Za-z0-9]{1,54}"))) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Mercado Pago no devolvió una orden de prueba. Revisa el Access Token en la pestaña Prueba");
        }
    }

    private void aplicarOrden(Checkout intento, JsonNode orden) {
        var cita = agenda.buscar(intento.citaId()).orElseThrow();
        var ahora = LocalDateTime.now(reloj);
        String estado = orden.path("status").asString("");
        String detalle = orden.path("status_detail").asString("");
        boolean registrado = pagos.deCita(cita.id()).stream().anyMatch(p ->
            p.medio() == MedioPago.MERCADO_PAGO && p.operacion().equals(intento.ordenExterna()));
        if ("refunded".equals(estado) || "partially_refunded".equals(detalle)
                || "charged_back".equals(estado) || "charged_back".equals(detalle)) {
            checkout.actualizar(cita.id(), "INCIDENCIA", "Mercado Pago notificó un reembolso o contracargo. Revisa la operación.", ahora);
            return;
        }
        if (registrado || "INCIDENCIA".equals(intento.estado())) return;
        if ("failed".equals(estado) || "canceled".equals(estado) || "expired".equals(estado)) {
            checkout.actualizar(cita.id(), "CERRADO", null, ahora);
        } else if ("processing".equals(estado) || "action_required".equals(estado)) {
            if (intento.abierto(ahora)) checkout.actualizar(cita.id(), "EN_PROCESO", null, ahora);
        } else if ("processed".equals(estado) && "accredited".equals(detalle)) {
            acreditar(cita, intento, orden, ahora);
        }
    }

    private void acreditar(Cita cita, Checkout intento, JsonNode orden, LocalDateTime ahora) {
        boolean valido = "PEN".equals(orden.path("currency").asString(""))
            && mismoMonto(cita.monto(), orden, "total_amount") && mismoMonto(cita.monto(), orden, "total_paid_amount");
        if (!valido || !intento.vence().isAfter(ahora) || !AgendaService.pendienteVigente(cita, ahora)) {
            checkout.actualizar(cita.id(), "INCIDENCIA", "Pago acreditado fuera de plazo o con datos distintos. Requiere conciliación.", ahora);
            return;
        }
        if (pagos.hayPagoEnRevision(cita.id())) {
            checkout.actualizar(cita.id(), "INCIDENCIA", "Pago acreditado mientras había otro medio en revisión. Requiere conciliación.", ahora);
            return;
        }
        var pago = pagos.crear(cita, MedioPago.MERCADO_PAGO, intento.ordenExterna(), null, ahora);
        pagos.revisar(pago.id(), EstadoPago.APROBADO, null, null, ahora);
        agenda.cambiarEstado(cita.id(), EstadoCita.CONFIRMADA, null);
        checkout.actualizar(cita.id(), "ACREDITADO", null, ahora);
        auditoria.registrar(null, "PAGO", pago.id(), "APROBADO", "Mercado Pago");
        auditoria.registrar(null, "CITA", cita.id(), "CONFIRMADA", "Mercado Pago");
        if (!modoPrueba) correo.programar(pago, cita);
    }

    private void verificarFirma(String id, String requestId, String firma) {
        if (id == null || !id.matches("[A-Za-z0-9]{5,60}") || requestId == null || requestId.isBlank() || firma == null) {
            throw firmaInvalida();
        }
        String ts = null;
        String digest = null;
        for (String parte : firma.split(",")) {
            var campo = parte.trim().split("=", 2);
            if (campo.length != 2) continue;
            if (campo[0].equals("ts")) ts = campo[1];
            if (campo[0].equals("v1")) digest = campo[1];
        }
        if (ts == null || !ts.matches("[0-9]{1,16}") || digest == null || !digest.matches("[A-Fa-f0-9]{64}")) {
            throw firmaInvalida();
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String manifiesto = "id:" + id.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + ts + ";";
            byte[] esperado = mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(esperado, HexFormat.of().parseHex(digest))) throw firmaInvalida();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo validar la firma de Mercado Pago", ex);
        }
    }

    private static boolean mismoMonto(BigDecimal monto, JsonNode orden, String campo) {
        try {
            return monto.compareTo(new BigDecimal(orden.path(campo).asString(""))) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean enlaceValido(String valor) {
        try {
            URI uri = URI.create(valor);
            return "https".equals(uri.getScheme()) && uri.getUserInfo() == null && uri.getPort() == -1
                && ("www.mercadopago.com.pe".equals(uri.getHost()) || "sandbox.mercadopago.com.pe".equals(uri.getHost()));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static ApiException firmaInvalida() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Firma de notificación inválida");
    }

    private static ApiException noDisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Mercado Pago no está disponible en este momento");
    }
}
