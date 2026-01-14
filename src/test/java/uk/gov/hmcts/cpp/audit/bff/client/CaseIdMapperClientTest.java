package uk.gov.hmcts.cpp.audit.bff.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.CaseIdMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CaseIdMapperClientTest {

    private SystemIdMapperClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new SystemIdMapperClient(restTemplate, "http://localhost:8080", "test-user-id", "/system-id-mappers", "application/json");
    }

    @Test
    void shouldGetMappingsByCaseUrns() {
        String json = """
            {
              "systemIds": [
                {
                  "sourceId": "URN01",
                  "sourceType": "SystemACaseId",
                  "targetId": "id-1",
                  "targetType": "TFL"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("sourceIds=URN01")))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<CaseIdMapper> result = client.getMappingsByCaseUrns("URN01", "TFL", "corr-id");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).caseId()).isEqualTo("id-1");
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenResponseBodyIsNullForGetMappingsByCaseUrns() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("sourceIds=URN01")))
            .andRespond(withStatus(HttpStatus.NO_CONTENT));

        List<CaseIdMapper> result = client.getMappingsByCaseUrns("URN01", "TFL", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenSystemIdsIsNullForGetMappingsByCaseUrns() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("sourceIds=URN01")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<CaseIdMapper> result = client.getMappingsByCaseUrns("URN01", "TFL", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionOnServerErrorForGetMappingsByCaseUrns() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("sourceIds=URN01")))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns("URN01", "TFL", "corr-id"))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldGetMappingsByCaseIds() {
        String json = """
            {
              "systemIds": [
                {
                  "sourceId": "URN01",
                  "sourceType": "SystemACaseId",
                  "targetId": "id-1",
                  "targetType": "TFL"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("targetIds=id-1")))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<CaseIdMapper> result = client.getMappingsByCaseIds("id-1", "TFL", "corr-id");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).caseUrn()).isEqualTo("URN01");
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenResponseBodyIsNullForGetMappingsByCaseIds() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("targetIds=id-1")))
            .andRespond(withStatus(HttpStatus.NO_CONTENT));

        List<CaseIdMapper> result = client.getMappingsByCaseIds("id-1", "TFL", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldReturnEmptyListWhenSystemIdsIsNullForGetMappingsByCaseIds() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("targetIds=id-1")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<CaseIdMapper> result = client.getMappingsByCaseIds("id-1", "TFL", "corr-id");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionOnServerErrorForGetMappingsByCaseIds() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("targetIds=id-1")))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getMappingsByCaseIds("id-1", "TFL", "corr-id"))
            .isInstanceOf(RestClientException.class);

        mockServer.verify();
    }
}
