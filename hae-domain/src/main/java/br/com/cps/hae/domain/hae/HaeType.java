package br.com.cps.hae.domain.hae;

/** Tipo de projeto da HAE. Define o formulário de fechamento e se há alunos vinculados. */
public enum HaeType {
    APOIO_DIRECAO(false),
    ESTAGIO(true),
    TCC(true);

    private final boolean hasStudents;

    HaeType(boolean hasStudents) {
        this.hasStudents = hasStudents;
    }

    public boolean hasStudents() {
        return hasStudents;
    }
}
