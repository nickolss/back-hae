package br.com.cps.hae.application.hae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.cps.hae.domain.shared.BusinessRuleViolationException;

class AttachmentServiceTest {

    @Test
    void detectaTipoPeloConteudo() {
        assertThat(AttachmentService.FileKind.detect("%PDF-1.7".getBytes()))
                .isEqualTo(AttachmentService.FileKind.PDF);
        assertThatThrownBy(() -> AttachmentService.FileKind.detect("<script>".getBytes()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void sanitizaNomeDoArquivo() {
        assertThat(AttachmentService.sanitize("../../etc/passwd.exe", "pdf")).isEqualTo(".._.._etc_passwd.pdf");
        assertThat(AttachmentService.sanitize("<img onerror=x>.png", "png")).isEqualTo("img onerrorx.png");
        assertThat(AttachmentService.sanitize(null, "jpg")).isEqualTo("comprovante.jpg");
    }
}
