package uk.gov.hmcts.cpp.audit.bff.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cpp.audit.bff.model.MaterialCase;
import uk.gov.hmcts.cpp.audit.bff.service.ProgressionService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    @Mock
    private ProgressionService progressionService;

    private MaterialController materialController;

    @BeforeEach
    void setUp() {
        materialController = new MaterialController(progressionService);
    }

    @Test
    void shouldReturnMaterialCasesWhenMaterialIdsFound() {
        final var materialIds = Arrays.asList("m1,m2".split(","));
        String correlationId = "corr-id";
        List<MaterialCase> materialCases = List.of(
            new MaterialCase("m1", "cd1", "c1", "u1"),
            new MaterialCase("m2", "cd2", "c2", "u2")
        );

        when(progressionService.getMaterialCase(materialIds, correlationId)).thenReturn(materialCases);

        ResponseEntity<List<MaterialCase>> response = materialController.getMaterial(materialIds, correlationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).isEqualTo(materialCases);
        verify(progressionService).getMaterialCase(materialIds, correlationId);
    }

    @Test
    void shouldReturnSingleMaterialCaseWhenMaterialIdFound() {
        final var materialIds = List.of("m1");
        String correlationId = "corr-id";
        List<MaterialCase> materialCases = List.of(new MaterialCase("m1", "cd1", "c1", "u1"));

        when(progressionService.getMaterialCase(materialIds, correlationId)).thenReturn(materialCases);

        ResponseEntity<List<MaterialCase>> response = materialController.getMaterial(materialIds, correlationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).materialId()).isEqualTo("m1");
        verify(progressionService).getMaterialCase(materialIds, correlationId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenMaterialIdsNotFound() {
        final var materialIds = Arrays.asList("missing1,missing2".split(","));
        String correlationId = "corr-id";

        when(progressionService.getMaterialCase(materialIds, correlationId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> materialController.getMaterial(materialIds, correlationId))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
            .hasMessageContaining("No Material Cases found for the provided Material IDs");

        verify(progressionService).getMaterialCase(materialIds, correlationId);
    }
}
