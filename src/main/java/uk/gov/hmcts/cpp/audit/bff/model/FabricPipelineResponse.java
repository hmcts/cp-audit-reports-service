package uk.gov.hmcts.cpp.audit.bff.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response model for Fabric pipeline execution.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabricPipelineResponse {

    @JsonProperty("id")
    private String jobId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdTimeUtc")
    private String createdTimeUtc;

    @JsonProperty("rootActivityId")
    private String rootActivityId;
}
