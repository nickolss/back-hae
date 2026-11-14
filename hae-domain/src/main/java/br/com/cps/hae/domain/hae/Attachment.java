package br.com.cps.hae.domain.hae;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Comprovante anexado à HAE (armazenado externamente). */
public record Attachment(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        String url,
        String storageKey,
        Instant uploadedAt) {

    public Attachment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(url);
        Objects.requireNonNull(storageKey);
        Objects.requireNonNull(uploadedAt);
    }
}
