
package uk.gov.hmcts.cpp.audit.bff.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cpp.audit.bff.model.ErrorResponse;
import uk.gov.hmcts.cpp.audit.bff.model.User;
import uk.gov.hmcts.cpp.audit.bff.service.UserService;

import java.util.List;

import static uk.gov.hmcts.cpp.audit.bff.constants.HeaderConstants.HEADER_CORRELATION_ID;

@Slf4j
@RestController
public record UserController(UserService userService) {

    @Operation(summary = "Get Users by Emails", description = "Retrieves Users associated with "
        + "the provided email addresses.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "No users found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/email")
    public ResponseEntity<List<User>> getUsers(
        @Parameter(description = "Email addresses of the users (comma separated)", required = true)
        @RequestParam("emails") List<String> emails,
        @Parameter(description = "Correlation ID for tracking the request", required = true)
        @RequestHeader(HEADER_CORRELATION_ID) String correlationId
    ) {
        log.info("Fetching users for emails: {} with correlationId: {}", emails, correlationId);

        final var users = userService.getUsersByEmails(emails, correlationId);

        if (users.isEmpty()) {
            log.warn("No users found for emails: {} with correlationId: {}", emails, correlationId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                              "No users found for the provided email addresses");
        }

        log.debug("Successfully retrieved {} users for emails: {}", users.size(), emails);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get Users by User IDs", description = "Retrieves the user information associated with "
        + "the provided User IDs.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "No users found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/id")
    public ResponseEntity<List<User>> getEmail(
        @Parameter(description = "User IDs of the users (comma separated)", required = true)
        @RequestParam("userIds") List<String> userIds,
        @Parameter(description = "Correlation ID for tracking the request", required = true)
        @RequestHeader(HEADER_CORRELATION_ID) String correlationId
    ) {
        log.info("Fetching users for IDs: {} with correlationId: {}", userIds, correlationId);

        final var users = userService.getEmailByUserId(userIds, correlationId);

        if (users.isEmpty()) {
            log.warn("No users found for IDs: {} with correlationId: {}", userIds, correlationId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                              "No users found for the provided User IDs");
        }

        log.debug("Successfully retrieved {} users for IDs: {}", users.size(), userIds);
        return ResponseEntity.ok(users);
    }
}
