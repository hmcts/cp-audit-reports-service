package uk.gov.hmcts.cpp.audit.bff.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.MaterialCase;
import uk.gov.hmcts.cpp.audit.bff.model.MaterialCaseResponse;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.cpp.audit.bff.utility.ClientHelper.getResults;

@Slf4j
@Service
public record ProgressionService(
    RestTemplate restTemplate,
    @Value("${cqrs.client.base-url}") String progressionServiceUrl,
    @Value("${cqrs.client.headers.cjs-cppuid}") String headerUserId,
    @Value("${cqrs.client.progression.material-path}") String materialPath,
    @Value("${cqrs.client.progression.accept-header}") String acceptHeader
) {

    public List<MaterialCase> getMaterialCase(List<String> materialIds, String correlationId) {
        log.info("Requesting material cases for IDs: {} from ProgressionClient", materialIds);

        final var result = getResults(
            log, restTemplate, correlationId, MaterialCaseResponse.class,
            progressionServiceUrl, materialPath, headerUserId, acceptHeader,
            Map.of("materialIds", materialIds),
            MaterialCaseResponse::materialIds
        );

        log.debug("Received {} material cases for IDs: {}", result.size(), materialIds);
        return result;
    }
}
