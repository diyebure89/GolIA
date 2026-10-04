package com.diyebure.golia.common;

import com.diyebure.golia.auth.dto.AuthDtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones a un cuerpo JSON de error con el formato que la app
 * Android espera ({@code error}, {@code message}, {@code status_code}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(ex.getError(), ex.getMessage(), ex.getStatus().value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Datos invalidos.";
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("validation_error", message,
                        HttpStatus.UNPROCESSABLE_ENTITY.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log completo del stacktrace para diagnosticar errores 500.
        log.error("Error inesperado procesando la peticion", ex);
        // TEMPORAL (desarrollo): se expone el detalle real del error en el cuerpo
        // para diagnosticar sin revisar logs. Revertir a un mensaje generico
        // antes de produccion.
        String detail = ex.getClass().getSimpleName()
                + ": " + (ex.getMessage() != null ? ex.getMessage() : "sin mensaje");
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root != ex) {
            detail += " | causa: " + root.getClass().getSimpleName()
                    + ": " + (root.getMessage() != null ? root.getMessage() : "sin mensaje");
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("server_error", detail,
                        HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
