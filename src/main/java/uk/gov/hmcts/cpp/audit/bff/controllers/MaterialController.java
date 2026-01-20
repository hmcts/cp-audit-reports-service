
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
import uk.gov.hmcts.cpp.audit.bff.model.MaterialCase;
import uk.gov.hmcts.cpp.audit.bff.service.ProgressionService;

import java.util.List;

import static uk.gov.hmcts.cpp.audit.bff.constants.HeaderConstants.HEADER_CORRELATION_ID;

@Slf4j
@RestController
public record MaterialController(ProgressionService progressionService) {

    @Operation(summary = "Get Material Cases by Material IDs", description = "Retrieves Material Cases associated with "
        + "the provided material IDs.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Material cases found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MaterialCase.class))),
        @ApiResponse(responseCode = "404", description = "No material cases found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/material/id")
    public ResponseEntity<List<MaterialCase>> getMaterial(
        @Parameter(description = "Material IDs (comma separated)", required = true)
        @RequestParam("materialIds") List<String> materialIds,
        @Parameter(description = "Correlation ID for tracking the request", required = true)
        @RequestHeader(HEADER_CORRELATION_ID) String correlationId
    ) {
        log.info("Fetching Material Cases for IDs: {} with correlationId: {}", materialIds, correlationId);

        final var materialCases = progressionService.getMaterialCase(materialIds, correlationId);

        if (materialCases.isEmpty()) {
            log.warn("No Material Cases found for IDs: {} with correlationId: {}", materialIds, correlationId);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No Material Cases found for the provided Material IDs");
        }

        log.debug("Successfully retrieved {} Material Cases for IDs: {}", materialCases.size(), materialIds);
        return ResponseEntity.ok(materialCases);
    }
}
