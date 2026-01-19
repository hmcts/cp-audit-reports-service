package uk.gov.hmcts.cpp.audit.bff.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cpp.audit.bff.client.FabricClient;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineRequest;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineResponse;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FabricServiceTest {

    private FabricService fabricService;

    @Mock
    private FabricClient fabricClient;

    @BeforeEach
    void setUp() {
        fabricService = new FabricService(fabricClient);
    }

    @Test
    void shouldSuccessfullyTriggerAuditPipeline() {
        String correlationId = "test-correlation-id";
        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-123")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse expectedResponse = FabricPipelineResponse.builder()
            .jobId("job-123")
            .status("Queued")
            .createdTimeUtc("2026-01-14T10:00:00Z")
            .rootActivityId("activity-789")
            .build();

        when(fabricClient.runOnDemandPipeline(any(FabricPipelineRequest.class), eq(correlationId)))
            .thenReturn(expectedResponse);

        FabricPipelineResponse response = fabricService.triggerAuditPipeline(request, correlationId);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("job-123");
        assertThat(response.getStatus()).isEqualTo("Queued");
        verify(fabricClient).runOnDemandPipeline(request, correlationId);
    }

    @Test
    void shouldPassRequestAndCorrelationIdToClient() {
        String correlationId = "another-correlation-id";
        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("audit@example.com")
            .userId("user-456")
            .fromDateUtc(LocalDate.of(2026, 1, 5))
            .toDateUtc(LocalDate.of(2026, 1, 20))
            .build();

        FabricPipelineResponse expectedResponse = FabricPipelineResponse.builder()
            .jobId("job-456")
            .status("Queued")
            .build();

        when(fabricClient.runOnDemandPipeline(request, correlationId))
            .thenReturn(expectedResponse);

        fabricService.triggerAuditPipeline(request, correlationId);

        verify(fabricClient).runOnDemandPipeline(request, correlationId);
    }

    @Test
    void shouldReturnResponseFromClient() {
        String correlationId = "test-id";
        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("user@example.com")
            .userId("user-789")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 10))
            .build();

        FabricPipelineResponse expectedResponse = FabricPipelineResponse.builder()
            .jobId("job-789")
            .status("Queued")
            .createdTimeUtc("2026-01-14T13:00:00Z")
            .rootActivityId("activity-999")
            .build();

        when(fabricClient.runOnDemandPipeline(request, correlationId))
            .thenReturn(expectedResponse);

        FabricPipelineResponse response = fabricService.triggerAuditPipeline(request, correlationId);

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(response.getJobId()).isEqualTo("job-789");
        assertThat(response.getRootActivityId()).isEqualTo("activity-999");
    }
}
