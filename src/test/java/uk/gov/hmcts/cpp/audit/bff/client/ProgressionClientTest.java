package uk.gov.hmcts.cpp.audit.bff.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.MaterialCase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProgressionClientTest {

    private RestTemplate restTemplate;
    private ProgressionClient progressionClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        progressionClient = new ProgressionClient(restTemplate, "http://localhost:8080", "test-user", "/progression", "application/json");
    }

    @Test
    void shouldReturnListOfMaterialCasesForMaterialIds() {
        String responseJson = """
            {
              "materialIds": [
                {
                  "materialId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
                  "courtDocumentId": "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e",
                  "caseId": "c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f",
                  "caseUrn": "39GD1116822"
                },
                {
                  "materialId": "f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c",
                  "courtDocumentId": null,
                  "caseId": "a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d",
                  "caseUrn": "TFL122222"
                },
                {
                  "materialId": "b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e",
                  "courtDocumentId": "c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f",
                  "caseId": null,
                  "caseUrn": null
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers
            .containsString("materialIds=a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d,"
                                + "f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c,b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<MaterialCase> actualCases = progressionClient.getMaterialCases("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d,"
            + "f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c,b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e", "corr-id");

        assertThat(actualCases).hasSize(3);
        assertThat(actualCases.get(0).caseId()).isEqualTo("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f");
        assertThat(actualCases.get(1).caseId()).isEqualTo("a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d");
        assertNull(actualCases.get(2).caseId());
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenResponseIsNull() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("materialIds=m1")))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        List<MaterialCase> actualCases = progressionClient.getMaterialCases("m1", "corr-id");

        assertThat(actualCases).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldHandleMaterialCasesWithPartialNullFields() {
        String responseJson = """
            {
              "materialIds": [
                {
                  "materialId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
                  "courtDocumentId": "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e",
                  "caseId": "c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f",
                  "caseUrn": "39GD1116822"
                },
                {
                  "materialId": "f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c",
                  "courtDocumentId": null,
                  "caseId": "a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d",
                  "caseUrn": "TFL122222"
                },
                {
                  "materialId": "b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e",
                  "courtDocumentId": "c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f",
                  "caseId": null,
                  "caseUrn": null
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d,"
                                        + "f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c,b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<MaterialCase> actualCases = progressionClient.getMaterialCases("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d,"
                           + "f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c,b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e", "corr-id");

        assertThat(actualCases).hasSize(3);
        assertThat(actualCases.get(0))
            .hasFieldOrPropertyWithValue("materialId", "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d")
            .hasFieldOrPropertyWithValue("courtDocumentId", "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e")
            .hasFieldOrPropertyWithValue("caseUrn", "39GD1116822");

        assertThat(actualCases.get(1))
            .hasFieldOrPropertyWithValue("courtDocumentId", null)
            .hasFieldOrPropertyWithValue("caseUrn", "TFL122222");

        assertThat(actualCases.get(2))
            .hasFieldOrPropertyWithValue("caseId", null)
            .hasFieldOrPropertyWithValue("caseUrn", null);

        mockServer.verify();
    }

    @Test
    void shouldReturnSingleMaterialCase() {
        String responseJson = """
            {
              "materialIds": [
                {
                  "materialId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
                  "courtDocumentId": "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e",
                  "caseId": "c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f",
                  "caseUrn": "39GD1116822"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<MaterialCase> actualCases = progressionClient
            .getMaterialCases("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", "corr-id");

        assertThat(actualCases).hasSize(1);
        assertThat(actualCases.get(0).materialId()).isEqualTo("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
        assertThat(actualCases.get(0).caseUrn()).isEqualTo("39GD1116822");
        mockServer.verify();
    }

    @Test
    void shouldVerifyCorrelationIdIsPassedInHeaders() {
        String responseJson = """
            {
              "materialIds": []
            }
            """;
        String correlationId = "test-correlation-id-12345";

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("materialIds=test-id")))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                           .header("CPPCLIENTCORRELATIONID", correlationId))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        progressionClient.getMaterialCases("test-id", correlationId);

        mockServer.verify();
    }

    @Test
    void shouldReturnMaterialCasesWithAllFieldsPopulated() {
        String responseJson = """
            {
              "materialIds": [
                {
                  "materialId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
                  "courtDocumentId": "b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e",
                  "caseId": "c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f",
                  "caseUrn": "39GD1116822"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<MaterialCase> actualCases = progressionClient
            .getMaterialCases("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", "corr-id");

        assertThat(actualCases).hasSize(1);
        MaterialCase materialCase = actualCases.get(0);
        assertThat(materialCase.materialId()).isEqualTo("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
        assertThat(materialCase.courtDocumentId()).isEqualTo("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");
        assertThat(materialCase.caseId()).isEqualTo("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f");
        assertThat(materialCase.caseUrn()).isEqualTo("39GD1116822");
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenMaterialIdsArrayIsEmpty() {
        String responseJson = """
            {
              "materialIds": []
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("materialIds=empty-id")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<MaterialCase> actualCases = progressionClient.getMaterialCases("empty-id", "corr-id");

        assertThat(actualCases).isEmpty();
        mockServer.verify();
    }


    @Test
    void shouldThrowRestClientExceptionWhenApiCallFails() {
        String materialIds = "m1,m2";
        String correlationId = "corr-error-id";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionOnNetworkError() {
        String materialIds = "m1";
        String correlationId = "corr-network-error";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith503ServiceUnavailable() {
        String materialIds = "m1,m2,m3";
        String correlationId = "corr-service-unavailable";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith400BadRequest() {
        String materialIds = "invalid-format";
        String correlationId = "corr-bad-request";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith401Unauthorized() {
        String materialIds = "m1";
        String correlationId = "corr-unauthorized";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith403Forbidden() {
        String materialIds = "m1";
        String correlationId = "corr-forbidden";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith404NotFound() {
        String materialIds = "non-existent-material";
        String correlationId = "corr-not-found";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith502BadGateway() {
        String materialIds = "m1,m2";
        String correlationId = "corr-bad-gateway";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWhenResponseIsInvalidJson() {
        String materialIds = "m1";
        String correlationId = "corr-invalid-json";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withSuccess("invalid json {", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionForConnectionTimeout() {
        String materialIds = "m1";
        String correlationId = "corr-timeout";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldPropagateRestClientExceptionWithCorrectMaterialIds() {
        String materialIds = "specific-material-id-12345";
        String correlationId = "corr-specific-error";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionAndPreserveStackTrace() {
        String materialIds = "m1";
        String correlationId = "corr-stack-trace";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class)
            .hasStackTraceContaining("RestTemplate");

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWithMultipleMaterialIds() {
        String materialIds = "m1,m2,m3,m4,m5";
        String correlationId = "corr-multiple-ids-error";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("materialIds=" + materialIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> progressionClient.getMaterialCases(materialIds, correlationId))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }
}
