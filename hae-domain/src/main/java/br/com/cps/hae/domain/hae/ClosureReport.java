package br.com.cps.hae.domain.hae;

import br.com.cps.hae.domain.shared.Guard;

/**
 * Relatório de fechamento, específico para cada tipo de HAE.
 */
public sealed interface ClosureReport {

    int TEXT_MAX = 5_000;

    HaeType type();

    record Tcc(String role, int studentCount, String studentNames, String approvedStudents, String projectInfo)
            implements ClosureReport {

        public Tcc {
            role = Guard.notBlank(role, "papel no TCC", 100);
            studentCount = Guard.range(studentCount, 0, 500, "quantidade de alunos");
            studentNames = Guard.notBlank(studentNames, "nomes dos alunos", TEXT_MAX);
            approvedStudents = Guard.optional(approvedStudents, "alunos aprovados", TEXT_MAX);
            projectInfo = Guard.optional(projectInfo, "informações do projeto", TEXT_MAX);
        }

        @Override
        public HaeType type() {
            return HaeType.TCC;
        }
    }

    record Internship(String studentInfo, String approvedStudents) implements ClosureReport {

        public Internship {
            studentInfo = Guard.notBlank(studentInfo, "informações dos estagiários", TEXT_MAX);
            approvedStudents = Guard.optional(approvedStudents, "alunos aprovados", TEXT_MAX);
        }

        @Override
        public HaeType type() {
            return HaeType.ESTAGIO;
        }
    }

    record DirectionSupport(String supportType, String description, String approvedStudents,
            String certificateStudents) implements ClosureReport {

        public DirectionSupport {
            supportType = Guard.notBlank(supportType, "tipo de apoio", 100);
            description = Guard.notBlank(description, "descrição", TEXT_MAX);
            approvedStudents = Guard.optional(approvedStudents, "alunos aprovados", TEXT_MAX);
            certificateStudents = Guard.optional(certificateStudents, "alunos com certificado", TEXT_MAX);
        }

        @Override
        public HaeType type() {
            return HaeType.APOIO_DIRECAO;
        }
    }
}
