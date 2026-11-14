package br.com.cps.hae.application.feedback;

import br.com.cps.hae.application.port.Notifier;
import br.com.cps.hae.application.port.Notifier.FeedbackMessage;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.shared.Guard;

@UseCase
public class FeedbackService {

    public enum Category {
        BUG,
        SUGESTAO,
        DUVIDA
    }

    private final Notifier notifier;

    public FeedbackService(Notifier notifier) {
        this.notifier = notifier;
    }

    public void send(Actor actor, Category category, String subject, String description) {
        Guard.notNull(category, "categoria");
        notifier.sendFeedback(new FeedbackMessage(actor.name(), actor.email(), category.name(),
                Guard.notBlank(subject, "assunto", 150), Guard.notBlank(description, "descrição", 5_000)));
    }
}
