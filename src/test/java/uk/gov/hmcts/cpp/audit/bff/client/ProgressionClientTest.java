package uk.gov.hmcts.cpp.audit.bff.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.MaterialCase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProgressionClientTest {

    private ProgressionClient progressionClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
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
    void shouldReturnEmptyListWhenResponseBodyIsNull() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("materialIds=m1")))
            .andRespond(withStatus(HttpStatus.NO_CONTENT));

        List<MaterialCase> result = progressionClient.getMaterialCases("m1", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenMaterialIdsListIsNullInBody() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("materialIds=m1")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<MaterialCase> result = progressionClient.getMaterialCases("m1", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionOnApiError() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("materialIds=m1")))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> progressionClient.getMaterialCases("m1", "corr-id"))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }
}
