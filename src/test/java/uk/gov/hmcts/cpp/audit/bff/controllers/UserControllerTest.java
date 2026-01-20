
package uk.gov.hmcts.cpp.audit.bff.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cpp.audit.bff.model.User;
import uk.gov.hmcts.cpp.audit.bff.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
    }

    @Test
    void shouldReturnUsersWhenEmailsFound() {
        final var emails = Arrays.asList("found@example.com,john@doe.com".split(","));
        String correlationId = "corr-123";
        List<User> users = List.of(new User("id1", "First", "Last", "found@example.com"),
                                   new User("id2", "John", "Doe", "john@doe.com"));

        when(userService.getUsersByEmails(emails, correlationId)).thenReturn(users);

        ResponseEntity<List<User>> response = userController.getUsers(emails, correlationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).isEqualTo(users);
        verify(userService).getUsersByEmails(emails, correlationId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEmailsNotFound() {
        final var emails = Arrays.asList("missing@example.com,john@doe.com".split(","));
        String correlationId = "corr-123";

        when(userService.getUsersByEmails(emails, correlationId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> userController.getUsers(emails, correlationId))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
            .hasMessageContaining("No users found for the provided email addresses");

        verify(userService).getUsersByEmails(emails, correlationId);
    }

    @Test
    void shouldReturnUsersWhenUserIdsFound() {
        final var userIds = Arrays.asList("user-123,user-456".split(","));
        String correlationId = "corr-123";
        List<User> users = List.of(new User("user-123", "First", "Last", "email@example.com"),
                                   new User("user-456", "John", "Doe", "john@doe.com"));

        when(userService.getEmailByUserId(userIds, correlationId)).thenReturn(users);

        ResponseEntity<List<User>> response = userController.getEmail(userIds, correlationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).isEqualTo(users);
        verify(userService).getEmailByUserId(userIds, correlationId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserIdsNotFound() {
        final var userIds = List.of("missing-id");
        String correlationId = "corr-123";

        when(userService.getEmailByUserId(userIds, correlationId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> userController.getEmail(userIds, correlationId))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
            .hasMessageContaining("No users found for the provided User IDs");

        verify(userService).getEmailByUserId(userIds, correlationId);
    }
}
