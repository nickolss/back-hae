package br.com.cps.hae.infrastructure.mail;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import br.com.cps.hae.application.port.Notifier;
import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.shared.EmailAddress;
import br.com.cps.hae.infrastructure.config.HaeProperties;
import jakarta.mail.MessagingException;

/**
 * Envio assíncrono via SMTP. Falhas são registradas e não interrompem o fluxo de negócio. Tokens vão no
 * fragmento da URL (#token=...) para não aparecerem em logs de servidor nem no cabeçalho Referer.
 */
@Component
class SmtpNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotifier.class);

    private final JavaMailSender sender;
    private final HaeProperties.Mail config;

    SmtpNotifier(JavaMailSender sender, HaeProperties properties) {
        this.sender = sender;
        this.config = properties.mail();
    }

    @Async
    @Override
    public void sendAccountActivation(EmailAddress to, String name, String token) {
        String link = frontend("/ativar-conta#token=" + token);
        send("ativacao", to.value(), "Ative sua conta", EmailTemplates.layout("Confirme seu cadastro",
                EmailTemplates.paragraph("Olá, " + name + "!")
                        + EmailTemplates.paragraph("Para concluir o cadastro no Sistema de HAEs, confirme seu e-mail."
                                + " O link expira em 24 horas.")
                        + EmailTemplates.button("Ativar minha conta", link)
                        + EmailTemplates.paragraph("Se você não fez este cadastro, ignore esta mensagem.")));
    }

    @Async
    @Override
    public void sendPasswordReset(EmailAddress to, String name, String token) {
        String link = frontend("/redefinir-senha#token=" + token);
        send("redefinicao-senha", to.value(), "Redefinição de senha", EmailTemplates.layout("Redefinição de senha",
                EmailTemplates.paragraph("Olá, " + name + ".")
                        + EmailTemplates.paragraph("Recebemos um pedido para redefinir sua senha. O link é válido por"
                                + " 30 minutos e só pode ser usado uma vez.")
                        + EmailTemplates.button("Criar nova senha", link)
                        + EmailTemplates.paragraph("Se não foi você, ignore este e-mail: sua senha continua a mesma.")));
    }

    @Async
    @Override
    public void sendPasswordChanged(EmailAddress to, String name) {
        send("senha-alterada", to.value(), "Sua senha foi alterada", EmailTemplates.layout("Senha alterada",
                EmailTemplates.paragraph("Olá, " + name + ". A senha da sua conta acabou de ser alterada.")
                        + EmailTemplates.paragraph("Se não foi você, redefina a senha imediatamente e avise a"
                                + " coordenação.")));
    }

    @Async
    @Override
    public void notifyHaeSubmitted(EmailAddress coordinator, HaeNotice notice) {
        send("hae-enviada", coordinator.value(), "Nova HAE para avaliação: " + notice.title(), EmailTemplates.layout(
                "HAE aguardando avaliação",
                EmailTemplates.field("Professor(a)", notice.professorName())
                        + EmailTemplates.field("Curso", notice.courseName())
                        + EmailTemplates.field("Projeto", notice.title())
                        + EmailTemplates.button("Avaliar HAE", frontend("/haes/" + notice.haeId()))));
    }

    @Async
    @Override
    public void notifyHaeReviewed(EmailAddress professor, HaeNotice notice, HaeStatus newStatus, String comment) {
        String statusLabel = switch (newStatus) {
            case APROVADO -> "aprovada";
            case REPROVADO -> "reprovada";
            case COMPLETO -> "concluída (fechamento aprovado)";
            default -> "atualizada (" + newStatus + ")";
        };
        send("hae-avaliada", professor.value(), "Sua HAE foi " + statusLabel, EmailTemplates.layout("HAE " + statusLabel,
                EmailTemplates.field("Projeto", notice.title())
                        + (comment == null ? "" : EmailTemplates.field("Comentário", comment))
                        + EmailTemplates.button("Ver HAE", frontend("/haes/" + notice.haeId()))));
    }

    @Async
    @Override
    public void notifyClosureRequested(EmailAddress coordinator, HaeNotice notice) {
        send("fechamento-solicitado", coordinator.value(), "Fechamento de HAE solicitado: " + notice.title(), EmailTemplates.layout(
                "Fechamento aguardando avaliação",
                EmailTemplates.field("Professor(a)", notice.professorName())
                        + EmailTemplates.field("Projeto", notice.title())
                        + EmailTemplates.button("Avaliar fechamento", frontend("/haes/" + notice.haeId()))));
    }

    @Async
    @Override
    public void sendFeedback(FeedbackMessage message) {
        send("feedback", config.feedbackTo(), "[Feedback " + message.category() + "] " + message.subject(),
                EmailTemplates.layout("Nova mensagem de suporte",
                        EmailTemplates.field("De", message.fromName() + " <" + message.fromEmail() + ">")
                                + EmailTemplates.field("Categoria", message.category())
                                + EmailTemplates.field("Assunto", message.subject())
                                + EmailTemplates.preformatted(message.description())));
    }

    private String frontend(String path) {
        String base = config.frontendUrl() == null ? "" : config.frontendUrl().replaceAll("/+$", "");
        return base + path;
    }

    /**
     * @param kind identificador fixo do tipo de e-mail, usado nos logs. O assunto não é registrado porque pode
     *             conter texto digitado por usuários (evita injeção em logs).
     */
    private void send(String kind, String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            log.warn("E-mail [{}] não enviado: destinatário não configurado", kind);
            return;
        }
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(config.from());
            helper.setTo(to);
            helper.setSubject(subject.replaceAll("[\\r\\n]", " "));
            helper.setText(html, true);
            sender.send(message);
            log.info("E-mail [{}] enviado", kind);
        } catch (MessagingException | MailException e) {
            log.error("Falha ao enviar e-mail [{}]", kind, e);
        }
    }
}
