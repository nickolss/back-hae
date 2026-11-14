package br.com.cps.hae.application.institution;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.ConflictException;
import br.com.cps.hae.domain.shared.NotFoundException;
import br.com.cps.hae.domain.shared.Semester;
import jakarta.transaction.Transactional;

@UseCase
@Transactional
public class InstitutionService {

    private final InstitutionRepository institutions;
    private final HaeRepository haes;
    private final Clock clock;

    public InstitutionService(InstitutionRepository institutions, HaeRepository haes, Clock clock) {
        this.institutions = institutions;
        this.haes = haes;
        this.clock = clock;
    }

    public record PublicInstitution(int code, String name) {
    }

    public record InstitutionCommand(int code, String name, String address, int weeklyHoursLimit) {
    }

    /** Lista pública (tela de cadastro): apenas código e nome das unidades ativas. */
    public List<PublicInstitution> listPublic() {
        return institutions.findAll().stream()
                .filter(Institution::active)
                .sorted(Comparator.comparing(Institution::name))
                .map(i -> new PublicInstitution(i.code(), i.name()))
                .toList();
    }

    public List<InstitutionView> list(Actor actor) {
        if (actor.isGlobal()) {
            return institutions.findAll().stream()
                    .sorted(Comparator.comparing(Institution::name))
                    .map(InstitutionView::of)
                    .toList();
        }
        return institutions.findById(actor.institutionId()).map(InstitutionView::of).stream().toList();
    }

    public InstitutionView get(Actor actor, UUID id) {
        actor.requireInstitutionAccess(id);
        return InstitutionView.of(load(id));
    }

    public InstitutionView create(Actor actor, InstitutionCommand command) {
        actor.requireGlobal();
        if (institutions.existsByCode(command.code())) {
            throw new ConflictException("Já existe uma instituição com o código " + command.code() + ".");
        }
        if (institutions.existsByNameIgnoreCase(command.name())) {
            throw new ConflictException("Já existe uma instituição com esse nome.");
        }
        Institution created = Institution.create(command.code(), command.name(), command.address(),
                command.weeklyHoursLimit(), clock.instant());
        return InstitutionView.of(institutions.save(created));
    }

    public InstitutionView update(Actor actor, UUID id, InstitutionCommand command) {
        actor.requireGlobal();
        Institution institution = load(id);
        institutions.findByCode(command.code())
                .filter(other -> !other.id().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("Já existe uma instituição com o código " + command.code() + ".");
                });
        if (!institution.name().equalsIgnoreCase(command.name().strip())
                && institutions.existsByNameIgnoreCase(command.name())) {
            throw new ConflictException("Já existe uma instituição com esse nome.");
        }
        institution.update(command.code(), command.name(), command.address(), command.weeklyHoursLimit());
        return InstitutionView.of(institutions.save(institution));
    }

    public InstitutionView setActive(Actor actor, UUID id, boolean active) {
        actor.requireGlobal();
        Institution institution = load(id);
        if (active) {
            institution.activate();
        } else {
            institution.deactivate();
        }
        return InstitutionView.of(institutions.save(institution));
    }

    public InstitutionView changeHoursLimit(Actor actor, UUID id, int weeklyHoursLimit) {
        actor.requireInstitutionManager(id);
        Institution institution = load(id);
        institution.changeWeeklyHoursLimit(weeklyHoursLimit);
        return InstitutionView.of(institutions.save(institution));
    }

    public HoursSummary hoursSummary(Actor actor, UUID id, Semester semester) {
        actor.requireInstitutionAccess(id);
        Institution institution = load(id);
        Semester target = semester != null ? semester : Semester.of(LocalDate.now(clock));
        int allocated = haes.sumWeeklyHours(id, target, HaeStatus.consumingHours(), null);
        return new HoursSummary(id, target.toString(), institution.weeklyHoursLimit(), allocated,
                Math.max(0, institution.weeklyHoursLimit() - allocated));
    }

    private Institution load(UUID id) {
        return institutions.findById(id).orElseThrow(() -> NotFoundException.of("Instituição"));
    }
}
