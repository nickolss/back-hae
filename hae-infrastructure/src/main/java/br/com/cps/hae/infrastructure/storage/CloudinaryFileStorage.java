package br.com.cps.hae.infrastructure.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.com.cps.hae.application.port.FileStorage;
import br.com.cps.hae.domain.shared.BusinessRuleViolationException;
import br.com.cps.hae.infrastructure.config.HaeProperties;

/**
 * Armazena comprovantes no Cloudinary usando a API REST assinada (sem SDK, compatível com imagem nativa).
 */
@Component
@RegisterReflectionForBinding(CloudinaryFileStorage.UploadResponse.class)
class CloudinaryFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryFileStorage.class);
    private static final String API = "https://api.cloudinary.com/v1_1/";

    private final HaeProperties.Cloudinary config;
    private final RestClient http;
    private final Clock clock;

    CloudinaryFileStorage(HaeProperties properties, RestClient.Builder builder, Clock clock) {
        this.config = properties.cloudinary();
        this.http = builder.baseUrl(API).build();
        this.clock = clock;
    }

    @Override
    public StoredFile store(String folder, String fileName, String contentType, byte[] content) {
        ensureConfigured();
        String resourceType = contentType.startsWith("image/") ? "image" : "raw";
        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("public_id", UUID.randomUUID().toString());
        params.put("timestamp", String.valueOf(clock.instant().getEpochSecond()));

        var body = new LinkedMultiValueMap<String, Object>();
        params.forEach(body::add);
        body.add("api_key", config.apiKey());
        body.add("signature", sign(params));
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        try {
            UploadResponse response = http.post()
                    .uri("{cloud}/{type}/upload", config.cloudName(), resourceType)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(UploadResponse.class);
            if (response == null || response.secure_url() == null) {
                throw new IllegalStateException("Resposta vazia do Cloudinary");
            }
            return new StoredFile(response.secure_url(), resourceType + ":" + response.public_id());
        } catch (RestClientException | IllegalStateException e) {
            log.error("Falha no upload para o Cloudinary", e);
            throw new BusinessRuleViolationException("Não foi possível enviar o arquivo. Tente novamente.");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (!config.configured() || storageKey == null) {
            return;
        }
        int sep = storageKey.indexOf(':');
        String resourceType = sep > 0 ? storageKey.substring(0, sep) : "raw";
        String publicId = sep > 0 ? storageKey.substring(sep + 1) : storageKey;
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId);
        params.put("timestamp", String.valueOf(clock.instant().getEpochSecond()));
        var body = new LinkedMultiValueMap<String, Object>();
        params.forEach(body::add);
        body.add("api_key", config.apiKey());
        body.add("signature", sign(params));
        try {
            http.post()
                    .uri("{cloud}/{type}/destroy", config.cloudName(), resourceType)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha ao remover arquivo {} do Cloudinary", publicId, e);
        }
    }

    /** Assinatura exigida pela API do Cloudinary: SHA-1(params ordenados + api_secret). */
    private String sign(Map<String, String> sortedParams) {
        String toSign = sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")) + config.apiSecret();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(toSign.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void ensureConfigured() {
        if (config == null || !config.configured()) {
            throw new BusinessRuleViolationException("Envio de arquivos indisponível: armazenamento não configurado.");
        }
    }

    record UploadResponse(String secure_url, String public_id) {
    }
}
