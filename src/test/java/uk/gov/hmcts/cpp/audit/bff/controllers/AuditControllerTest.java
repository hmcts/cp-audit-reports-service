package uk.gov.hmcts.cpp.audit.bff.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineRequest;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineResponse;
import uk.gov.hmcts.cpp.audit.bff.service.FabricService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private AuditController auditController;

    @Mock
    private FabricService fabricService;

    @BeforeEach
    void setUp() {
        auditController = new AuditController(fabricService);
    }

    @Test
    void shouldReturnAcceptedStatusWithResponse() {
        String requestingUser = "test@example.com";
        String userId = "user-123";
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        String correlationId = "test-correlation-id";

        FabricPipelineResponse expectedResponse = FabricPipelineResponse.builder()
            .jobId("job-123")
            .status("Queued")
            .createdTimeUtc("2026-01-14T10:00:00Z")
            .rootActivityId("activity-789")
            .build();

        when(fabricService.triggerAuditPipeline(any(FabricPipelineRequest.class), eq(correlationId)))
            .thenReturn(expectedResponse);

        ResponseEntity<FabricPipelineResponse> response = auditController.triggerAuditPipeline(
            requestingUser, userId, fromDate, toDate, correlationId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
    }

    @Test
    void shouldGenerateCorrelationIdIfNotProvided() {
        String requestingUser = "audit@example.com";
        String userId = "user-456";
        LocalDate fromDate = LocalDate.of(2026, 1, 5);
        LocalDate toDate = LocalDate.of(2026, 1, 20);

        FabricPipelineResponse expectedResponse = FabricPipelineResponse.builder()
            .jobId("job-456")
            .status("Queued")
            .build();

        when(fabricService.triggerAuditPipeline(any(FabricPipelineRequest.class), any(String.class)))
            .thenReturn(expectedResponse);

        ResponseEntity<FabricPipelineResponse> response = auditController.triggerAuditPipeline(
            requestingUser, userId, fromDate, toDate, null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        verify(fabricService).triggerAuditPipeline(any(FabricPipelineRequest.class), any(String.class));
    }

    @Test
    void shouldPassCorrectParametersToService() {
        String requestingUser = "search@example.com";
        String userId = "user-789";
        LocalDate fromDate = LocalDate.of(2026, 1, 10);
        LocalDate toDate = LocalDate.of(2026, 1, 25);
        String correlationId = "my-correlation-id";

        FabricPipelineResponse expectedResponse = FabricPipelineResponse.builder()
            .jobId("job-789")
            .status("Queued")
            .build();

        when(fabricService.triggerAuditPipeline(any(FabricPipelineRequest.class), eq(correlationId)))
            .thenReturn(expectedResponse);

        ResponseEntity<FabricPipelineResponse> response = auditController.triggerAuditPipeline(
            requestingUser, userId, fromDate, toDate, correlationId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(fabricService).triggerAuditPipeline(any(FabricPipelineRequest.class), eq(correlationId));
    }

    @Test
    void shouldReturn202StatusCodeAsPerAcceptanceCriteria() {
        String requestingUser = "test@example.com";
        String userId = "user-123";
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        String correlationId = "test-id";

        FabricPipelineResponse response = FabricPipelineResponse.builder()
            .jobId("job-123")
            .status("Queued")
            .build();

        when(fabricService.triggerAuditPipeline(any(FabricPipelineRequest.class), eq(correlationId)))
            .thenReturn(response);

        ResponseEntity<FabricPipelineResponse> result = auditController.triggerAuditPipeline(
            requestingUser, userId, fromDate, toDate, correlationId
        );

        assertThat(result.getStatusCode().value()).isEqualTo(202);
    }
}
