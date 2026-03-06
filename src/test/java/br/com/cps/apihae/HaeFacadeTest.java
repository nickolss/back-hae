package br.com.cps.apihae;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.cps.apihae.adapter.controller.HaeController;
import br.com.cps.apihae.adapter.dto.request.HaeRequest;
import br.com.cps.apihae.adapter.dto.request.HaeStatusUpdateRequest;
import br.com.cps.apihae.adapter.dto.request.WeeklyScheduleEntry;
import br.com.cps.apihae.adapter.dto.response.HaeDetailDTO;
import br.com.cps.apihae.adapter.dto.response.HaeHoursResponseDTO;
import br.com.cps.apihae.adapter.dto.response.HaeResponseDTO;
import br.com.cps.apihae.adapter.facade.HaeFacade;
import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.entity.Hae;
import br.com.cps.apihae.domain.entity.Institution;
import br.com.cps.apihae.domain.enums.DimensaoHae;
import br.com.cps.apihae.domain.enums.HaeType;
import br.com.cps.apihae.domain.enums.Modality;
import br.com.cps.apihae.domain.enums.Status;
import br.com.cps.apihae.useCase.Interface.IEmployeeRepository;
import br.com.cps.apihae.useCase.util.JWTUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HaeController.class)
public class HaeFacadeTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private HaeFacade haeFacade;

        @MockBean
        private JWTUtils jwtUtils;

        @MockBean
        private IEmployeeRepository employeeRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private HaeRequest sampleRequest;
        private Hae sampleHae;
        private HaeDetailDTO sampleDetail;
        private HaeResponseDTO sampleResponse;
        private HaeHoursResponseDTO sampleHours;
        private static final String AUTH_TOKEN = "valid-token";
        private static final String LOGGED_USER_ID = "user-1";
        private static final String INSTITUTION_ID = "inst1";

        @BeforeEach
        void setup() {
                sampleRequest = new HaeRequest();
                sampleRequest.setEmployeeId("emp1");
                sampleRequest.setProjectTitle("Projeto de Teste");
                sampleRequest.setWeeklyHours(10);
                sampleRequest.setCourse("ADS");
                sampleRequest.setProjectType(HaeType.ApoioDirecao);
                sampleRequest.setModality(Modality.PRESENCIAL); 
                sampleRequest.setStartDate(LocalDate.now().plusDays(1));
                sampleRequest.setEndDate(LocalDate.now().plusDays(10));
                sampleRequest.setDimensao(DimensaoHae.DIMENSAO_1_DIDATICO_PEDAGOGICO); 
                sampleRequest.setObservations("Observações de teste");
                sampleRequest.setDayOfWeek(List.of("Monday"));
                sampleRequest.setTimeRange("08:00-12:00");
                sampleRequest.setProjectDescription("Descrição do projeto");
                sampleRequest.setInstitutionCode(123);
                sampleRequest.setStudentRAs(List.of("RA12345"));
                Map<String, WeeklyScheduleEntry> schedule = Map.of(
                                "Monday", new WeeklyScheduleEntry(),
                                "Tuesday", new WeeklyScheduleEntry());
                sampleRequest.setWeeklySchedule(schedule);

                sampleHae = new Hae();
                sampleHae.setId("1");
                sampleHae.setCourse("ADS");
                sampleHae.setProjectTitle("Projeto de Teste");
                sampleHae.setStatus(Status.PENDENTE);
                sampleHae.setProjectType(HaeType.ApoioDirecao);
                sampleHae.setWeeklyHours(10);
                sampleHae.setDayOfWeek(List.of("Monday"));
                sampleHae.setStartDate(LocalDate.now().plusDays(1));
                sampleHae.setEndDate(LocalDate.now().plusDays(10));
                sampleHae.setDimensao(DimensaoHae.DIMENSAO_1_DIDATICO_PEDAGOGICO);
                sampleHae.setProjectDescription("Descrição do projeto");
                sampleHae.setObservations("Observações de teste");
                sampleHae.setViewed(false);
                sampleHae.setModality(Modality.PRESENCIAL);

                Institution institution = new Institution();
                institution.setId(INSTITUTION_ID);
                sampleHae.setInstitution(institution);

                Employee authenticatedEmployee = new Employee();
                authenticatedEmployee.setId(LOGGED_USER_ID);
                authenticatedEmployee.setInstitution(institution);

                Mockito.when(jwtUtils.validateToken(AUTH_TOKEN)).thenReturn(LOGGED_USER_ID);
                Mockito.when(employeeRepository.findById(Mockito.anyString()))
                                .thenReturn(Optional.of(authenticatedEmployee));

                sampleDetail = new HaeDetailDTO();
                sampleDetail.setCourse("ADS");
                sampleDetail.setProjectTitle("Projeto de Teste");
                sampleDetail.setStatus(Status.PENDENTE);

                sampleResponse = new HaeResponseDTO();
                sampleResponse.setCourse("ADS");
                sampleResponse.setProjectTitle("Projeto de Teste");
                sampleResponse.setStatus(Status.PENDENTE);

                sampleHours = new HaeHoursResponseDTO("1", 10);
        }

        @Test
        void testCreateHae() throws Exception {
                Hae createdHae = new Hae();
                createdHae.setId("1");
                createdHae.setCourse("ADS");
                createdHae.setProjectTitle("Projeto de Teste");
                createdHae.setProjectType(HaeType.ApoioDirecao);
                createdHae.setWeeklyHours(10);
                createdHae.setStatus(Status.PENDENTE);
                createdHae.setDayOfWeek(List.of("Monday"));
                createdHae.setStartDate(LocalDate.now().plusDays(1));
                createdHae.setEndDate(LocalDate.now().plusDays(10));
                createdHae.setDimensao(DimensaoHae.DIMENSAO_1_DIDATICO_PEDAGOGICO);
                createdHae.setProjectDescription("Descrição do projeto");
                createdHae.setObservations("Observações de teste");
                createdHae.setViewed(false);
                createdHae.setModality(Modality.PRESENCIAL);

                Mockito.when(haeFacade.createHae(Mockito.any()))
                                .thenReturn(createdHae);

                mockMvc.perform(post("/hae/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.course").value("ADS"))
                                .andExpect(jsonPath("$.projectTitle").value("Projeto de Teste"));
        }

        @Test
        void testGetHaeById() throws Exception {
                Mockito.when(haeFacade.getHaeById("1", INSTITUTION_ID)).thenReturn(sampleDetail);

                mockMvc.perform(get("/hae/getHaeById/1").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.course").value("ADS"));
        }

        @Test
        void testDeleteHae() throws Exception {
                Mockito.doNothing().when(haeFacade).deleteHae("1");

                mockMvc.perform(delete("/hae/delete/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mensagem").value("HAE deletado com sucesso."));
        }

        @Test
        void testGetHaesByEmployeeId() throws Exception {
                Mockito.when(haeFacade.getHaesByEmployeeId("emp1"))
                                .thenReturn(Collections.singletonList(sampleHae));

                mockMvc.perform(get("/hae/employee/emp1").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetHaesByEmployeeIdWithLimit() throws Exception {
                Mockito.when(haeFacade.getHaesByEmployeeIdWithLimit("emp1"))
                                .thenReturn(Collections.singletonList(sampleHae));

                mockMvc.perform(
                                get("/hae/getHaesByEmployeeIdWithLimit/emp1").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetAllHaes() throws Exception {
                Mockito.when(haeFacade.getHaesByInstitutionId(INSTITUTION_ID))
                                .thenReturn(Collections.singletonList(sampleResponse));

                mockMvc.perform(get("/hae/getAll").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testChangeHaeStatus() throws Exception {
                HaeStatusUpdateRequest statusRequest = new HaeStatusUpdateRequest();
                statusRequest.setNewStatus(Status.APROVADO);
                statusRequest.setCoordenadorId("coord1");

                Mockito.when(haeFacade.updateHaeStatus("1", Status.APROVADO, "coord1"))
                                .thenReturn(sampleHae);

                mockMvc.perform(put("/hae/change-status/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(statusRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.course").value("ADS"));
        }

        @Test
        void testGetHaesByProfessor() throws Exception {
                Mockito.when(haeFacade.getHaesByProfessorId("prof1"))
                                .thenReturn(Collections.singletonList(sampleResponse));

                mockMvc.perform(get("/hae/getHaesByProfessor/prof1").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetHaesByCourse() throws Exception {
                Mockito.when(haeFacade.advancedHaeSearch(INSTITUTION_ID, "ADS", null, null, null))
                                .thenReturn(Collections.singletonList(sampleResponse));

                mockMvc.perform(get("/hae/getHaesByCourse/ADS").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetHaesByType() throws Exception {

                Mockito.when(haeFacade.getHaesByType(HaeType.ApoioDirecao))
                                .thenReturn(Collections.singletonList(sampleHae));

                mockMvc.perform(get("/hae/getHaesByType/ApoioDirecao").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testUpdateHae() throws Exception {
                Mockito.when(haeFacade.updateHae("1", sampleRequest)).thenReturn(sampleHae);

                mockMvc.perform(put("/hae/update/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.course").value("ADS"));
        }

        @Test
        void testCreateHaeAsCoordinator() throws Exception {
                Mockito.when(haeFacade.getHaesByEmployeeId("emp1"))
                                .thenReturn(Collections.singletonList(sampleHae));
                mockMvc.perform(post("/hae/createHaeAsCoordinator/coord1/emp1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest)))
                                .andExpect(status().isCreated());
        }

        @Test
        void testToggleViewed() throws Exception {
                Mockito.when(haeFacade.toggleViewedStatus("1")).thenReturn(sampleHae);

                mockMvc.perform(put("/hae/viewed/toggle/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.course").value("ADS"));
        }

        @Test
        void testGetViewed() throws Exception {
                Mockito.when(haeFacade.getViewed()).thenReturn(Collections.singletonList(sampleHae));

                mockMvc.perform(get("/hae/viewed").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetNotViewed() throws Exception {
                Mockito.when(haeFacade.getNotViewed()).thenReturn(Collections.singletonList(sampleHae));

                mockMvc.perform(get("/hae/not-viewed").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetHaesByInstitution() throws Exception {
                Mockito.when(haeFacade.getHaesByInstitutionId("inst1"))
                                .thenReturn(Collections.singletonList(sampleResponse));

                mockMvc.perform(get("/hae/institution/inst1").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetHaesByStatus() throws Exception {
                Mockito.when(haeFacade.advancedHaeSearch(INSTITUTION_ID, null, null, Status.APROVADO, null))
                                .thenReturn(Collections.singletonList(sampleResponse));

                mockMvc.perform(get("/hae/getHaeByStatus/APROVADO").cookie(new Cookie("auth_token", AUTH_TOKEN)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testSearchHaes() throws Exception {
                Mockito.when(haeFacade.advancedHaeSearch(INSTITUTION_ID, "ADS", HaeType.ApoioDirecao, Status.APROVADO,
                                true))
                                .thenReturn(Collections.singletonList(sampleResponse));

                mockMvc.perform(get("/hae/search")
                                .cookie(new Cookie("auth_token", AUTH_TOKEN))
                                .param("course", "ADS")
                                .param("haeType", "ApoioDirecao")
                                .param("status", "APROVADO")
                                .param("viewed", "true"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].course").value("ADS"));
        }

        @Test
        void testGetWeeklyHoursByHae() throws Exception {
                Mockito.when(haeFacade.getWeeklyHoursByHae("1"))
                                .thenReturn(sampleHours);

                mockMvc.perform(get("/hae/getWeeklyHoursByHae/1"))
                                .andExpect(status().isOk());
        }
}
