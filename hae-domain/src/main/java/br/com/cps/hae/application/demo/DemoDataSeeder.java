package br.com.cps.hae.application.demo;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.cps.hae.application.port.CourseRepository;
import br.com.cps.hae.application.port.EmployeeRepository;
import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.port.InstitutionRepository;
import br.com.cps.hae.application.port.PasswordHasher;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.employee.Employee;
import br.com.cps.hae.domain.employee.Role;
import br.com.cps.hae.domain.hae.ClosureReport;
import br.com.cps.hae.domain.hae.Dimension;
import br.com.cps.hae.domain.hae.Hae;
import br.com.cps.hae.domain.hae.HaeDetails;
import br.com.cps.hae.domain.hae.HaePeriod;
import br.com.cps.hae.domain.hae.HaeType;
import br.com.cps.hae.domain.hae.Modality;
import br.com.cps.hae.domain.hae.ReviewDecision;
import br.com.cps.hae.domain.hae.TimeRange;
import br.com.cps.hae.domain.institution.Course;
import br.com.cps.hae.domain.institution.Institution;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.domain.shared.Semester;
import jakarta.transaction.Transactional;

/**
 * Popula o banco com uma unidade fictícia completa, cobrindo todos os estados da HAE, para apresentações.
 * Idempotente: não faz nada se a unidade de demonstração já existir. Nenhum e-mail é enviado.
 */
@UseCase
@Transactional
public class DemoDataSeeder {

    public static final int DEMO_INSTITUTION_CODE = 999;
    private static final String DOMAIN = "@fatec-demo.example.com";

    /** Contas criadas pela demonstração (exibidas no login, sem a senha). */
    public static final List<DemoAccount> ACCOUNTS = List.of(
            new DemoAccount("Helena Duarte", "diretora" + DOMAIN, Role.DIRETOR,
                    "Diretora: indicadores, funcionários, cursos e limite de horas"),
            new DemoAccount("Marcos Vieira", "coord.ads" + DOMAIN, Role.COORDENADOR,
                    "Coordenador de ADS: avalia HAEs e fechamentos"),
            new DemoAccount("Patrícia Nunes", "coord.logistica" + DOMAIN, Role.COORDENADOR,
                    "Coordenadora de Logística"),
            new DemoAccount("Ana Ribeiro", "ana.ribeiro" + DOMAIN, Role.PROFESSOR,
                    "Professora de ADS: HAE pendente e HAE aprovada"),
            new DemoAccount("Bruno Carvalho", "bruno.carvalho" + DOMAIN, Role.PROFESSOR,
                    "Professor de ADS: HAE reprovada e fechamento em análise"),
            new DemoAccount("Carla Mendes", "carla.mendes" + DOMAIN, Role.PROFESSOR,
                    "Professora de Logística: HAE concluída"),
            new DemoAccount("Diego Santos", "diego.santos" + DOMAIN, Role.PROFESSOR,
                    "Professor de Gestão Empresarial"));

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final InstitutionRepository institutions;
    private final CourseRepository courses;
    private final EmployeeRepository employees;
    private final HaeRepository haes;
    private final PasswordHasher hasher;
    private final Clock clock;

    public DemoDataSeeder(InstitutionRepository institutions, CourseRepository courses, EmployeeRepository employees,
            HaeRepository haes, PasswordHasher hasher, Clock clock) {
        this.institutions = institutions;
        this.courses = courses;
        this.employees = employees;
        this.haes = haes;
        this.hasher = hasher;
        this.clock = clock;
    }

    /** @return verdadeiro se os dados foram criados nesta execução */
    public boolean seed(DemoSettings settings) {
        if (!settings.canSeed() || institutions.existsByCode(DEMO_INSTITUTION_CODE)) {
            return false;
        }
        Instant now = clock.instant();
        Institution fatec = institutions.save(Institution.create(DEMO_INSTITUTION_CODE, "Fatec Demonstração",
                "Av. Exemplo, 1000 – São Paulo/SP (endereço fictício)", 120, now));
        Course ads = courses.save(Course.create(fatec.id(), "Análise e Desenvolvimento de Sistemas"));
        Course logistica = courses.save(Course.create(fatec.id(), "Logística"));
        Course gestao = courses.save(Course.create(fatec.id(), "Gestão Empresarial"));

        String hash = hasher.hash(settings.password());
        Employee diretora = account(0, hash, fatec, ads);
        Employee coordAds = account(1, hash, fatec, ads);
        Employee coordLog = account(2, hash, fatec, logistica);
        Employee ana = account(3, hash, fatec, ads);
        Employee bruno = account(4, hash, fatec, ads);
        Employee carla = account(5, hash, fatec, logistica);
        Employee diego = account(6, hash, fatec, gestao);

        HaePeriod period = currentPeriod();

        // Ana: uma pendente e uma aprovada
        save(Hae.create(ana.id(), fatec.id(), ads.id(), details("Orientação de TCC – sistemas para ONGs da Zona Leste",
                "Orientação de quatro grupos de TCC que desenvolvem sistemas web para organizações sociais da região, "
                        + "com encontros semanais, revisão de código e acompanhamento das entregas.",
                HaeType.TCC, Dimension.DIMENSAO_1_DIDATICO_PEDAGOGICO, 6, period,
                List.of("1110482612001", "1110482612002", "1110482612003")), now));
        Hae semana = Hae.create(ana.id(), fatec.id(), ads.id(), details("Organização da Semana de Tecnologia",
                "Apoio à direção na organização da Semana de Tecnologia: curadoria de palestras, contato com empresas "
                        + "parceiras e coordenação dos monitores voluntários.",
                HaeType.APOIO_DIRECAO, Dimension.DIMENSAO_7_CONVENIOS_E_PARCEIRAS_INSTITUCIONAIS, 4, period, List.of()),
                now);
        semana.review(ReviewDecision.APPROVE, coordAds.id(), "Projeto alinhado ao calendário institucional.", now);
        save(semana);

        // Bruno: uma reprovada (com motivo) e uma com fechamento em análise
        Hae lab = Hae.create(bruno.id(), fatec.id(), ads.id(), details("Laboratório de Inovação Aberta",
                "Estruturação de um laboratório aberto para prototipação de projetos dos alunos.",
                HaeType.APOIO_DIRECAO, Dimension.DIMENSAO_2_LABORATORIOS_ENSINO_E_EQUIPAMENTOS, 6, period, List.of()),
                now);
        lab.review(ReviewDecision.REJECT, coordAds.id(),
                "Detalhe o cronograma de atividades e quais equipamentos serão utilizados.", now);
        save(lab);
        Hae estagio = Hae.create(bruno.id(), fatec.id(), ads.id(), details("Supervisão de estágios obrigatórios",
                "Acompanhamento dos estágios obrigatórios dos alunos do 5º semestre, com visitas e avaliação dos "
                        + "relatórios.",
                HaeType.ESTAGIO, Dimension.DIMENSAO_4_ATIVIDADES_FORMATIVAS, 4, period,
                List.of("1110482612010", "1110482612011")), now);
        estagio.review(ReviewDecision.APPROVE, coordAds.id(), null, now);
        estagio.requestClosure(new ClosureReport.Internship(
                "Lucas Prado (RA 1110482612010) – Empresa Alfa; Mariana Lopes (RA 1110482612011) – Empresa Beta",
                "Lucas Prado; Mariana Lopes"), now);
        save(estagio);

        // Carla: uma concluída (ciclo completo) e uma pendente
        Hae reversa = Hae.create(carla.id(), fatec.id(), logistica.id(), details(
                "Projeto de logística reversa de eletrônicos",
                "Orientação de TCC sobre coleta e destinação de resíduos eletrônicos em parceria com cooperativas.",
                HaeType.TCC, Dimension.DIMENSAO_3_PESQUISA_E_EXTENSAO_EQUIPAMENTOS_E_LABORATORIOS, 4, period,
                List.of("2220482612020", "2220482612021")), now);
        reversa.review(ReviewDecision.APPROVE, coordLog.id(), "Ótima proposta de extensão.", now);
        reversa.requestClosure(new ClosureReport.Tcc("Orientadora", 2, "Rafael Gomes; Beatriz Alves",
                "Rafael Gomes; Beatriz Alves", "Trabalho apresentado na mostra de TCCs com nota máxima."), now);
        reversa.reviewClosure(ReviewDecision.APPROVE, coordLog.id(), "Fechamento aprovado.", now);
        save(reversa);
        save(Hae.create(carla.id(), fatec.id(), logistica.id(), details("Visitas técnicas a centros de distribuição",
                "Planejamento e acompanhamento de visitas técnicas com as turmas de Logística.",
                HaeType.APOIO_DIRECAO, Dimension.DIMENSAO_4_ATIVIDADES_FORMATIVAS, 2, period, List.of()), now));

        // Diego e o coordenador de ADS: aprovadas
        Hae mentoria = Hae.create(diego.id(), fatec.id(), gestao.id(), details(
                "Programa de mentoria em empreendedorismo",
                "Mentoria quinzenal para alunos com projetos de negócio, em parceria com a incubadora regional.",
                HaeType.APOIO_DIRECAO, Dimension.DIMENSAO_6_DESENVOLVIMENTO_DE_PESSOAS, 4, period, List.of()), now);
        mentoria.review(ReviewDecision.APPROVE, coordAds.id(), null, now);
        save(mentoria);
        Hae ppc = Hae.create(coordAds.id(), fatec.id(), ads.id(), details("Atualização do projeto pedagógico de ADS",
                "Condução das reuniões do NDE para atualização do projeto pedagógico do curso.",
                HaeType.APOIO_DIRECAO, Dimension.DIMENSAO_8_IMPLANTACAO_DE_CURSOS, 4, period, List.of()), now);
        ppc.review(ReviewDecision.APPROVE, coordLog.id(), null, now);
        save(ppc);

        log.info("Dados de demonstração criados: unidade {}, {} contas, 8 HAEs (diretora: {})",
                DEMO_INSTITUTION_CODE, ACCOUNTS.size(), diretora.email());
        return true;
    }

    private Employee account(int index, String passwordHash, Institution institution, Course course) {
        DemoAccount a = ACCOUNTS.get(index);
        return employees.save(Employee.create(a.name(), new EmailAddress(a.email()), passwordHash, a.role(),
                institution.id(), course.id(), clock.instant()));
    }

    private void save(Hae hae) {
        haes.save(hae);
    }

    /** Período dentro do semestre atual, para os dados aparecerem nos indicadores. */
    private HaePeriod currentPeriod() {
        Semester semester = Semester.of(LocalDate.now(clock));
        return new HaePeriod(semester.firstDay().plusDays(14), semester.lastDay().minusDays(14));
    }

    private static HaeDetails details(String title, String description, HaeType type, Dimension dimension, int hours,
            HaePeriod period, List<String> students) {
        int half = hours / 2;
        var schedule = Map.of(
                DayOfWeek.TUESDAY, new TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 0).plusHours(half)),
                DayOfWeek.THURSDAY, new TimeRange(LocalTime.of(14, 0), LocalTime.of(14, 0).plusHours(hours - half)));
        return new HaeDetails(title, description, null, type, Modality.PRESENCIAL, dimension, hours, period, schedule,
                students);
    }
}
