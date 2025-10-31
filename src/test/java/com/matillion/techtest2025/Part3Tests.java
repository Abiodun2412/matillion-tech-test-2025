package com.matillion.techtest2025;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matillion.techtest2025.controller.response.ColumnProfileResponse;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Part3Tests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DataAnalysisRepository dataAnalysisRepository;

    @BeforeEach
    void init() {
        dataAnalysisRepository.deleteAll();
    }

    @Test
    void shouldInferTypesAndComputeStats(
            @Value("classpath:test-data/part3-types.csv") Resource csv
    ) throws Exception {
        // ingest CSV
        String data = csv.getContentAsString(UTF_8);
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(data))
                .andExpect(status().isOk());

        // get the saved analysis ID
        var entity = dataAnalysisRepository.findAll().getFirst();
        Long id = entity.getId();

        // call profile endpoint
        var mvcResp = mockMvc.perform(get("/api/analysis/{id}/profile", id))
                .andExpect(status().isOk())
                .andReturn();

        ColumnProfileResponse resp = objectMapper.readValue(
                mvcResp.getResponse().getContentAsString(),
                ColumnProfileResponse.class
        );

        assertThat(resp.analysisId()).isEqualTo(id);
        assertThat(resp.columns()).hasSize(4);

        // type assertions
        assertThat(resp.columns())
                .anyMatch(c -> c.columnName().equals("name") && c.dataType().name().equals("STRING"))
                .anyMatch(c -> c.columnName().equals("age") && c.dataType().name().equals("INTEGER"))
                .anyMatch(c -> c.columnName().equals("price") && c.dataType().name().equals("DECIMAL"))
                .anyMatch(c -> c.columnName().equals("active") && c.dataType().name().equals("BOOLEAN"));

        // basic numeric stats sanity checks (age & price have 3 numeric values each)
        var age = resp.columns().stream().filter(c -> c.columnName().equals("age")).findFirst().orElseThrow();
        assertThat(age.numericCount()).isEqualTo(3);
        assertThat(age.min()).isEqualTo(26.0);
        assertThat(age.max()).isEqualTo(39.0);
        assertThat(age.median()).isEqualTo(27.0);

        var price = resp.columns().stream().filter(c -> c.columnName().equals("price")).findFirst().orElseThrow();
        assertThat(price.numericCount()).isEqualTo(3);
        assertThat(price.min()).isEqualTo(7.0);
        assertThat(price.max()).isEqualTo(31.0);
        assertThat(price.median()).isEqualTo(12.5);
    }

    @Test
    void shouldReturn404ForMissingProfile() throws Exception {
        mockMvc.perform(get("/api/analysis/{id}/profile", 999999L))
                .andExpect(status().isNotFound());
    }
}
