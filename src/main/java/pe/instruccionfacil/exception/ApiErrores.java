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
import pe.instruccionfacil.dto.ErrorApiResponseDTO;
import pe.instruccionfacil.exception.ApiException;

@RestControllerAdvice
public class ApiErrores {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorApiResponseDTO> negocio(ApiException ex) {
        return ResponseEntity.status(ex.estado()).body(new ErrorApiResponseDTO(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApiResponseDTO> validacion(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + " " + e.getDefaultMessage()).distinct().toList();
        return ResponseEntity.badRequest().body(new ErrorApiResponseDTO("Revisa los datos ingresados", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApiResponseDTO> formato() {
        return ResponseEntity.badRequest().body(new ErrorApiResponseDTO("El cuerpo de la solicitud tiene un formato inválido", List.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorApiResponseDTO> tamano() {
        return ResponseEntity.status(413).body(new ErrorApiResponseDTO("El comprobante debe pesar como máximo 5 MB", List.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApiResponseDTO> duplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorApiResponseDTO("El registro está duplicado o entra en conflicto con los datos existentes", List.of()));
    }
}
