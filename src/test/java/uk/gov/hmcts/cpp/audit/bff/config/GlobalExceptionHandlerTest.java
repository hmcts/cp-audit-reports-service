package uk.gov.hmcts.cpp.audit.bff.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cpp.audit.bff.model.ErrorResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest mockRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        mockRequest = mock(WebRequest.class);
    }

    @Test
    void shouldHandleHttpClientErrorExceptionWithCorrelationId() {
        HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid input");
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/test");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("corr-123");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpClientErrorException(ex, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("corr-123", response.getBody().correlationId());
        assertEquals("/api/test", response.getBody().path());
        assertEquals("Bad Request", response.getBody().error());
    }

    @Test
    void shouldHandleHttpClientErrorExceptionWithUnknownStatus() {
        HttpClientErrorException ex = mock(HttpClientErrorException.class);
        org.springframework.http.HttpStatusCode customStatus = org.springframework.http.HttpStatusCode.valueOf(999);
        when(ex.getStatusCode()).thenReturn(customStatus);
        when(ex.getMessage()).thenReturn("Unknown status");

        when(mockRequest.getDescription(false)).thenReturn("uri=/unknown");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpClientErrorException(ex, mockRequest);

        assertEquals(999, response.getStatusCode().value());
        assertEquals("Unknown Error", response.getBody().error());
        assertEquals("N/A", response.getBody().correlationId());
    }

    @Test
    void shouldHandleHttpServerErrorException() {
        HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server failure");
        when(mockRequest.getDescription(false)).thenReturn("uri=/server-error");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("corr-server");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpServerErrorException(ex, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("corr-server", response.getBody().correlationId());
    }

    @Test
    void shouldHandleHttpServerErrorExceptionWithUnknownStatus() {
        HttpServerErrorException ex = mock(HttpServerErrorException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.valueOf(500));
        when(ex.getStatusCode()).thenReturn(HttpStatus.valueOf(500));
        org.springframework.http.HttpStatusCode customStatus = org.springframework.http.HttpStatusCode.valueOf(600);
        when(ex.getStatusCode()).thenReturn(customStatus);
        when(ex.getMessage()).thenReturn("Unknown server status");

        when(mockRequest.getDescription(false)).thenReturn("uri=/unknown-server");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpServerErrorException(ex, mockRequest);

        assertEquals(600, response.getStatusCode().value());
        assertEquals("Unknown Error", response.getBody().error());
    }

    @Test
    void shouldHandleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource missing");
        when(mockRequest.getDescription(false)).thenReturn("uri=/missing");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("corr-404");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResponseStatusException(ex, mockRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Resource missing", response.getBody().message());
        assertEquals("Not Found", response.getBody().error());
    }

    @Test
    void shouldHandleResponseStatusExceptionWithUnknownStatus() {
        ResponseStatusException ex = mock(ResponseStatusException.class);
        org.springframework.http.HttpStatusCode customStatus = org.springframework.http.HttpStatusCode.valueOf(888);
        when(ex.getStatusCode()).thenReturn(customStatus);
        when(ex.getReason()).thenReturn("Unknown response status");

        when(mockRequest.getDescription(false)).thenReturn("uri=/unknown-resp");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResponseStatusException(ex, mockRequest);

        assertEquals(888, response.getStatusCode().value());
        assertEquals("Unknown Error", response.getBody().error());
        assertEquals("N/A", response.getBody().correlationId());
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Fatal error");
        when(mockRequest.getDescription(false)).thenReturn("uri=/fatal");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn("corr-999");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(ex, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().message());
        assertEquals("corr-999", response.getBody().correlationId());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void shouldHandleGenericExceptionWithoutCorrelationId() {
        Exception ex = new RuntimeException("Error");
        when(mockRequest.getDescription(false)).thenReturn("uri=/test");
        when(mockRequest.getHeader("CPPCLIENTCORRELATIONID")).thenReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(ex, mockRequest);

        assertEquals("N/A", response.getBody().correlationId());
    }
}
