package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.JwtService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests de capa web para FarmController.
 * Utiliza @WithMockUser de spring-security-test para simular usuarios
 * autenticados
 * sin necesitar un JWT real.
 */
@WebMvcTest(FarmController.class)
@ActiveProfiles("test")
@DisplayName("FarmController - Tests de Capa Web")
class FarmControllerTest {

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

    private Farm testFarm;
    private FarmRequest farmRequest;

    @BeforeEach
    void setUp() {
        testFarm = new Farm();
        testFarm.setId(1);
        testFarm.setName("Finca Test");
        testFarm.setLocation("La Serena");
        testFarm.setReservoirCapacity(new BigDecimal("1000"));
        testFarm.setFarmSize(new BigDecimal("50"));

        farmRequest = new FarmRequest();
        farmRequest.setName("Finca Test");
        farmRequest.setLocation("La Serena");
        farmRequest.setReservoirCapacity(new BigDecimal("1000"));
        farmRequest.setFarmSize(new BigDecimal("50"));
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    @DisplayName("GET /api/farms debe retornar 200 OK con lista de fincas para usuario ADMIN autenticado")
    void getAllFarms_usuarioAdmin_retorna200ConListaFincas() throws Exception {
        when(farmService.getAllFarms()).thenReturn(List.of(testFarm));

        mockMvc.perform(get("/api/farms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Finca Test"))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/farms sin autenticacion debe retornar 401 Unauthorized")
    void getAllFarms_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/farms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    @DisplayName("POST /api/farms debe retornar 201 Created al crear nueva finca")
    void createFarm_administradorAutenticado_retorna201() throws Exception {
        when(farmService.createFarm(any(FarmRequest.class))).thenReturn(testFarm);

        mockMvc.perform(post("/api/farms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(farmRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Finca Test"));
    }

    @Test
    @WithMockUser(username = "operario", roles = { "OPERARIO" })
    @DisplayName("POST /api/farms debe retornar 403 para usuario OPERARIO")
    void createFarm_usuarioOperario_retorna403() throws Exception {
        when(farmService.createFarm(any(FarmRequest.class)))
                .thenThrow(new AccessDeniedException("Acceso denegado"));

        mockMvc.perform(post("/api/farms")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(farmRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    @DisplayName("GET /api/farms/{id} debe retornar 200 OK con la finca correcta")
    void getFarmById_farmExistente_retorna200() throws Exception {
        when(farmService.getFarmById(1)).thenReturn(testFarm);

        mockMvc.perform(get("/api/farms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Finca Test"));
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    @DisplayName("DELETE /api/farms/{id} debe retornar 204 No Content al eliminar la finca")
    void deleteFarm_administradorAutenticado_retorna204() throws Exception {
        mockMvc.perform(delete("/api/farms/1")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
