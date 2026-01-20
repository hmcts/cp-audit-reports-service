package uk.gov.hmcts.cpp.audit.bff.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.CaseIdMapper;
import uk.gov.hmcts.cpp.audit.bff.model.SystemIdMapperResponse;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.cpp.audit.bff.utility.ClientHelper.getResults;

@Slf4j
@Service
public record CaseService(
    RestTemplate restTemplate,
    @Value("${cqrs.client.base-url}") String systemIdMapperUrl,
    @Value("${cqrs.client.headers.cjs-cppuid}") String headerUserId,
    @Value("${cqrs.client.system-id-mappers.system-id-path}") String systemIdMapperPath,
    @Value("${cqrs.client.system-id-mappers.accept-header}") String acceptHeader
) {

    public List<CaseIdMapper> getCaseIdByUrn(List<String> caseUrns, String correlationId) {
        log.info("Requesting Case IDs for URNs: {} from SystemIdMapperClient", caseUrns);

        final var result = getMappingResults("sourceIds", caseUrns, correlationId);

        log.debug("Received {} mappings for URNs: {}", result.size(), caseUrns);
        return result;
    }

    public List<CaseIdMapper> getCaseUrnByCaseId(List<String> caseIds, String correlationId) {
        log.info("Requesting Case URNs for IDs: {} from SystemIdMapperClient", caseIds);

        final var result = getMappingResults("targetIds", caseIds, correlationId);

        log.debug("Received {} mappings for IDs: {}", result.size(), caseIds);
        return result;
    }

    private List<CaseIdMapper> getMappingResults(String paramName, List<String> params, String correlationId) {
        return getResults(
            log, restTemplate, correlationId, SystemIdMapperResponse.class,
            systemIdMapperUrl, systemIdMapperPath, headerUserId, acceptHeader,
            Map.of(paramName, params, "targetType", List.of("CASE_ID")),
            SystemIdMapperResponse::systemIds
        );
    }
}
