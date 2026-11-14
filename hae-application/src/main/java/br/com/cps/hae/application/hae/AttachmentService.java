package br.com.cps.hae.application.hae;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

import br.com.cps.hae.application.port.FileStorage;
import br.com.cps.hae.application.port.HaeRepository;
import br.com.cps.hae.application.port.FileStorage.StoredFile;
import br.com.cps.hae.application.shared.Actor;
import br.com.cps.hae.application.shared.UseCase;
import br.com.cps.hae.domain.hae.Attachment;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.domain.shared.NotFoundException;

/**
 * Upload de comprovantes. O tipo é validado pela assinatura do conteúdo (magic bytes), não pelo cabeçalho
 * enviado pelo cliente. Sem transação: o upload externo não deve segurar conexão com o banco.
 */
@UseCase
public class AttachmentService {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final HaeService haeService;
    private final HaeRepository haes;
    private final FileStorage storage;
    private final Clock clock;

    public AttachmentService(HaeService haeService, HaeRepository haes, FileStorage storage, Clock clock) {
        this.haeService = haeService;
        this.haes = haes;
        this.storage = storage;
        this.clock = clock;
    }

    public record Upload(String fileName, byte[] content) {
    }

    public HaeView upload(Actor actor, UUID haeId, Upload upload) {
        // valida acesso antes de enviar o arquivo ao armazenamento externo
        var hae = haes.findById(haeId)
                .filter(found -> HaeAccess.canView(actor, found))
                .orElseThrow(() -> NotFoundException.of("HAE"));
        HaeAccess.require(HaeAccess.canManageAttachments(actor, hae));

        byte[] content = upload.content();
        if (content == null || content.length == 0) {
            throw new BusinessRuleViolationException("Arquivo vazio.");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new BusinessRuleViolationException("O arquivo deve ter no máximo 5 MB.");
        }
        FileKind kind = FileKind.detect(content);
        String fileName = sanitize(upload.fileName(), kind.extension);

        StoredFile stored = storage.store("hae/" + haeId, fileName, kind.contentType, content);
        var attachment = new Attachment(UUID.randomUUID(), fileName, kind.contentType, content.length, stored.url(),
                stored.storageKey(), clock.instant());
        try {
            return haeService.addAttachment(actor, haeId, attachment);
        } catch (RuntimeException e) {
            storage.delete(stored.storageKey());
            throw e;
        }
    }

    static String sanitize(String original, String extension) {
        String base = original == null ? "comprovante" : original;
        base = base.replaceAll("[\\\\/]", "_").replaceAll("[^\\p{L}\\p{N}._ -]", "").strip();
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        if (base.isBlank()) {
            base = "comprovante";
        }
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        return base + "." + extension;
    }

    enum FileKind {
        PDF("application/pdf", "pdf", new byte[] {0x25, 0x50, 0x44, 0x46}),
        PNG("image/png", "png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}),
        JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        final String contentType;
        final String extension;
        final byte[] signature;

        FileKind(String contentType, String extension, byte[] signature) {
            this.contentType = contentType;
            this.extension = extension;
            this.signature = signature;
        }

        static FileKind detect(byte[] content) {
            for (FileKind kind : values()) {
                if (content.length >= kind.signature.length && startsWith(content, kind.signature)) {
                    return kind;
                }
            }
            throw new BusinessRuleViolationException("Formato não suportado. Envie PDF, PNG ou JPG.");
        }

        private static boolean startsWith(byte[] content, byte[] prefix) {
            for (int i = 0; i < prefix.length; i++) {
                if (content[i] != prefix[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
