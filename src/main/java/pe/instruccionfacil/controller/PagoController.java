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
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.service.PagoService;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {
    private final PagoService pagos;

    public PagoController(PagoService pagos) {
        this.pagos = pagos;
    }

    @GetMapping("/pagos/medios")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    List<PagoService.MedioDisponible> medios() {
        return pagos.medios();
    }

    @GetMapping("/citas/{id}/pagos")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    List<Pago> historial(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return pagos.historial(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping(path = "/citas/{id}/pagos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Pago subir(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @RequestParam MedioPago medio,
               @RequestParam String operacion, @RequestParam MultipartFile archivo) {
        return pagos.subir(Long.parseLong(jwt.getSubject()), id, medio, operacion, archivo);
    }

    @GetMapping("/admin/pagos/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Pago> pendientes() {
        return pagos.pendientes();
    }

    @PatchMapping("/admin/pagos/{id}/revision")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    Pago revisar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Solicitudes.Revision datos) {
        return pagos.revisar(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @GetMapping("/pagos/{id}/comprobante")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
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
