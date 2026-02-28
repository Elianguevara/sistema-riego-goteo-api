package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Task;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.TaskStatus;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.JwtService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.ExcelReportService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.PdfReportService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.ReportTaskService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de capa web para los endpoints de descarga directa de ReportController:
 * GET /api/reports/tasks/pdf
 * GET /api/reports/tasks/excel
 *
 * Los servicios de generación (PdfReportService, ExcelReportService,
 * TaskService)
 * son mocked para aislar la lógica HTTP del controller.
 */
@WebMvcTest(ReportController.class)
@ActiveProfiles("test")
@DisplayName("ReportController - Tests de Capa Web (descarga directa)")
class ReportControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PdfReportService pdfReportService;

        @MockitoBean
        private ExcelReportService excelReportService;

        @MockitoBean
        private ReportTaskService reportTaskService;

        @MockitoBean
        private TaskService taskService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserDetailsService userDetailsService;

        private List<Task> sampleTasks;
        private byte[] fakePdfBytes;
        private byte[] fakeXlsxBytes;

        @BeforeEach
        void setUp() {
                Role rolAnalista = new Role("ANALISTA");
                Role rolOperario = new Role("OPERARIO");

                User creator = new User("Ana López", "analista", "pass", "ana@test.com", rolAnalista);
                User assignee = new User("Carlos Op", "operario", "pass", "op@test.com", rolOperario);

                Farm farm = new Farm();
                farm.setId(1);
                farm.setName("Finca Norte");

                Sector sector = new Sector();
                sector.setId(1);
                sector.setName("Lote 1");
                sector.setFarm(farm);

                Task task = new Task();
                task.setDescription("Tarea de prueba para reporte");
                task.setStatus(TaskStatus.PENDIENTE);
                task.setCreatedBy(creator);
                task.setAssignedTo(assignee);
                task.setSector(sector);

                sampleTasks = List.of(task);

                // Firma %PDF para simular un PDF mínimo válido en las cabeceras
                fakePdfBytes = ("%PDF-1.4 fake content for test").getBytes();

                // Cabecera PK (ZIP) para simular un xlsx mínimo
                fakeXlsxBytes = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x00, 0x00 };
        }

        // ════════════════════════════════════════════════════════════════════════
        // GET /api/reports/tasks/pdf
        // ════════════════════════════════════════════════════════════════════════

        @Test
        @WithMockUser(username = "analista", roles = { "ANALISTA" })
        @DisplayName("GET /tasks/pdf con ANALISTA debe retornar 200, Content-Type PDF y cabecera de descarga")
        void downloadTasksPdf_analista_retorna200ConPdf() throws Exception {
                when(taskService.getAllTasks()).thenReturn(sampleTasks);
                when(pdfReportService.generateCorporateReport(anyList(), anyString(), anyString()))
                                .thenReturn(fakePdfBytes);

                mockMvc.perform(get("/api/reports/tasks/pdf"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                                .andExpect(header().string("Content-Disposition",
                                                containsString("tasks-report-")))
                                .andExpect(header().string("Content-Disposition",
                                                containsString(".pdf")));
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        @DisplayName("GET /tasks/pdf con ADMIN debe retornar 200")
        void downloadTasksPdf_admin_retorna200() throws Exception {
                when(taskService.getAllTasks()).thenReturn(sampleTasks);
                when(pdfReportService.generateCorporateReport(anyList(), anyString(), anyString()))
                                .thenReturn(fakePdfBytes);

                mockMvc.perform(get("/api/reports/tasks/pdf"))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /tasks/pdf sin autenticación debe retornar 401 Unauthorized")
        void downloadTasksPdf_sinAutenticacion_retorna401() throws Exception {
                mockMvc.perform(get("/api/reports/tasks/pdf"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "operario", roles = { "OPERARIO" })
        @DisplayName("GET /tasks/pdf con OPERARIO debe retornar 403 Forbidden")
        void downloadTasksPdf_operario_retorna403() throws Exception {
                when(taskService.getAllTasks())
                                .thenThrow(new AccessDeniedException("Acceso denegado"));

                mockMvc.perform(get("/api/reports/tasks/pdf"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "analista", roles = { "ANALISTA" })
        @DisplayName("GET /tasks/pdf cuando taskService lanza excepción debe retornar 500")
        void downloadTasksPdf_errorEnServicio_retorna500() throws Exception {
                when(taskService.getAllTasks())
                                .thenThrow(new RuntimeException("Error simulado de base de datos"));

                mockMvc.perform(get("/api/reports/tasks/pdf"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(username = "analista", roles = { "ANALISTA" })
        @DisplayName("GET /tasks/pdf con lista vacía de tareas debe retornar 200 igualmente")
        void downloadTasksPdf_sinTareas_retorna200() throws Exception {
                when(taskService.getAllTasks()).thenReturn(List.of());
                when(pdfReportService.generateCorporateReport(anyList(), anyString(), anyString()))
                                .thenReturn(fakePdfBytes);

                mockMvc.perform(get("/api/reports/tasks/pdf"))
                                .andExpect(status().isOk());
        }

        // ════════════════════════════════════════════════════════════════════════
        // GET /api/reports/tasks/excel
        // ════════════════════════════════════════════════════════════════════════

        @Test
        @WithMockUser(username = "analista", roles = { "ANALISTA" })
        @DisplayName("GET /tasks/excel con ANALISTA debe retornar 200, Content-Type xlsx y cabecera de descarga")
        void downloadTasksExcel_analista_retorna200ConXlsx() throws Exception {
                when(taskService.getAllTasks()).thenReturn(sampleTasks);
                when(excelReportService.generateReport(anyList(), anyString()))
                                .thenReturn(fakeXlsxBytes);

                mockMvc.perform(get("/api/reports/tasks/excel"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .andExpect(header().string("Content-Disposition",
                                                containsString("tasks-report-")))
                                .andExpect(header().string("Content-Disposition",
                                                containsString(".xlsx")));
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        @DisplayName("GET /tasks/excel con ADMIN debe retornar 200")
        void downloadTasksExcel_admin_retorna200() throws Exception {
                when(taskService.getAllTasks()).thenReturn(sampleTasks);
                when(excelReportService.generateReport(anyList(), anyString()))
                                .thenReturn(fakeXlsxBytes);

                mockMvc.perform(get("/api/reports/tasks/excel"))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /tasks/excel sin autenticación debe retornar 401 Unauthorized")
        void downloadTasksExcel_sinAutenticacion_retorna401() throws Exception {
                mockMvc.perform(get("/api/reports/tasks/excel"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "operario", roles = { "OPERARIO" })
        @DisplayName("GET /tasks/excel con OPERARIO debe retornar 403 Forbidden")
        void downloadTasksExcel_operario_retorna403() throws Exception {
                when(taskService.getAllTasks())
                                .thenThrow(new AccessDeniedException("Acceso denegado"));

                mockMvc.perform(get("/api/reports/tasks/excel"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "analista", roles = { "ANALISTA" })
        @DisplayName("GET /tasks/excel cuando excelReportService lanza excepción debe retornar 500")
        void downloadTasksExcel_errorEnServicio_retorna500() throws Exception {
                when(taskService.getAllTasks())
                                .thenThrow(new RuntimeException("Error simulado de generación Excel"));

                mockMvc.perform(get("/api/reports/tasks/excel"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(username = "analista", roles = { "ANALISTA" })
        @DisplayName("GET /tasks/excel con lista vacía de tareas debe retornar 200 igualmente")
        void downloadTasksExcel_sinTareas_retorna200() throws Exception {
                when(taskService.getAllTasks()).thenReturn(List.of());
                when(excelReportService.generateReport(anyList(), anyString()))
                                .thenReturn(fakeXlsxBytes);

                mockMvc.perform(get("/api/reports/tasks/excel"))
                                .andExpect(status().isOk());
        }
}
