package uk.gov.hmcts.cpp.audit.bff.client;

import com.azure.identity.DefaultAzureCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineRequest;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineResponse;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.cpp.audit.bff.constants.HeaderConstants.HEADER_CORRELATION_ID;

/**
 * Client for interacting with Microsoft Fabric API to trigger notebook execution.
 */
@Slf4j
@Component
public class FabricClient {

    private final RestTemplate restTemplate;
    private final DefaultAzureCredential azureCredential;
    private final String fabricBaseUrl;
    private final String workspaceId;
    private final String pipelineId;
    private final String pipelineName;

    public FabricClient(RestTemplate restTemplate,
                        DefaultAzureCredential azureCredential,
                        @Value("${azure.fabric.fabric-base-url}") String fabricBaseUrl,
                        @Value("${azure.fabric.workspace-id}") String workspaceId,
                        @Value("${azure.fabric.pipeline-id}") String pipelineId,
                        @Value("${azure.fabric.pipeline-name}") String pipelineName) {
        this.restTemplate = restTemplate;
        this.azureCredential = azureCredential;
        this.fabricBaseUrl = fabricBaseUrl;
        this.workspaceId = workspaceId;
        this.pipelineId = pipelineId;
        this.pipelineName = pipelineName;
    }

    /**
     * Triggers the execution of a pipeline with the provided parameters in Microsoft Fabric.
     *
     * @param request           the pipeline execution request containing parameters
     * @param correlationId     the correlation ID for request tracking
     * @return FabricPipelineResponse with job details
     * @throws RestClientException if the API call fails
     */
    public FabricPipelineResponse runOnDemandPipeline(FabricPipelineRequest request, String correlationId) {
        log.info("Triggering Fabric pipeline {} for user {} with correlationId: {}",
                 pipelineName, request.getUserId(), correlationId);

        try {
            String url = buildPipelineUrl();
            HttpEntity<Map<String, Object>> entity = createRequestEntity(request, correlationId);

            ResponseEntity<FabricPipelineResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                FabricPipelineResponse.class
            );

            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                log.info("Successfully queued Fabric pipeline job. Status: {}, JobId: {}",
                         response.getStatusCode(),
                         response.getBody() != null ? response.getBody().getJobId() : "unknown");
                return response.getBody();
            } else {
                log.warn("Unexpected status code from Fabric API: {}", response.getStatusCode());
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.error("Error triggering Fabric pipeline for user {}: {}", request.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Builds the Fabric API URL for the on-demand pipeline execution endpoint.
     *
     * @return the complete URL for pipeline execution
     */
    private String buildPipelineUrl() {
        return UriComponentsBuilder.fromUriString(fabricBaseUrl)
            .path("/workspaces/{workspaceId}/items/{itemId}/jobs/instances")
            .queryParam("jobType", "Pipeline")
            .buildAndExpand(workspaceId, pipelineId)
            .toUriString();
    }

    /**
     * Creates an HTTP entity with headers and request body for Fabric API call.
     *
     * @param request       the pipeline execution request
     * @param correlationId the correlation ID
     * @return HttpEntity with headers and body
     */
    private HttpEntity<Map<String, Object>> createRequestEntity(FabricPipelineRequest request, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_CORRELATION_ID, correlationId);
        // Azure authentication is handled transparently via DefaultAzureCredential

        Map<String, Object> body = new HashMap<>();
        body.put("parameters", request.getParametersAsMap());

        return new HttpEntity<>(body, headers);
    }
}
