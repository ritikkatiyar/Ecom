package com.ecom.search.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecom.search.dto.RelevanceCaseResultResponse;
import com.ecom.search.dto.RelevanceDatasetHealthResponse;
import com.ecom.search.dto.RelevanceEvaluationResponse;
import com.ecom.search.service.SearchUseCases;

@WebMvcTest(SearchController.class)
class SearchControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchUseCases searchUseCases;

    @Test
    void shouldExposeRelevanceEvaluationContract() throws Exception {
        RelevanceEvaluationResponse response = new RelevanceEvaluationResponse(
                2,
                2,
                0,
                100.0,
                85.0,
                true,
                Instant.parse("2026-02-14T10:00:00Z"),
                List.of(new RelevanceCaseResultResponse(
                        "iphone 15",
                        "prod-iphone-15",
                        "prod-iphone-15",
                        true,
                        true,
                        List.of("prod-iphone-15"))));
        when(searchUseCases.evaluateRelevanceDataset(anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/search/admin/relevance/evaluate").param("topN", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetSize").value(2))
                .andExpect(jsonPath("$.passRate").value(100.0))
                .andExpect(jsonPath("$.targetPassRate").value(85.0))
                .andExpect(jsonPath("$.meetsTarget").value(true))
                .andExpect(jsonPath("$.results[0].query").value("iphone 15"));
    }

    @Test
    void shouldExposeDatasetHealthContract() throws Exception {
        RelevanceDatasetHealthResponse response = new RelevanceDatasetHealthResponse(
                "2026.02.14",
                10,
                Instant.parse("2026-02-01T00:00:00Z"),
                14,
                13,
                false,
                Instant.parse("2026-02-14T10:00:00Z"));
        when(searchUseCases.evaluateRelevanceDatasetHealth()).thenReturn(response);

        mockMvc.perform(get("/api/search/admin/relevance/dataset/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetVersion").value("2026.02.14"))
                .andExpect(jsonPath("$.datasetSize").value(10))
                .andExpect(jsonPath("$.refreshCadenceDays").value(14))
                .andExpect(jsonPath("$.refreshRequired").value(false));
    }
}
