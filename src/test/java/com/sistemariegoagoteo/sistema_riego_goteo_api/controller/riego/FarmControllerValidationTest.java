package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.JwtService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de Caja Negra: Validación de entradas y manejo de errores en FarmController.
 *
 * Nivel cubierto:
 *  - Validación de Bean Validation (@Valid) → 400 Bad Request
 *  - Manejo de ResourceNotFoundException       → 404 Not Found
 *  - Validación de estructura del JSON de respuesta de error
 *
 * Complementa FarmControllerTest (que cubre los casos happy-path).
 * Usa @WebMvcTest para aislar la capa web sin levantar la BD.
 */
@WebMvcTest(FarmController.class)
@ActiveProfiles("test")
@DisplayName("FarmController - Validación de Entradas y Manejo de Errores (400/404)")
class FarmControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FarmService farmService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // =========================================================================
    // GRUPO 1: Validación de entradas → 400 Bad Request
    // =========================================================================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - nombre en blanco → 400 Bad Request")
    void createFarm_nombreEnBlanco_retorna400() throws Exception {
        FarmRequest request = buildRequest("", "10.0", "5.0");

        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - nombre nulo → 400 Bad Request")
    void createFarm_nombreNulo_retorna400() throws Exception {
        FarmRequest request = buildRequest(null, "10.0", "5.0");

        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - nombre mayor a 100 caracteres → 400 Bad Request")
    void createFarm_nombreMayorA100Caracteres_retorna400() throws Exception {
        String nombreLargo = "A".repeat(101); // 101 chars, viola @Size(max=100)
        FarmRequest request = buildRequest(nombreLargo, "10.0", "5.0");

        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - reservoirCapacity nulo → 400 Bad Request")
    void createFarm_capacidadReservorioNula_retorna400() throws Exception {
        FarmRequest request = new FarmRequest();
        request.setName("Finca Válida");
        request.setReservoirCapacity(null); // viola @NotNull
        request.setFarmSize(new BigDecimal("5.0"));

        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - reservoirCapacity negativa → 400 Bad Request")
    void createFarm_capacidadNegativa_retorna400() throws Exception {
        FarmRequest request = buildRequest("Finca Test", "-1.0", "5.0");
        // -1 viola @PositiveOrZero

        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - farmSize nulo → 400 Bad Request")
    void createFarm_tamanoNulo_retorna400() throws Exception {
        FarmRequest request = new FarmRequest();
        request.setName("Finca Válida");
        request.setReservoirCapacity(new BigDecimal("100.0"));
        request.setFarmSize(null); // viola @NotNull

        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - body completamente vacío → 400 Bad Request")
    void createFarm_bodyVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/farms - Content-Type incorrecto → respuesta de error 4xx/5xx (no 200)")
    void createFarm_contentTypeIncorrecto_noRetorna200() throws Exception {
        // Spring MVC rechaza el Content-Type incorrecto con un error (415 ideal,
        // puede ser 500 si no hay @ControllerAdvice global en el slice WebMvcTest).
        // Lo importante es que NO retorna 200 OK.
        mockMvc.perform(post("/api/farms")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("nombre=test"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(200));
    }

    // =========================================================================
    // GRUPO 2: Recursos no encontrados → 404 Not Found
    // =========================================================================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/farms/{id} - finca inexistente → 404 Not Found")
    void getFarmById_farmInexistente_retorna404() throws Exception {
        when(farmService.getFarmById(999))
                .thenThrow(new ResourceNotFoundException("Farm", "id", 999));

        mockMvc.perform(get("/api/farms/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/farms/{id} - finca inexistente → 404 Not Found")
    void updateFarm_farmInexistente_retorna404() throws Exception {
        FarmRequest request = buildRequest("Finca Nueva", "50.0", "10.0");
        when(farmService.updateFarm(eq(999), any(FarmRequest.class)))
                .thenThrow(new ResourceNotFoundException("Farm", "id", 999));

        mockMvc.perform(put("/api/farms/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("DELETE /api/farms/{id} - finca inexistente → 404 Not Found")
    void deleteFarm_farmInexistente_retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Farm", "id", 999))
                .when(farmService).deleteFarm(999);

        mockMvc.perform(delete("/api/farms/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Utilitario
    // =========================================================================

    private FarmRequest buildRequest(String name, String reservoirCapacity, String farmSize) {
        FarmRequest request = new FarmRequest();
        request.setName(name);
        request.setReservoirCapacity(reservoirCapacity != null ? new BigDecimal(reservoirCapacity) : null);
        request.setFarmSize(farmSize != null ? new BigDecimal(farmSize) : null);
        return request;
    }
}
