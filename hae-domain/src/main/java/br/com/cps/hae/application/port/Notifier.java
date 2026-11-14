package br.com.cps.hae.application.port;

import java.util.UUID;

import br.com.cps.hae.domain.hae.HaeStatus;
import br.com.cps.hae.domain.shared.EmailAddress;

/**
 * Notificações por e-mail. Implementações devem ser assíncronas e tolerantes a falhas: uma falha de envio
 * não deve desfazer a operação de negócio.
 */
public interface Notifier {

    void sendAccountActivation(EmailAddress to, String name, String token);

    void sendPasswordReset(EmailAddress to, String name, String token);

    void sendPasswordChanged(EmailAddress to, String name);

    void notifyHaeSubmitted(EmailAddress coordinator, HaeNotice notice);

    void notifyHaeReviewed(EmailAddress professor, HaeNotice notice, HaeStatus newStatus, String comment);

    void notifyClosureRequested(EmailAddress coordinator, HaeNotice notice);

    void sendFeedback(FeedbackMessage message);

    record HaeNotice(UUID haeId, String title, String professorName, String courseName) {
    }

    record FeedbackMessage(String fromName, String fromEmail, String category, String subject, String description) {
    }
}
