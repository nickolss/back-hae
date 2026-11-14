package br.com.cps.hae.application.hae;

import java.util.UUID;

import br.com.cps.hae.application.port.HaeQueries;
import br.com.cps.hae.application.port.HaeQueries.HaeSummary;
import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.PageQuery;
import br.com.cps.hae.application.shared.PageResult;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.shared.NotFoundException;
import jakarta.transaction.Transactional;

@UseCase
@Transactional
public class HaeQueryService {

    private final HaeQueries queries;
    private final HaeRepository haes;
    private final HaeViewAssembler views;

    public HaeQueryService(HaeQueries queries, HaeRepository haes, HaeViewAssembler views) {
        this.queries = queries;
        this.haes = haes;
        this.views = views;
    }

    /**
     * Busca aplicando o escopo do usuário: professores veem apenas as próprias HAEs; coordenadores e diretores,
     * as da sua instituição; administradores, todas.
     */
    public PageResult<HaeSummary> search(Actor actor, HaeQueries.Criteria requested, PageQuery page) {
        UUID employeeId = actor.role().canViewInstitutionHaes() ? requested.employeeId() : actor.id();
        var criteria = new HaeQueries.Criteria(actor.scopeInstitution(requested.institutionId()),
                requested.courseId(), employeeId, requested.type(), requested.status(), requested.viewed(),
                requested.semester(), requested.text());
        return queries.search(criteria, page);
    }

    public PageResult<HaeSummary> mine(Actor actor, HaeQueries.Criteria requested, PageQuery page) {
        var criteria = new HaeQueries.Criteria(null, requested.courseId(), actor.id(), requested.type(),
                requested.status(), requested.viewed(), requested.semester(), requested.text());
        return queries.search(criteria, page);
    }

    public HaeView get(Actor actor, UUID id) {
        Hae hae = haes.findById(id).orElseThrow(() -> NotFoundException.of("HAE"));
        if (!HaeAccess.canView(actor, hae)) {
            throw NotFoundException.of("HAE");
        }
        return views.toView(actor, hae);
    }
}
