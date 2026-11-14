package br.com.cps.hae.domain.hae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;

class HaeTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");
    private final UUID owner = UUID.randomUUID();
    private final UUID coordinator = UUID.randomUUID();
    private Hae hae;

    @BeforeEach
    void setUp() {
        hae = Hae.create(owner, UUID.randomUUID(), UUID.randomUUID(), HaeFixtures.details(HaeType.TCC, 4), NOW);
    }

    @Test
    void novaHaeComecaPendenteENaoVisualizada() {
        assertThat(hae.status()).isEqualTo(HaeStatus.PENDENTE);
        assertThat(hae.viewed()).isFalse();
    }

    @Nested
    class Avaliacao {

        @Test
        void coordenadorAprova() {
            hae.review(ReviewDecision.APPROVE, coordinator, null, NOW);
            assertThat(hae.status()).isEqualTo(HaeStatus.APROVADO);
            assertThat(hae.reviewerId()).isEqualTo(coordinator);
        }

        @Test
        void reprovacaoExigeMotivo() {
            assertThatThrownBy(() -> hae.review(ReviewDecision.REJECT, coordinator, " ", NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        @DisplayName("dono não pode avaliar a própria HAE")
        void naoPermiteAutoAvaliacao() {
            assertThatThrownBy(() -> hae.review(ReviewDecision.APPROVE, owner, null, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        void naoAvaliaDuasVezes() {
            hae.review(ReviewDecision.APPROVE, coordinator, null, NOW);
            assertThatThrownBy(() -> hae.review(ReviewDecision.REJECT, coordinator, "x", NOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    class Edicao {

        @Test
        void edicaoDeHaeAprovadaVoltaParaPendente() {
            hae.review(ReviewDecision.APPROVE, coordinator, "ok", NOW);
            hae.update(hae.courseId(), HaeFixtures.details(HaeType.TCC, 6), NOW);
            assertThat(hae.status()).isEqualTo(HaeStatus.PENDENTE);
            assertThat(hae.reviewerId()).isNull();
            assertThat(hae.details().weeklyHours()).isEqualTo(6);
        }

        @Test
        void naoEditaEmFechamento() {
            hae.review(ReviewDecision.APPROVE, coordinator, null, NOW);
            hae.requestClosure(tcc(), NOW);
            assertThatThrownBy(() -> hae.update(hae.courseId(), HaeFixtures.details(HaeType.TCC, 6), NOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    class Fechamento {

        @Test
        void fluxoCompletoDeFechamento() {
            hae.review(ReviewDecision.APPROVE, coordinator, null, NOW);
            hae.requestClosure(tcc(), NOW);
            assertThat(hae.status()).isEqualTo(HaeStatus.FECHAMENTO_SOLICITADO);
            assertThat(hae.pendingClosure()).isPresent();

            hae.reviewClosure(ReviewDecision.APPROVE, coordinator, null, NOW);
            assertThat(hae.status()).isEqualTo(HaeStatus.COMPLETO);
            assertThat(hae.pendingClosure()).isEmpty();
            assertThat(hae.closures()).singleElement()
                    .satisfies(c -> assertThat(c.decision()).isEqualTo(ReviewDecision.APPROVE));
        }

        @Test
        void fechamentoRecusadoVoltaParaAprovadoEPermiteReenvio() {
            hae.review(ReviewDecision.APPROVE, coordinator, null, NOW);
            hae.requestClosure(tcc(), NOW);
            hae.reviewClosure(ReviewDecision.REJECT, coordinator, "faltam dados", NOW);
            assertThat(hae.status()).isEqualTo(HaeStatus.APROVADO);

            hae.requestClosure(tcc(), NOW);
            assertThat(hae.closures()).hasSize(2);
        }

        @Test
        void relatorioDeveCorresponderAoTipo() {
            hae.review(ReviewDecision.APPROVE, coordinator, null, NOW);
            var estagio = new ClosureReport.Internship("aluno X", null);
            assertThatThrownBy(() -> hae.requestClosure(estagio, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        void naoSolicitaFechamentoDeHaePendente() {
            assertThatThrownBy(() -> hae.requestClosure(tcc(), NOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    private static ClosureReport tcc() {
        return new ClosureReport.Tcc("Orientador", 2, "Ana, Bruno", "Ana", null);
    }

    @Test
    void exclusaoSomenteEmPendenteOuReprovado() {
        hae.ensureDeletable();
        hae.review(ReviewDecision.APPROVE, coordinator, null, NOW);
        assertThatThrownBy(hae::ensureDeletable).isInstanceOf(BusinessRuleViolationException.class);
    }
}
