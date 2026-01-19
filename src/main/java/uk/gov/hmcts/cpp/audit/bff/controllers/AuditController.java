package uk.gov.hmcts.cpp.audit.bff.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineRequest;
import uk.gov.hmcts.cpp.audit.bff.model.FabricPipelineResponse;
import uk.gov.hmcts.cpp.audit.bff.service.FabricService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for audit-related operations including pipeline execution.
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "Audit report APIs")
public class AuditController {

    private final FabricService fabricService;

    public AuditController(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    /**
     * Triggers the execution of the audit pipeline with the specified parameters.
     *
     * @param requestingUser the email address of the user being searched
     * @param userId         the user ID of the logged-in user
     * @param fromDateUtc    the start date for audit data in UTC format (YYYY-MM-DD)
     * @param toDateUtc      the end date for audit data in UTC format (YYYY-MM-DD)
     * @param correlationId  the correlation ID for request tracking (optional; generated if not provided)
     * @return ResponseEntity with 202 Accepted status and job details
     */
    @PostMapping("/pipeline/run")
    @Operation(summary = "Trigger audit pipeline execution",
        description = "Initiates the execution of the audit pipeline in Microsoft Fabric with the provided parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Pipeline execution queued successfully",
            content = @Content(schema = @Schema(implementation = FabricPipelineResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FabricPipelineResponse> triggerAuditPipeline(
        @RequestParam("requestingUser") String requestingUser,
        @RequestParam("userId") String userId,
        @RequestParam("fromDateUtc") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDateUtc,
        @RequestParam("toDateUtc") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDateUtc,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String finalCorrelationId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        log.info("Received audit pipeline trigger request for user: {} with correlationId: {}",
                 userId, finalCorrelationId);

        FabricPipelineRequest request = FabricPipelineRequest.builder()
            .requestingUser(requestingUser)
            .userId(userId)
            .fromDateUtc(fromDateUtc)
            .toDateUtc(toDateUtc)
            .build();

        FabricPipelineResponse response = fabricService.triggerAuditPipeline(request, finalCorrelationId);

        log.info("Pipeline execution queued successfully with jobId: {}",
                 response != null ? response.getJobId() : "unknown");

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
}
