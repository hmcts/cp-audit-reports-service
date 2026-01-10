package uk.gov.hmcts.cpp.audit.bff.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cpp.audit.bff.model.ErrorResponse;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest mockRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        mockRequest = mock(WebRequest.class);
    }

    @Test
    void testHandleHttpClientErrorExceptionBadRequest() {
        String errorMessage = "Invalid request parameters";
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.BAD_REQUEST,
            errorMessage
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/test-endpoint");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("correlation-123");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpClientErrorException(exception, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("/test-endpoint", response.getBody().path());
        assertEquals("correlation-123", response.getBody().correlationId());
    }

    @Test
    void testHandleHttpClientErrorExceptionUnauthorized() {
        String errorMessage = "Unauthorized access";
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.UNAUTHORIZED,
            errorMessage
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/secure-endpoint");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpClientErrorException(exception, mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("N/A", response.getBody().correlationId());
    }

    @Test
    void testHandleHttpClientErrorExceptionForbidden() {
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.FORBIDDEN,
            "Access denied"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/admin");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("corr-456");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpClientErrorException(exception, mockRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("Forbidden", response.getBody().error());
    }

    @Test
    void testHandleHttpClientErrorExceptionNotFound() {
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.NOT_FOUND,
            "Resource not found"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/missing");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("corr-789");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpClientErrorException(exception, mockRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
    }

    @Test
    void testHandleHttpServerErrorExceptionInternalServerError() {
        String errorMessage = "Internal server error occurred";
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            errorMessage
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/process");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("server-err-123");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpServerErrorException(exception, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("server-err-123", response.getBody().correlationId());
    }

    @Test
    void testHandleHttpServerErrorExceptionServiceUnavailable() {
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service temporarily unavailable"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/service");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("svc-unavail");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpServerErrorException(exception, mockRequest);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getBody().status());
        assertEquals("Service Unavailable", response.getBody().error());
    }

    @Test
    void testHandleHttpServerErrorExceptionBadGateway() {
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.BAD_GATEWAY,
            "Bad gateway"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/gateway");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("gateway-err");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpServerErrorException(exception, mockRequest);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().status());
        assertEquals("Bad Gateway", response.getBody().error());
    }

    @Test
    void testHandleGenericException() {
        Exception exception = new IllegalArgumentException("Unexpected error");
        when(mockRequest.getDescription(false)).thenReturn("uri=/operation");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("generic-err-001");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGenericException(exception, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected error occurred. Please try again later.",
                     response.getBody().message());
        assertEquals("/operation", response.getBody().path());
        assertEquals("generic-err-001", response.getBody().correlationId());
    }

    @Test
    void testHandleGenericExceptionNullPointerException() {
        NullPointerException exception = new NullPointerException();
        when(mockRequest.getDescription(false)).thenReturn("uri=/process-data");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGenericException(exception, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("N/A", response.getBody().correlationId());
    }

    @Test
    void testHandleGenericExceptionRuntimeException() {
        RuntimeException exception = new RuntimeException("System error");
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/v1/users");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("runtime-err-999");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGenericException(exception, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected error occurred. Please try again later.",
                     response.getBody().message());
    }

    @Test
    void testHandleHttpClientErrorExceptionWithoutCorrelationId() {
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.BAD_REQUEST,
            "Invalid input"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/input");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpClientErrorException(exception, mockRequest);

        assertEquals("N/A", response.getBody().correlationId());
    }

    @Test
    void testHandleResponseStatusExceptionNotFound() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Resource not found"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/materials");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-404-001");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Resource not found", response.getBody().message());
        assertEquals("/api/materials", response.getBody().path());
        assertEquals("resp-404-001", response.getBody().correlationId());
    }

    @Test
    void testHandleResponseStatusExceptionBadRequest() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid request format"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/validate");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-400-002");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Invalid request format", response.getBody().message());
    }

    @Test
    void testHandleResponseStatusExceptionUnauthorized() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Authentication required"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/protected");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-401-003");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("Authentication required", response.getBody().message());
    }

    @Test
    void testHandleResponseStatusExceptionForbidden() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Access forbidden"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/admin");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-403-004");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
        assertEquals("Forbidden", response.getBody().error());
    }

    @Test
    void testHandleResponseStatusExceptionConflict() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Resource already exists"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/create");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-409-005");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Resource already exists", response.getBody().message());
    }

    @Test
    void testHandleResponseStatusExceptionInternalServerError() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Server encountered an error"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/process");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-500-006");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
    }

    @Test
    void testHandleResponseStatusExceptionWithoutCorrelationId() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No Material Cases found"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/materials");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("N/A", response.getBody().correlationId());
        assertEquals("/api/materials", response.getBody().path());
    }

    @Test
    void testHandleResponseStatusExceptionNoReason() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST);
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/test");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-no-reason");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleResponseStatusExceptionGatewayTimeout() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.GATEWAY_TIMEOUT,
            "Gateway timeout occurred"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/external");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-504-007");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals(504, response.getBody().status());
        assertEquals("Gateway Timeout", response.getBody().error());
    }

    @Test
    void testHandleResponseStatusExceptionServiceUnavailable() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service is temporarily unavailable"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/service");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("resp-503-008");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getBody().status());
        assertEquals("Service Unavailable", response.getBody().error());
        assertEquals("Service is temporarily unavailable", response.getBody().message());
    }

    @Test
    void testHandleResponseStatusExceptionResponseBodyNotNull() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No users found for the provided email addresses"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/user/email");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("user-not-found");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().timestamp());
        assertEquals("/user/email", response.getBody().path());
    }

    @Test
    void testHandleResponseStatusExceptionTimestampPresent() {
        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Resource not found"
        );
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/test");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("timestamp-test");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResponseStatusException(exception, mockRequest);

        assertNotNull(response.getBody().timestamp());
    }
}
