package pe.instruccionfacil.exception;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiErrores {
    record Error(String mensaje, List<String> campos) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Error> negocio(ApiException ex) {
        return ResponseEntity.status(ex.estado()).body(new Error(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Error> validacion(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + " " + e.getDefaultMessage()).distinct().toList();
        return ResponseEntity.badRequest().body(new Error("Revisa los datos ingresados", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Error> formato() {
        return ResponseEntity.badRequest().body(new Error("El cuerpo de la solicitud tiene un formato inválido", List.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Error> tamano() {
        return ResponseEntity.status(413).body(new Error("El comprobante debe pesar como máximo 5 MB", List.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Error> duplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new Error("El registro está duplicado o entra en conflicto con los datos existentes", List.of()));
    }
}
