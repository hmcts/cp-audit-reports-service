package uk.gov.hmcts.cpp.audit.bff.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.cpp.audit.bff.client.FabricClient;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineRequest;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineResponse;

/**
 * Service for orchestrating Fabric pipeline execution.
 */
@Slf4j
@Service
public class FabricService {

    private final FabricClient fabricClient;

    public FabricService(FabricClient fabricClient) {
        this.fabricClient = fabricClient;
    }

    /**
     * Initiates the execution of the Fabric audit pipeline with the provided parameters.
     *
     * @param request       the pipeline execution request containing audit parameters
     * @param correlationId the correlation ID for request tracking
     * @return FabricPipelineResponse containing job details
     * @throws RestClientException if the API call fails
     */
    public FabricPipelineResponse triggerAuditPipeline(FabricPipelineRequest request, String correlationId) {
        log.debug("FabricService: Triggering audit pipeline for user: {}, correlationId: {}",
                  request.getUserId(), correlationId);

        return fabricClient.runOnDemandPipeline(request, correlationId);
    }
}
