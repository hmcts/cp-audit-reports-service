
package uk.gov.hmcts.cpp.audit.bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cpp.audit.bff.model.ErrorResponse;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientErrorException(
        HttpClientErrorException ex,
        WebRequest request) {
        LOGGER.error("HTTP Client Error: {} - {}", ex.getStatusCode(), ex.getMessage());

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String reasonPhrase = status != null ? status.getReasonPhrase() : "Unknown Error";

        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatusCode().value(),
            reasonPhrase,
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            LocalDateTime.now(),
            getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerErrorException(
        HttpServerErrorException ex,
        WebRequest request) {
        LOGGER.error("HTTP Server Error: {} - {}", ex.getStatusCode(), ex.getMessage());

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String reasonPhrase = status != null ? status.getReasonPhrase() : "Unknown Error";

        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatusCode().value(),
            reasonPhrase,
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            LocalDateTime.now(),
            getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
        ResponseStatusException ex,
        WebRequest request) {
        LOGGER.error("Response Status Exception: {} - {}", ex.getStatusCode(), ex.getReason());

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String reasonPhrase = status != null ? status.getReasonPhrase() : "Unknown Error";

        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatusCode().value(),
            reasonPhrase,
            ex.getReason(),
            request.getDescription(false).replace("uri=", ""),
            LocalDateTime.now(),
            getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        WebRequest request) {
        LOGGER.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred. Please try again later.",
            request.getDescription(false).replace("uri=", ""),
            LocalDateTime.now(),
            getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getCorrelationId(WebRequest request) {
        return request.getHeader("CPPCLIENTCORRELATIONID") != null
            ? request.getHeader("CPPCLIENTCORRELATIONID")
            : "N/A";
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestHeader(MissingRequestHeaderException ex,
                                                                          WebRequest request) {
        Map<String, Object> body = Map.of(
            "error", "Bad Request",
            "message", ex.getMessage(),
            "correlationId", getCorrelationId(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

}
