
package uk.gov.hmcts.cpp.audit.bff.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cpp.audit.bff.model.ErrorResponse;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientErrorException(
        HttpClientErrorException ex,
        WebRequest request
    ) {
        log.error("HTTP Client Error: {} - {}", ex.getStatusCode(), ex.getMessage());

        return handleException(
            ex, request,
            RestClientResponseException::getStatusCode,
            Throwable::getMessage
        );
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerErrorException(
        HttpServerErrorException ex,
        WebRequest request
    ) {
        log.error("HTTP Server Error: {} - {}", ex.getStatusCode(), ex.getMessage());

        return handleException(
            ex, request,
            RestClientResponseException::getStatusCode,
            Throwable::getMessage
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
        ResponseStatusException ex,
        WebRequest request
    ) {
        log.error("Response Status Exception: {} - {}", ex.getStatusCode(), ex.getReason());

        return handleException(
            ex, request,
            ErrorResponseException::getStatusCode,
            ResponseStatusException::getReason
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        return handleException(
            ex, request,
            ignored -> HttpStatus.INTERNAL_SERVER_ERROR,
            ignored -> HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
        );
    }

    private <T> ResponseEntity<ErrorResponse> handleException(
        T ex, WebRequest request,
        Function<T, HttpStatusCode> statusCode,
        Function<T, String> message
    ) {
        final var status = HttpStatus.resolve(statusCode.apply(ex).value());
        final var reasonPhrase = Optional
            .ofNullable(status)
            .map(HttpStatus::getReasonPhrase)
            .orElse("Unknown Error");

        final var errorResponse = new ErrorResponse(
            statusCode.apply(ex).value(),
            reasonPhrase,
            message.apply(ex),
            request.getDescription(false).replace("uri=", ""),
            LocalDateTime.now(),
            Optional.ofNullable(request.getHeader("CPPCLIENTCORRELATIONID")).orElse("N/A")
        );

        return ResponseEntity.status(statusCode.apply(ex)).body(errorResponse);
    }
}
