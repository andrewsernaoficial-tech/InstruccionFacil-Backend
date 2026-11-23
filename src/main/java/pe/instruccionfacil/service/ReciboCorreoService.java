package pe.instruccionfacil.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.entity.EstadoPago;
import pe.instruccionfacil.entity.Modelo.Cita;
import pe.instruccionfacil.entity.Modelo.Pago;
import pe.instruccionfacil.repository.AgendaRepository;
import pe.instruccionfacil.repository.PagoRepository;
import pe.instruccionfacil.repository.ReciboCorreoRepository;
import pe.instruccionfacil.repository.UsuarioRepository;
import tools.jackson.databind.JsonNode;

@Service
public class ReciboCorreoService {
    private static final Logger LOG = LoggerFactory.getLogger(ReciboCorreoService.class);

    private final RestClient http;
    private final ReciboCorreoRepository cola;
    private final PagoRepository pagos;
    private final AgendaRepository agenda;
    private final AgendaService citas;
    private final UsuarioRepository usuarios;
    private final Clock reloj;
    private final String oauthUrl;
    private final String gmailUrl;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String remitente;

    public ReciboCorreoService(RestClient http, ReciboCorreoRepository cola, PagoRepository pagos,
            AgendaRepository agenda, AgendaService citas, UsuarioRepository usuarios, Clock reloj,
            @Value("${app.gmail-oauth-url}") String oauthUrl, @Value("${app.gmail-api-url}") String gmailUrl,
            @Value("${app.gmail-client-id}") String clientId, @Value("${app.gmail-client-secret}") String clientSecret,
            @Value("${app.gmail-refresh-token}") String refreshToken, @Value("${app.gmail-remitente}") String remitente) {
        this.http = http;
        this.cola = cola;
        this.pagos = pagos;
        this.agenda = agenda;
        this.citas = citas;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.oauthUrl = oauthUrl;
        this.gmailUrl = gmailUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.remitente = remitente;
    }

    public boolean habilitado() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank() && correoValido(remitente);
    }

    void programar(Pago pago, Cita cita) {
        if (!habilitado()) return;
        var email = usuarios.buscar(cita.alumnoId()).orElseThrow().email();
        if (correoValido(email)) cola.encolar(pago.id(), email, LocalDateTime.now(reloj));
    }

    @Transactional
    public Respuestas.EnvioCorreo solicitar(long actor, long pagoId) {
        agenda.bloquearCambios();
        var pago = buscarPago(pagoId);
        var cita = citas.autorizada(actor, pago.citaId());
        if (pago.estado() != EstadoPago.APROBADO) throw ApiException.conflicto("El pago todavía no está aprobado");
        var existente = cola.buscar(pagoId);
        if (existente.isPresent() && !existente.get().estado().equals("FALLIDO")) {
            return new Respuestas.EnvioCorreo(existente.get().estado());
        }
        if (!habilitado()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El envío por Gmail no está configurado");
        var email = usuarios.buscar(cita.alumnoId()).orElseThrow().email();
        if (!correoValido(email)) throw ApiException.conflicto("El alumno debe registrar un correo válido en su perfil");
        var ahora = LocalDateTime.now(reloj);
        if (existente.isEmpty()) cola.encolar(pagoId, email, ahora);
        else cola.reintentar(pagoId, email, ahora);
        return new Respuestas.EnvioCorreo("PENDIENTE");
    }

    @Scheduled(fixedDelay = 15000)
    void entregarPendientes() {
        if (!habilitado()) return;
        var ahora = LocalDateTime.now(reloj);
        for (long id : cola.pendientes(ahora)) {
            if (!cola.reclamar(id, LocalDateTime.now(reloj))) continue;
            var envio = cola.buscar(id).orElseThrow();
            try {
                var pago = buscarPago(id);
                var cita = agenda.buscar(pago.citaId()).orElseThrow();
                String alumno = usuarios.buscar(cita.alumnoId()).orElseThrow().nombre();
                byte[] pdf = ReciboPdf.crear(pago, alumno);
                String gmailId = enviar(envio.destinatario(), pago.id(), pdf);
                cola.enviado(envio, gmailId);
            } catch (RestClientException | IllegalStateException ex) {
                cola.fallo(envio, LocalDateTime.now(reloj));
                LOG.warn("No se pudo entregar recibo {}, intento {}", id, envio.intentos());
            }
        }
    }

    private String enviar(String destinatario, long pagoId, byte[] pdf) {
        var formulario = new LinkedMultiValueMap<String, String>();
        formulario.add("client_id", clientId);
        formulario.add("client_secret", clientSecret);
        formulario.add("refresh_token", refreshToken);
        formulario.add("grant_type", "refresh_token");
        var oauth = http.post().uri(oauthUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formulario).retrieve().body(JsonNode.class);
        String token = oauth == null ? "" : oauth.path("access_token").asString("");
        if (token.isBlank()) throw new IllegalStateException("Gmail no devolvió un token");
        String limite = "recibo-" + UUID.randomUUID();
        String contenido = Base64.getMimeEncoder().encodeToString(pdf);
        String correo = String.join("\r\n", "From: " + remitente, "To: " + destinatario,
            "Subject: InstruccionFacil - Recibo de pago #" + pagoId, "MIME-Version: 1.0",
            "Content-Type: multipart/mixed; boundary=\"" + limite + "\"", "",
            "--" + limite, "Content-Type: text/plain; charset=UTF-8", "",
            "Adjuntamos el recibo de tu pago en InstruccionFacil.", "", "--" + limite,
            "Content-Type: application/pdf", "Content-Transfer-Encoding: base64",
            "Content-Disposition: attachment; filename=\"recibo-" + pagoId + ".pdf\"", "",
            contenido, "--" + limite + "--", "");
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(correo.getBytes(StandardCharsets.UTF_8));
        var respuesta = http.post().uri(gmailUrl + "/gmail/v1/users/me/messages/send")
            .headers(headers -> headers.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("raw", raw)).retrieve().body(JsonNode.class);
        String id = respuesta == null ? "" : respuesta.path("id").asString("");
        if (id.isBlank()) throw new IllegalStateException("Gmail no devolvió un identificador");
        return id;
    }

    private Pago buscarPago(long id) {
        return pagos.buscar(id).orElseThrow(() -> ApiException.noEncontrado("Pago no encontrado"));
    }

    private static boolean correoValido(String valor) {
        return valor != null && valor.length() <= 254 && valor.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+");
    }
}
