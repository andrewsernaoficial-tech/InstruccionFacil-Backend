package pe.instruccionfacil.controller;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.instruccionfacil.dto.Respuestas.ErrorApi;
import pe.instruccionfacil.service.ApiException;

@RestControllerAdvice
public class ApiErrores {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorApi> negocio(ApiException ex) {
        return ResponseEntity.status(ex.estado()).body(new ErrorApi(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + " " + e.getDefaultMessage()).distinct().toList();
        return ResponseEntity.badRequest().body(new ErrorApi("Revisa los datos ingresados", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApi> formato() {
        return ResponseEntity.badRequest().body(new ErrorApi("El cuerpo de la solicitud tiene un formato inválido", List.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorApi> tamano() {
        return ResponseEntity.status(413).body(new ErrorApi("El comprobante debe pesar como máximo 5 MB", List.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> duplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorApi("El registro está duplicado o entra en conflicto con los datos existentes", List.of()));
    }
}
