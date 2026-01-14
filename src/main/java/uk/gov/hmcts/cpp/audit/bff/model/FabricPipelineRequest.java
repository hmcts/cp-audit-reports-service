package uk.gov.hmcts.cpp.audit.bff.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request model for executing a pipeline in Microsoft Fabric.
 */
@Getter
@Setter
@Builder
public class FabricPipelineRequest {

    @NonNull
    private String requestingUser;

    @NonNull
    private String userId;

    @NonNull
    private LocalDate fromDateUtc;

    @NonNull
    private LocalDate toDateUtc;

    /**
     * Converts the request parameters to a map format required by Fabric API.
     *
     * @return map of parameter names to values
     */
    public Map<String, Object> getParametersAsMap() {
        return Map.of(
            "requestinguser", requestingUser,
            "userid", userId,
            "from_dateutc", fromDateUtc.toString(),
            "to_dateutc", toDateUtc.toString()
        );
    }
}
