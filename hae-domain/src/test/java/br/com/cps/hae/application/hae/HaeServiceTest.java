package br.com.cps.hae.application.hae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.cps.hae.application.hae.HaeService.CreateHaeCommand;
import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.FileStorage;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.port.Notifier;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.support.InMemoryHaeRepository;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.hae.Dimension;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeDetails;
import br.com.cps.hae.domain.hae.HaePeriod;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.hae.Modality;
import br.com.cps.hae.domain.hae.ReviewDecision;
import br.com.cps.hae.domain.hae.TimeRange;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.AccessDeniedException;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.NotFoundException;

class HaeServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    private final InMemoryHaeRepository haes = new InMemoryHaeRepository();
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final InstitutionRepository institutions = mock(InstitutionRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final Notifier notifier = mock(Notifier.class);
    private HaeService service;

    private Institution fatec;
    private Course course;
    private Employee professor;
    private Employee coordinator;
    private Employee otherInstitutionCoordinator;

    @BeforeEach
    void setUp() {
        fatec = Institution.create(110, "Fatec Zona Leste", "Av. Águia de Haia", 10, NOW);
        course = Course.create(fatec.id(), "ADS");
        professor = employee("prof@fatec.sp.gov.br", Role.PROFESSOR, fatec.id());
        coordinator = employee("coord@fatec.sp.gov.br", Role.COORDENADOR, fatec.id());
        otherInstitutionCoordinator = employee("coord2@fatec.sp.gov.br", Role.COORDENADOR, UUID.randomUUID());

        when(institutions.findById(fatec.id())).thenReturn(Optional.of(fatec));
        when(courses.findById(course.id())).thenReturn(Optional.of(course));
        for (Employee e : List.of(professor, coordinator, otherInstitutionCoordinator)) {
            when(employees.findById(e.id())).thenReturn(Optional.of(e));
        }
        when(employees.findByRoleAndCourse(Role.COORDENADOR, course.id())).thenReturn(List.of(coordinator));

        var views = new HaeViewAssembler(employees, courses, institutions);
        service = new HaeService(haes, employees, institutions, courses, mock(FileStorage.class), notifier, views,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void professorCriaHaeENotificaCoordenadorDoCurso() {
        HaeView view = service.create(Actor.of(professor), new CreateHaeCommand(null, course.id(), details(4)));

        assertThat(view.status()).isEqualTo(HaeStatus.PENDENTE);
        assertThat(view.employee().id()).isEqualTo(professor.id());
        assertThat(view.permissions().canEdit()).isTrue();
        assertThat(view.permissions().canReview()).isFalse();
        verify(notifier).notifyHaeSubmitted(eq(coordinator.email()), any());
    }

    @Test
    void respeitaLimiteDeHorasDaInstituicao() {
        service.create(Actor.of(professor), new CreateHaeCommand(null, course.id(), details(8)));
        assertThatThrownBy(() -> service.create(Actor.of(professor),
                new CreateHaeCommand(null, course.id(), details(3))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("restam 2h");
    }

    @Test
    void haeReprovadaNaoConsomeHoras() {
        HaeView first = service.create(Actor.of(professor), new CreateHaeCommand(null, course.id(), details(8)));
        service.review(Actor.of(coordinator), first.id(), ReviewDecision.REJECT, "Revise o cronograma");
        HaeView second = service.create(Actor.of(professor), new CreateHaeCommand(null, course.id(), details(8)));
        assertThat(second.status()).isEqualTo(HaeStatus.PENDENTE);
    }

    @Test
    void bloqueiaNovaHaeComPendenciaDeSemestreAnterior() {
        Hae old = Hae.create(professor.id(), fatec.id(), course.id(), details(2, LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 6, 1)), NOW);
        haes.save(old);
        assertThatThrownBy(() -> service.create(Actor.of(professor),
                new CreateHaeCommand(null, course.id(), details(2))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("semestres anteriores");
    }

    @Test
    void coordenadorDeOutraInstituicaoNaoEnxergaHae() {
        HaeView view = service.create(Actor.of(professor), new CreateHaeCommand(null, course.id(), details(2)));
        assertThatThrownBy(() -> service.review(Actor.of(otherInstitutionCoordinator), view.id(),
                ReviewDecision.APPROVE, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void professorNaoPodeAvaliar() {
        Employee colleague = employee("colega@fatec.sp.gov.br", Role.PROFESSOR, fatec.id());
        when(employees.findById(colleague.id())).thenReturn(Optional.of(colleague));
        HaeView view = service.create(Actor.of(professor), new CreateHaeCommand(null, course.id(), details(2)));
        assertThatThrownBy(() -> service.review(Actor.of(colleague), view.id(), ReviewDecision.APPROVE, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void professorNaoCriaHaeParaOutraPessoa() {
        assertThatThrownBy(() -> service.create(Actor.of(professor),
                new CreateHaeCommand(coordinator.id(), course.id(), details(2))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void coordenadorAprovaENotificaProfessor() {
        HaeView view = service.create(Actor.of(professor), new CreateHaeCommand(null, course.id(), details(2)));
        HaeView approved = service.review(Actor.of(coordinator), view.id(), ReviewDecision.APPROVE, "Ok");
        assertThat(approved.status()).isEqualTo(HaeStatus.APROVADO);
        assertThat(approved.reviewer().id()).isEqualTo(coordinator.id());
        verify(notifier).notifyHaeReviewed(eq(professor.email()), any(), eq(HaeStatus.APROVADO), eq("Ok"));
    }

    private static Employee employee(String email, Role role, UUID institutionId) {
        return Employee.create(email.substring(0, email.indexOf('@')), new EmailAddress(email), "hash", role,
                institutionId, null, NOW);
    }

    private static HaeDetails details(int hours) {
        return details(hours, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 4));
    }

    private static HaeDetails details(int hours, LocalDate start, LocalDate end) {
        return new HaeDetails("Projeto", "Descrição", null, HaeType.APOIO_DIRECAO, Modality.PRESENCIAL,
                Dimension.DIMENSAO_9_GESTAO_DA_ROTINA, hours, new HaePeriod(start, end),
                Map.of(DayOfWeek.TUESDAY, new TimeRange(LocalTime.of(14, 0), LocalTime.of(16, 0))), List.of());
    }
}
