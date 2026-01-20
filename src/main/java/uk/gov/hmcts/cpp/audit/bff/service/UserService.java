package uk.gov.hmcts.cpp.audit.bff.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.User;
import uk.gov.hmcts.cpp.audit.bff.model.UserResponse;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.cpp.audit.bff.utility.ClientHelper.getResults;

@Slf4j
@Service
public record UserService(
    RestTemplate restTemplate,
    @Value("${cqrs.client.base-url}") String userServiceUrl,
    @Value("${cqrs.client.headers.cjs-cppuid}") String headerUserId,
    @Value("${cqrs.client.user-groups.user-group-path}") String userAndGroupPath,
    @Value("${cqrs.client.user-groups.accept-header}") String acceptHeader
) {
    public List<User> getUsersByEmails(List<String> emails, String correlationId) {
        log.info("Requesting users for emails: {} from UserClient", emails);

        final var result = getUserResults("emails", emails, correlationId);

        log.debug("Received {} users for emails: {}", result.size(), emails);
        return result;
    }

    public List<User> getEmailByUserId(List<String> userIds, String correlationId) {
        log.info("Requesting users for IDs: {} from UserClient", userIds);

        final var result = getUserResults("userIds", userIds, correlationId);

        log.debug("Received {} users for IDs: {}", result.size(), userIds);
        return result;
    }

    private List<User> getUserResults(String paramName, List<String> params, String correlationId) {
        return getResults(
            log, restTemplate, correlationId, UserResponse.class,
            userServiceUrl, userAndGroupPath, headerUserId, acceptHeader,
            Map.of(paramName, params),
            UserResponse::users
        );
    }
}
