
package uk.gov.hmcts.cpp.audit.bff.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpMethod.GET;

class UserClientTest {

    private UserClient userClient;
    private MockRestServiceServer mockServer;

    private static final String RICHARD_USER_ID = "1e2f843e-d639-40b3-8611-8015f3a18958";
    private static final String SUSAN_USER_ID = "5g9f843e-d639-40b3-8611-8015f3a38293";
    private static final String RICHARD_EMAIL = "richard.chapman@acme.com";
    private static final String SUSAN_EMAIL = "Susan.Boora@acme.com";

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        userClient = new UserClient(
            restTemplate,
            "http://localhost:8080",
            "test-user",
            "/usersgroups-query-api/query/api/rest/usersgroups/users",
            "application/json"
        );
    }

    @Test
    void shouldReturnUsersWhenUserIdsAreProvided() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman@acme.com"
                },
                {
                  "userId": "5g9f843e-d639-40b3-8611-8015f3a38293",
                  "firstName": "Susan",
                  "lastName": "Boora",
                  "email": "Susan.Boora@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers
            .containsString("userIds=1e2f843e-d639-40b3-8611-8015f3a18958,5g9f843e-d639-40b3-8611-8015f3a38293")))
            .andExpect(method(GET))
            .andExpect(header("CJSCPPUID", "test-user"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> actualUsers = userClient.getUsers(RICHARD_USER_ID + "," + SUSAN_USER_ID, "corr-123");

        assertThat(actualUsers).hasSize(2);
        assertThat(actualUsers.get(0).firstName()).isEqualTo("Richard");
        assertThat(actualUsers.get(0).lastName()).isEqualTo("Chapman");
        assertThat(actualUsers.get(0).email()).isEqualTo(RICHARD_EMAIL);
        assertThat(actualUsers.get(1).firstName()).isEqualTo("Susan");
        assertThat(actualUsers.get(1).lastName()).isEqualTo("Boora");
        assertThat(actualUsers.get(1).email()).isEqualTo(SUSAN_EMAIL);
        mockServer.verify();
    }

    @Test
    void shouldReturnUsersWhenEmailsAreProvided() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman@acme.com"
                },
                {
                  "userId": "5g9f843e-d639-40b3-8611-8015f3a38293",
                  "firstName": "Susan",
                  "lastName": "Boora",
                  "email": "Susan.Boora@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("emails=richard.chapman@acme.com,Susan.Boora@acme.com")))
            .andExpect(method(GET))
            .andExpect(header("CJSCPPUID", "test-user"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> actualUsers = userClient.getUsersByEmail(RICHARD_EMAIL + "," + SUSAN_EMAIL, "corr-456");

        assertThat(actualUsers).hasSize(2);
        assertThat(actualUsers.get(0).userId()).isEqualTo(RICHARD_USER_ID);
        assertThat(actualUsers.get(0).email()).isEqualTo(RICHARD_EMAIL);
        assertThat(actualUsers.get(1).userId()).isEqualTo(SUSAN_USER_ID);
        assertThat(actualUsers.get(1).email()).isEqualTo(SUSAN_EMAIL);
        mockServer.verify();
    }

    @Test
    void shouldReturnSingleUser() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=" + RICHARD_USER_ID)))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers(RICHARD_USER_ID, "corr-single");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Richard");
        assertThat(result.get(0).lastName()).isEqualTo("Chapman");
        mockServer.verify();
    }

    @Test
    void shouldReturnSingleUserGetUsersByEmail() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=" + RICHARD_EMAIL)))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsersByEmail(RICHARD_EMAIL, "corr-single-email");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo(RICHARD_EMAIL);
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenNoUsers() {
        String responseJson = """
            {
              "users": null
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=nonexistent")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers("nonexistent", "corr-empty");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenNoUsersGetUsersByEmail() {
        String responseJson = """
            {
              "users": []
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=nonexistent@example.com")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsersByEmail("nonexistent@example.com", "corr-empty-email");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldIncludeCorrelationIdHeaderForGetUsers() {
        String responseJson = """
            {
              "users": []
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=user-1")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers("user-1", "corr-header-test");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldIncludeCorrelationIdHeaderForGetUsersByEmail() {
        String responseJson = """
            {
              "users": []
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=test@example.com")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsersByEmail("test@example.com", "corr-email-header");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldThrowOnServerErrorGetUsers() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=user-1")))
            .andRespond(withServerError());

        assertThrows(
            RestClientException.class, () ->
            userClient.getUsers("user-1", "corr-error")
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowOnServerErrorGetUsersByEmail() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=test@example.com")))
            .andRespond(withServerError());

        assertThrows(RestClientException.class, () ->
            userClient.getUsersByEmail("test@example.com", "corr-error")
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowOnClientErrorGetUsers() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=invalid")))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(RestClientException.class, () ->
            userClient.getUsers("invalid", "corr-404")
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowOnClientError() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=invalid@example.com")))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(RestClientException.class, () ->
            userClient.getUsersByEmail("invalid@example.com", "corr-400")
        );

        mockServer.verify();
    }

    @Test
    void shouldHandleMultipleUsersGetUsers() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman@acme.com"
                },
                {
                  "userId": "5g9f843e-d639-40b3-8611-8015f3a38293",
                  "firstName": "Susan",
                  "lastName": "Boora",
                  "email": "Susan.Boora@acme.com"
                },
                {
                  "userId": "7h3k943e-d639-40b3-8611-8015f3a49384",
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "john.smith@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers("id1,id2,id3", "corr-multi");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).firstName()).isEqualTo("Richard");
        assertThat(result.get(1).firstName()).isEqualTo("Susan");
        assertThat(result.get(2).firstName()).isEqualTo("John");
        mockServer.verify();
    }

    @Test
    void shouldHandleMultipleEmailsGetUsersByEmail() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman@acme.com"
                },
                {
                  "userId": "5g9f843e-d639-40b3-8611-8015f3a38293",
                  "firstName": "Susan",
                  "lastName": "Boora",
                  "email": "Susan.Boora@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsersByEmail("email1@test.com,email2@test.com", "corr-multi-email");

        assertThat(result).hasSize(2);
        mockServer.verify();
    }

    @Test
    void shouldVerifyRequestMethodGetUsers() {
        String responseJson = """
            {
              "users": []
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=test")))
            .andExpect(method(GET))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        userClient.getUsers("test", "corr-method");

        mockServer.verify();
    }

    @Test
    void shouldVerifyRequestMethodGetUsersByEmail() {
        String responseJson = """
            {
              "users": []
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=test@test.com")))
            .andExpect(method(GET))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        userClient.getUsersByEmail("test@test.com", "corr-method");

        mockServer.verify();
    }

    @Test
    void shouldVerifyUserIdHeaderGetUsers() {
        String responseJson = """
            {
              "users": []
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=test")))
            .andExpect(header("CJSCPPUID", "test-user"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        userClient.getUsers("test", "corr-user-header");

        mockServer.verify();
    }

    @Test
    void shouldHandleSpecialCharactersInEmailGetUsers() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman+test@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=test-special")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers("test-special", "corr-special");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).contains("+test");
        mockServer.verify();
    }

    @Test
    void shouldPreserveFirstNameAndLastNameCaseGetUsersByEmail() {
        String responseJson = """
            {
              "users": [
                {
                  "userId": "1e2f843e-d639-40b3-8611-8015f3a18958",
                  "firstName": "Richard",
                  "lastName": "Chapman",
                  "email": "richard.chapman@acme.com"
                },
                {
                  "userId": "5g9f843e-d639-40b3-8611-8015f3a38293",
                  "firstName": "Susan",
                  "lastName": "Boora",
                  "email": "Susan.Boora@acme.com"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("emails=")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsersByEmail(RICHARD_EMAIL + "," + SUSAN_EMAIL, "corr-case");

        assertThat(result.get(0).firstName()).isEqualTo("Richard");
        assertThat(result.get(1).firstName()).isEqualTo("Susan");
        mockServer.verify();
    }

    @Test
    void shouldReturnNullResponseBodyAsEmptyListGetUsers() {
        String responseJson = """
            {
              "users": null
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("userIds=test")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<User> result = userClient.getUsers("test", "corr-null-body");

        assertThat(result).isEmpty();
        mockServer.verify();
    }
}
