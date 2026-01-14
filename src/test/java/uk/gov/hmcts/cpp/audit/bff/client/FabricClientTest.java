package uk.gov.hmcts.cpp.audit.bff.client;

import com.azure.identity.DefaultAzureCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineRequest;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineResponse;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class FabricClientTest {

    private FabricClient fabricClient;
    private MockRestServiceServer mockServer;
    private RestTemplate restTemplate;

    @Mock
    private DefaultAzureCredential azureCredential;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        fabricClient = new FabricClient(
            restTemplate,
            azureCredential,
            "https://api.fabric.microsoft.com/v1",
            "workspace-123",
            "pipeline-456",
            "Param Test"
        );
    }

    @Test
    void shouldSuccessfullyTriggerPipelineExecution() {
        String correlationId = "test-correlation-id";
        String responseJson = """
            {
              "id": "job-123",
              "status": "Queued",
              "createdTimeUtc": "2026-01-14T10:00:00Z",
              "rootActivityId": "activity-789"
            }
            """;

        mockServer.expect(requestTo("https://api.fabric.microsoft.com/v1/workspaces/workspace-123/items/pipeline-456/jobs/instances?jobType=Pipeline"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.parameters.requestinguser").value("test@example.com"))
            .andExpect(jsonPath("$.parameters.userid").value("user-123"))
            .andExpect(jsonPath("$.parameters.from_dateutc").value("2026-01-01"))
            .andExpect(jsonPath("$.parameters.to_dateutc").value("2026-01-31"))
            .andRespond(withStatus(HttpStatus.ACCEPTED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-123")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("job-123");
        assertThat(response.getStatus()).isEqualTo("Queued");
        assertThat(response.getCreatedTimeUtc()).isEqualTo("2026-01-14T10:00:00Z");
        assertThat(response.getRootActivityId()).isEqualTo("activity-789");
        mockServer.verify();
    }

    @Test
    void shouldReturnResponseWith202AcceptedStatus() {
        String correlationId = "test-correlation-id";
        String responseJson = """
            {
              "id": "job-456",
              "status": "Queued",
              "createdTimeUtc": "2026-01-14T11:00:00Z",
              "rootActivityId": "activity-999"
            }
            """;

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.ACCEPTED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("audit@example.com")
            .userId("user-456")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("job-456");
        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionOnServerError() {
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-123")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        assertThatThrownBy(() -> fabricClient.runOnDemandPipeline(request, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionOnUnauthorized() {
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-123")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        assertThatThrownBy(() -> fabricClient.runOnDemandPipeline(request, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldIncludeParametersInRequestBody() {
        String correlationId = "test-correlation-id";
        String responseJson = """
            {
              "id": "job-789",
              "status": "Queued",
              "createdTimeUtc": "2026-01-14T12:00:00Z",
              "rootActivityId": "activity-111"
            }
            """;

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.parameters['requestinguser']").value("requestuser@example.com"))
            .andExpect(jsonPath("$.parameters['userid']").value("user-789"))
            .andExpect(jsonPath("$.parameters['from_dateutc']").value("2026-01-05"))
            .andExpect(jsonPath("$.parameters['to_dateutc']").value("2026-01-20"))
            .andRespond(withStatus(HttpStatus.ACCEPTED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("requestuser@example.com")
            .userId("user-789")
            .fromDateUtc(LocalDate.of(2026, 1, 5))
            .toDateUtc(LocalDate.of(2026, 1, 20))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        assertThat(response).isNotNull();
        mockServer.verify();
    }

    @Test
    void shouldReturnResponseWithUnexpectedStatusCode() {
        String correlationId = "test-correlation-id";
        String responseJson = """
            {
              "id": "job-200",
              "status": "Queued",
              "createdTimeUtc": "2026-01-14T14:00:00Z",
              "rootActivityId": "activity-200"
            }
            """;

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-200")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("job-200");
        mockServer.verify();
    }

    @Test
    void shouldHandleAcceptedStatusWithNullResponseBody() {
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.ACCEPTED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(""));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-300")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        assertThat(response).isNull();
        mockServer.verify();
    }

    @Test
    void shouldHandleUnexpectedStatusWithNullResponseBody() {
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CREATED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(""));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-400")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        assertThat(response).isNull();
        mockServer.verify();
    }

    @Test
    void shouldLogSuccessfullyQueuedMessageWhenAcceptedWithValidBody() {
        String correlationId = "test-correlation-id";
        String responseJson = """
            {
              "id": "job-success-123",
              "status": "Queued",
              "createdTimeUtc": "2026-01-14T15:00:00Z",
              "rootActivityId": "activity-success"
            }
            """;

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.ACCEPTED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-success")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        // Verify that response body is not null - exercises ternary:
        // response.getBody() != null ? response.getBody().getJobId() : "unknown"
        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("job-success-123");
        assertThat(response.getJobId()).isNotEqualTo("unknown");
        mockServer.verify();
    }

    @Test
    void shouldLogUnknownWhenAcceptedWithNullJobId() {
        String correlationId = "test-correlation-id";
        String responseJson = """
            {
              "status": "Queued",
              "createdTimeUtc": "2026-01-14T16:00:00Z",
              "rootActivityId": "activity-no-id"
            }
            """;

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.ACCEPTED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-no-id")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        // Response is not null but jobId will be null
        // Covers ternary operator path where jobId is null
        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isNull();
        mockServer.verify();
    }

    @Test
    void shouldLogUnexpectedStatusWithValidBody() {
        String correlationId = "test-correlation-id";
        String responseJson = """
            {
              "id": "job-created-200",
              "status": "Processing",
              "createdTimeUtc": "2026-01-14T17:00:00Z",
              "rootActivityId": "activity-201"
            }
            """;

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-created")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        FabricPipelineResponse response = fabricClient.runOnDemandPipeline(request, correlationId);

        // Exercises else branch logging with valid body
        // Ternary: response.getBody() != null ? response.getBody().getJobId() : "unknown"
        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("job-created-200");
        mockServer.verify();
    }

    @Test
    void shouldHandleForbiddenStatus() {
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-123")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        assertThatThrownBy(() -> fabricClient.runOnDemandPipeline(request, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldHandleBadRequestStatus() {
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(containsString("/workspaces/workspace-123/items/pipeline-456/jobs/instances")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser("test@example.com")
            .userId("user-123")
            .fromDateUtc(LocalDate.of(2026, 1, 1))
            .toDateUtc(LocalDate.of(2026, 1, 31))
            .build();

        assertThatThrownBy(() -> fabricClient.runOnDemandPipeline(request, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }
}
