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
import uk.gov.hmcts.cpp.audit.bff.model.CaseIdMapper;
import uk.gov.hmcts.cpp.audit.bff.service.CaseService;

import java.util.List;

import static uk.gov.hmcts.cpp.audit.bff.constants.HeaderConstants.HEADER_CORRELATION_ID;

@Slf4j
@RestController
public record CaseController(CaseService caseService) {

    @Operation(summary = "Get Case ID by Case URN", description = "Retrieves the Case ID associated with "
        + "the provided Case URN(s).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case ID(s) found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CaseIdMapper.class))),
        @ApiResponse(responseCode = "404", description = "Case ID(s) not found for the given URN(s)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/case/urn")
    public ResponseEntity<List<CaseIdMapper>> getCaseId(
        @Parameter(description = "Case URNs (comma separated)", required = true)
        @RequestParam("caseUrns") List<String> caseUrns,
        @Parameter(description = "Correlation ID for tracking the request", required = true)
        @RequestHeader(HEADER_CORRELATION_ID) String correlationId
    ) {
        log.info("Fetching Case IDs for URNs: {} with correlationId: {}", caseUrns, correlationId);

        final var  caseIdMappers = caseService.getCaseIdByUrn(caseUrns, correlationId);

        if (caseIdMappers.isEmpty()) {
            log.warn("No Case IDs found for URNs: {} with correlationId: {}", caseUrns, correlationId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                              "No Case IDs found for the provided URNs");
        }

        log.debug("Successfully retrieved {} Case IDs for URNs: {}", caseIdMappers.size(), caseUrns);
        return ResponseEntity.ok(caseIdMappers);
    }

    @Operation(summary = "Get Case URN by Case ID", description = "Retrieves the Case URN associated with "
        + "the provided Case ID(s).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case URN(s) found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CaseIdMapper.class))),
        @ApiResponse(responseCode = "404", description = "Case URN(s) not found for the given ID(s)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/case/id")
    public ResponseEntity<List<CaseIdMapper>> getCaseUrn(
        @Parameter(description = "Case IDs (comma separated)", required = true)
        @RequestParam("caseIds") List<String> caseIds,
        @Parameter(description = "Correlation ID for tracking the request", required = true)
        @RequestHeader(HEADER_CORRELATION_ID) String correlationId
    ) {
        log.info("Fetching Case URNs for IDs: {} with correlationId: {}", caseIds, correlationId);

        final var caseIdMappers = caseService.getCaseUrnByCaseId(caseIds, correlationId);
        if (caseIdMappers.isEmpty()) {
            log.warn("No Case URNs found for IDs: {} with correlationId: {}", caseIds, correlationId);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No Case URNs found for the provided Case IDs");
        }

        log.debug("Successfully retrieved {} Case URNs for IDs: {}", caseIdMappers.size(), caseIds);
        return ResponseEntity.ok(caseIdMappers);
    }
}
