package pe.instruccionfacil.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.entity.MedioPago;
import pe.instruccionfacil.service.PagoService;
import pe.instruccionfacil.service.PasarelaService;
import pe.instruccionfacil.service.ReciboCorreoService;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {
    private final PagoService pagos;

    private final PasarelaService pasarela;
    private final ReciboCorreoService correo;

    public PagoController(PagoService pagos, PasarelaService pasarela, ReciboCorreoService correo) {
        this.pagos = pagos;
        this.pasarela = pasarela;
        this.correo = correo;
    }

    @PostMapping("/citas/{id}/pagos/checkout")
    @PreAuthorize("hasRole('CLIENTE')")
    Respuestas.EnlaceCheckout checkout(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return pasarela.abrir(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/citas/{id}/pagos/checkout")
    @PreAuthorize("hasRole('CLIENTE')")
    Respuestas.VerificacionCheckout estadoCheckout(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return pasarela.estadoCheckout(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping("/citas/{id}/pagos/checkout/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    Respuestas.VerificacionCheckout cancelarCheckout(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return pasarela.cancelarCheckout(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping("/citas/{id}/pagos/checkout/verificar")
    @PreAuthorize("hasRole('CLIENTE')")
    Respuestas.VerificacionCheckout verificarCheckout(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return pasarela.verificarPagoPrueba(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping("/pagos/{id}/recibo/correo")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','RECEPCION')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Respuestas.EnvioCorreo enviarRecibo(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return correo.solicitar(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/pagos/medios")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    List<Respuestas.MedioDisponible> medios() {
        return pagos.medios();
    }

    @GetMapping("/citas/{id}/pagos")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    List<Respuestas.Pago> historial(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return pagos.historial(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping(path = "/citas/{id}/pagos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Respuestas.Pago subir(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @RequestParam("medio") MedioPago medio,
               @RequestParam(name = "operacion", required = false) String operacion,
               @RequestParam(name = "archivo", required = false) MultipartFile archivo) {
        return pagos.subir(Long.parseLong(jwt.getSubject()), id, new Solicitudes.CargaComprobante(medio, operacion, archivo));
    }

    @GetMapping("/admin/pagos/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Respuestas.Pago> pendientes() {
        return pagos.pendientes();
    }

    @PatchMapping("/admin/pagos/{id}/revision")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    Respuestas.Pago revisar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Solicitudes.Revision datos) {
        return pagos.revisar(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @GetMapping("/pagos/{id}/recibo")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','RECEPCION')")
    ResponseEntity<byte[]> recibo(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        var disposition = ContentDisposition.attachment().filename("recibo-" + id + ".pdf").build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(pagos.recibo(Long.parseLong(jwt.getSubject()), id));
    }

    @GetMapping("/pagos/{id}/comprobante")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','RECEPCION')")
    ResponseEntity<byte[]> descargar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        var archivo = pagos.descargar(Long.parseLong(jwt.getSubject()), id);
        String extension = switch (archivo.mime()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> "pdf";
        };
        var disposition = ContentDisposition.attachment().filename("comprobante-" + id + "." + extension).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(archivo.mime()))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(archivo.contenido());
    }
}
