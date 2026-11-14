package br.com.cps.hae.domain.hae;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.Semester;

/**
 * Regra: a soma das horas semanais das HAEs de uma instituição em um semestre não pode exceder o limite
 * definido pela direção.
 */
public final class HoursAllocationPolicy {

    private HoursAllocationPolicy() {
    }

    /**
     * @param limit          limite semanal da instituição
     * @param alreadyAllocated horas já alocadas no semestre (excluindo a própria HAE, em caso de edição)
     * @param requested      horas pedidas pela HAE
     */
    public static void ensureWithinLimit(int limit, int alreadyAllocated, int requested, Semester semester) {
        if (alreadyAllocated + requested > limit) {
            int remaining = Math.max(0, limit - alreadyAllocated);
            throw new BusinessRuleViolationException(
                    "Limite de horas da unidade excedido para o semestre " + semester + ": restam " + remaining
                            + "h de " + limit + "h e foram solicitadas " + requested + "h.");
        }
    }
}
