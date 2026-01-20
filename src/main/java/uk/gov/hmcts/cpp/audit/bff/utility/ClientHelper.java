package uk.gov.hmcts.cpp.audit.bff.utility;

import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static uk.gov.hmcts.cpp.audit.bff.constants.HeaderConstants.HEADER_CORRELATION_ID;
import static uk.gov.hmcts.cpp.audit.bff.constants.HeaderConstants.HEADER_USER;

public interface ClientHelper {

    static <T, R> List<R> getResults(
        Logger log, RestTemplate restTemplate, String correlationId, Class<T> clazz,
        String serviceUrl, String path, String headerUserId, String acceptHeader,
        Map<String, List<String>> queryParams,
        Function<T, List<R>> getIds
    ) {
        try {
            log.debug("Calling Service API for IDs: {}", queryParams);

            final var url = UriComponentsBuilder
                .fromUriString(serviceUrl).path(path)
                .queryParams(CollectionUtils.toMultiValueMap(queryParams))
                .toUriString();

            final var response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(
                    new HttpHeaders(CollectionUtils.toMultiValueMap(Map.of(
                        HEADER_USER, List.of(headerUserId),
                        HEADER_CORRELATION_ID, List.of(correlationId),
                        "Accept", List.of(acceptHeader)))
                    )
                ), clazz
            );

            final var result = Optional.ofNullable(response.getBody())
                .map(getIds).orElse(List.of());

            log.debug("Retrieved {} for IDs: {}", result.size(), queryParams);
            return result;

        } catch (RestClientException e) {
            log.error("Error calling Service API for IDs: {}", queryParams, e);
            throw e;
        }
    }
}
