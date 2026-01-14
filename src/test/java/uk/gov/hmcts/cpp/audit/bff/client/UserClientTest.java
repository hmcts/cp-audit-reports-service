package uk.gov.hmcts.cpp.audit.bff.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserClientTest {

    private UserClient userClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        userClient = new UserClient(restTemplate, "http://localhost:8080", "test-user", "/users", "application/json");
    }

    @Test
    void shouldReturnUsersByUserId() {
        String responseJson = "{\"users\": [{\"userId\": \"u1\", \"email\": \"u1@test.com\"}]}";

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=u1")))
            .andExpect(header("CPPCLIENTCORRELATIONID", "corr-id"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers("u1", "corr-id");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo("u1");
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenGetUsersHasNullBody() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=u1")))
            .andRespond(withStatus(HttpStatus.NO_CONTENT));

        List<User> result = userClient.getUsers("u1", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenGetUsersHasNullUsersList() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=u1")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers("u1", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionWhenGetUsersFails() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=u1")))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> userClient.getUsers("u1", "corr-id"))
            .isInstanceOf(RestClientException.class);
    }

    @Test
    void shouldReturnUsersByEmail() {
        String responseJson = "{\"users\": [{\"userId\": \"u1\", \"email\": \"e1@test.com\"}]}";

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=e1@test.com")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsersByEmail("e1@test.com", "corr-id");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("e1@test.com");
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenGetUsersByEmailHasNullBody() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=e1@test.com")))
            .andRespond(withStatus(HttpStatus.NO_CONTENT));

        List<User> result = userClient.getUsersByEmail("e1@test.com", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenGetUsersByEmailHasNullUsersList() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=e1@test.com")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsersByEmail("e1@test.com", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionWhenGetUsersByEmailFails() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=e1@test.com")))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> userClient.getUsersByEmail("e1@test.com", "corr-id"))
            .isInstanceOf(RestClientException.class);
    }
}
