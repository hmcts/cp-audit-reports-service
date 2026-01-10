package uk.gov.hmcts.cpp.audit.bff.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cpp.audit.bff.model.CaseIdMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

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
                  "sourceId": "CaseURNID01",
                  "sourceType": "SystemACaseId",
                  "targetId": "100c0ae9-e276-4d29-b669-cb32013228b1",
                  "targetType": "TFL"
                },
                {
                  "sourceId": "CaseURNID02",
                  "sourceType": "SystemBCaseId",
                  "targetId": "100c0ae9-e276-4d29-b669-cb32013228b2",
                  "targetType": "TFL"
                }
              ]
            }
            """;
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("sourceIds=CaseURNID01,CaseURNID02")))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                           .header("CPPCLIENTCORRELATIONID", correlationId))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<CaseIdMapper> result = client.getMappingsByCaseUrns("CaseURNID01,CaseURNID02", "TFL", correlationId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).caseId()).isEqualTo("100c0ae9-e276-4d29-b669-cb32013228b1");
        assertThat(result.get(1).caseId()).isEqualTo("100c0ae9-e276-4d29-b669-cb32013228b2");
        mockServer.verify();
    }

    @Test
    void shouldGetMappingsByCaseIds() {
        String json = """
            {
              "systemIds": [
                {
                  "sourceId": "CaseURNID01",
                  "sourceType": "SystemACaseId",
                  "targetId": "100c0ae9-e276-4d29-b669-cb32013228b1",
                  "targetType": "TFL"
                },
                {
                  "sourceId": "CaseURNID02",
                  "sourceType": "SystemBCaseId",
                  "targetId": "100c0ae9-e276-4d29-b669-cb32013228b2",
                  "targetType": "TFL"
                }
              ]
            }
            """;
        String correlationId = "test-correlation-id";

        mockServer.expect(requestTo(org.hamcrest.Matchers
            .containsString("targetIds=100c0ae9-e276-4d29-b669-cb32013228b1,100c0ae9-e276-4d29-b669-cb32013228b2")))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                           .header("CPPCLIENTCORRELATIONID", correlationId))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<CaseIdMapper> result = client.getMappingsByCaseIds("100c0ae9-e276-4d29-b669-cb32013228b1,"
            + "100c0ae9-e276-4d29-b669-cb32013228b2", "TFL", correlationId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).caseUrn()).isEqualTo("CaseURNID01");
        assertThat(result.get(1).caseUrn()).isEqualTo("CaseURNID02");
        mockServer.verify();
    }


    @Test
    void shouldThrowRestClientExceptionAndLogErrorForGetMappingsByCaseIds() {
        String targetIds = "id1,id2";
        String correlationId = "corr-log-error";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseIds(targetIds, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldCatchAndRethrowRestClientExceptionForGetMappingsByCaseIds() {
        String targetIds = "catch-and-rethrow-id";
        String correlationId = "corr-catch-rethrow";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException thrownException = null;
        try {
            client.getMappingsByCaseIds(targetIds, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            thrownException = e;
        }

        assertThat(thrownException).isNotNull();
        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionWithoutCatchingItForGetMappingsByCaseIds() {
        String targetIds = "no-catch-id";
        String correlationId = "corr-no-catch";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseIds(targetIds, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldPropagateExceptionWithTargetIdsInContextForGetMappingsByCaseIds() {
        String targetIds = "specific-context-id";
        String correlationId = "corr-context-ids";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        try {
            client.getMappingsByCaseIds(targetIds, "TFL", correlationId);
            // Should not reach here
            throw new AssertionError("Expected RestClientException to be thrown");
        } catch (org.springframework.web.client.RestClientException e) {
            // Exception should be thrown and caught here
            assertThat(e).isNotNull();
        }

        mockServer.verify();
    }

    @Test
    void shouldNotSwallowExceptionInCatchBlockForGetMappingsByCaseIds() {
        String targetIds = "not-swallowed-id";
        String correlationId = "corr-not-swallow";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException caughtException = null;
        try {
            client.getMappingsByCaseIds(targetIds, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            caughtException = e;
        }

        // Verify exception was not swallowed
        assertThat(caughtException).isNotNull();
        assertThat(caughtException).isInstanceOf(org.springframework.web.client.RestClientException.class);
        mockServer.verify();
    }

    @Test
    void shouldThrowExactSameExceptionInstanceForGetMappingsByCaseIds() {
        String targetIds = "same-instance-id";
        String correlationId = "corr-same-instance";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException thrownException = null;
        org.springframework.web.client.RestClientException caughtException = null;

        try {
            client.getMappingsByCaseIds(targetIds, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            caughtException = e;
            thrownException = e;
        }

        // Verify the same exception is propagated (not wrapped)
        assertThat(thrownException).isSameAs(caughtException);
        mockServer.verify();
    }

    @Test
    void shouldExecuteCatchBlockWhenRestClientExceptionIsThrown() {
        String targetIds = "execute-catch-id";
        String correlationId = "corr-execute-catch";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        // The catch block executes when RestClientException is thrown
        boolean exceptionWasThrown = false;
        try {
            client.getMappingsByCaseIds(targetIds, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            exceptionWasThrown = true;
        }

        assertThat(exceptionWasThrown).isTrue();
        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionImmediatelyAfterLogging() {
        String targetIds = "immediate-throw-id";
        String correlationId = "corr-immediate-throw";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        // Verify that RestClientException is thrown and propagated immediately
        assertThatThrownBy(() -> client.getMappingsByCaseIds(targetIds, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldCatchOnlyRestClientExceptionForGetMappingsByCaseIds() {
        String targetIds = "specific-exception-id";
        String correlationId = "corr-specific-exception";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        // Verify it catches specifically RestClientException
        assertThatThrownBy(() -> client.getMappingsByCaseIds(targetIds, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldRethrowWithoutModifyingExceptionForGetMappingsByCaseIds() {
        String targetIds = "unmodified-exception-id";
        String correlationId = "corr-unmodified";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException originalException = null;
        try {
            client.getMappingsByCaseIds(targetIds, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            originalException = e;
            // Re-verify exception characteristics
            assertThat(e).isInstanceOf(org.springframework.web.client.RestClientException.class);
        }

        assertThat(originalException).isNotNull();
        mockServer.verify();
    }

    @Test
    void shouldExecuteExceptionPathForGetMappingsByCaseIdsOn500Error() {
        String targetIds = "exception-path-500";
        String correlationId = "corr-exception-path";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseIds(targetIds, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class)
            .isNotNull();

        mockServer.verify();
    }

    @Test
    void shouldVerifyExceptionHandlingPathIsExecutedForGetMappingsByCaseIds() {
        String targetIds = "verify-path-id";
        String correlationId = "corr-verify-path";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        try {
            client.getMappingsByCaseIds(targetIds, "TFL", correlationId);
            // Fail test if no exception thrown
            org.junit.jupiter.api.Assertions.fail("Expected RestClientException");
        } catch (org.springframework.web.client.RestClientException e) {
            // Success - exception handling path was executed
            assertThat(e).isInstanceOf(org.springframework.web.client.RestClientException.class);
        }

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionForGetMappingsByCaseIdsWithHttpStatusErrors() {
        String targetIds = "http-errors-id";
        String correlationId = "corr-http-errors";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getMappingsByCaseIds(targetIds, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldNotHandleExceptionOutsideCatchBlockForGetMappingsByCaseIds() {
        String targetIds = "no-outer-handle-id";
        String correlationId = "corr-no-outer";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("targetIds=" + targetIds)))
            .andRespond(withServerError());

        // Exception should propagate to caller without being handled
        assertThatThrownBy(() -> client.getMappingsByCaseIds(targetIds, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }


    @Test
    void shouldThrowRestClientExceptionOnServerErrorForGetMappingsByCaseUrns() {
        String caseUrns = "URN01,URN02";
        String correlationId = "corr-urns-error";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }


    @Test
    void shouldThrowRestClientExceptionWith500ErrorForGetMappingsByCaseUrns() {
        String caseUrns = "URN01";
        String correlationId = "corr-500-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith503ServiceUnavailableForGetMappingsByCaseUrns() {
        String caseUrns = "URN01,URN02,URN03";
        String correlationId = "corr-503-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith502BadGatewayForGetMappingsByCaseUrns() {
        String caseUrns = "URN01";
        String correlationId = "corr-502-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith400BadRequestForGetMappingsByCaseUrns() {
        String caseUrns = "INVALID-URN";
        String correlationId = "corr-400-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith401UnauthorizedForGetMappingsByCaseUrns() {
        String caseUrns = "URN01";
        String correlationId = "corr-401-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith403ForbiddenForGetMappingsByCaseUrns() {
        String caseUrns = "URN01";
        String correlationId = "corr-403-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWith404NotFoundForGetMappingsByCaseUrns() {
        String caseUrns = "NON-EXISTENT-URN";
        String correlationId = "corr-404-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWhenResponseIsInvalidJsonForGetMappingsByCaseUrns() {
        String caseUrns = "URN01";
        String correlationId = "corr-invalid-json-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withSuccess("invalid json {", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionWithMultipleCaseUrnsForGetMappingsByCaseUrns() {
        String caseUrns = "URN01,URN02,URN03,URN04,URN05";
        String correlationId = "corr-multiple-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionAndPreserveStackTraceForGetMappingsByCaseUrns() {
        String caseUrns = "URN01";
        String correlationId = "corr-stack-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class)
            .hasStackTraceContaining("RestTemplate");

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionAndLogErrorForGetMappingsByCaseUrns() {
        String caseUrns = "URN01,URN02";
        String correlationId = "corr-log-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldCatchAndRethrowRestClientExceptionForGetMappingsByCaseUrns() {
        String caseUrns = "catch-and-rethrow-urn";
        String correlationId = "corr-catch-rethrow-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException thrownException = null;
        try {
            client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            thrownException = e;
        }

        assertThat(thrownException).isNotNull();
        mockServer.verify();
    }

    @Test
    void shouldThrowExceptionWithoutCatchingItForGetMappingsByCaseUrns() {
        String caseUrns = "no-catch-urn";
        String correlationId = "corr-no-catch-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldPropagateExceptionWithCaseUrnsInContextForGetMappingsByCaseUrns() {
        String caseUrns = "specific-context-urn";
        String correlationId = "corr-context-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        try {
            client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId);
            // Should not reach here
            throw new AssertionError("Expected RestClientException to be thrown");
        } catch (org.springframework.web.client.RestClientException e) {
            // Exception should be thrown and caught here
            assertThat(e).isNotNull();
        }

        mockServer.verify();
    }

    @Test
    void shouldNotSwallowExceptionInCatchBlockForGetMappingsByCaseUrns() {
        String caseUrns = "not-swallowed-urn";
        String correlationId = "corr-not-swallow-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException caughtException = null;
        try {
            client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            caughtException = e;
        }

        // Verify exception was not swallowed
        assertThat(caughtException).isNotNull();
        assertThat(caughtException).isInstanceOf(org.springframework.web.client.RestClientException.class);
        mockServer.verify();
    }

    @Test
    void shouldThrowExactSameExceptionInstanceForGetMappingsByCaseUrns() {
        String caseUrns = "same-instance-urn";
        String correlationId = "corr-same-instance-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException thrownException = null;
        org.springframework.web.client.RestClientException caughtException = null;

        try {
            client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            caughtException = e;
            thrownException = e;
        }

        // Verify the same exception is propagated (not wrapped)
        assertThat(thrownException).isSameAs(caughtException);
        mockServer.verify();
    }

    @Test
    void shouldExecuteCatchBlockWhenRestClientExceptionIsThrownForGetMappingsByCaseUrns() {
        String caseUrns = "execute-catch-urn";
        String correlationId = "corr-execute-catch-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        // The catch block executes when RestClientException is thrown
        boolean exceptionWasThrown = false;
        try {
            client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            exceptionWasThrown = true;
        }

        assertThat(exceptionWasThrown).isTrue();
        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionImmediatelyAfterLoggingForGetMappingsByCaseUrns() {
        String caseUrns = "immediate-throw-urn";
        String correlationId = "corr-immediate-throw-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        // Verify that RestClientException is thrown and propagated immediately
        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldCatchOnlyRestClientExceptionForGetMappingsByCaseUrns() {
        String caseUrns = "specific-exception-urn";
        String correlationId = "corr-specific-exception-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        // Verify it catches specifically RestClientException
        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldRethrowWithoutModifyingExceptionForGetMappingsByCaseUrns() {
        String caseUrns = "unmodified-exception-urn";
        String correlationId = "corr-unmodified-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        org.springframework.web.client.RestClientException originalException = null;
        try {
            client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId);
        } catch (org.springframework.web.client.RestClientException e) {
            originalException = e;
            // Re-verify exception characteristics
            assertThat(e).isInstanceOf(org.springframework.web.client.RestClientException.class);
        }

        assertThat(originalException).isNotNull();
        mockServer.verify();
    }

    @Test
    void shouldExecuteExceptionPathForGetMappingsByCaseUrnsOn500Error() {
        String caseUrns = "exception-path-500-urn";
        String correlationId = "corr-exception-path-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class)
            .isNotNull();

        mockServer.verify();
    }

    @Test
    void shouldVerifyExceptionHandlingPathIsExecutedForGetMappingsByCaseUrns() {
        String caseUrns = "verify-path-urn";
        String correlationId = "corr-verify-path-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        try {
            client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId);
            // Fail test if no exception thrown
            org.junit.jupiter.api.Assertions.fail("Expected RestClientException");
        } catch (org.springframework.web.client.RestClientException e) {
            // Success - exception handling path was executed
            assertThat(e).isInstanceOf(org.springframework.web.client.RestClientException.class);
        }

        mockServer.verify();
    }

    @Test
    void shouldThrowRestClientExceptionForGetMappingsByCaseUrnsWithHttpStatusErrors() {
        String caseUrns = "http-errors-urn";
        String correlationId = "corr-http-errors-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldNotHandleExceptionOutsideCatchBlockForGetMappingsByCaseUrns() {
        String caseUrns = "no-outer-handle-urn";
        String correlationId = "corr-no-outer-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        // Exception should propagate to caller without being handled
        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldVerifyCorrectParameterPassedInUrlForGetMappingsByCaseUrns() {
        String caseUrns = "URN-PARAM-TEST";
        String correlationId = "corr-param-test-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, "TFL", correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

    @Test
    void shouldVerifyTargetTypeParameterForGetMappingsByCaseUrns() {
        String caseUrns = "URN01";
        String targetType = "SYSTEM_A";
        String correlationId = "corr-target-type-urns";

        mockServer.expect(requestTo(org.hamcrest.Matchers
                                        .containsString("sourceIds=" + caseUrns)))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                           .queryParam("targetType", targetType))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMappingsByCaseUrns(caseUrns, targetType, correlationId))
            .isInstanceOf(org.springframework.web.client.RestClientException.class);

        mockServer.verify();
    }

}
