package br.com.cps.hae.application.port;

public interface FileStorage {

    StoredFile store(String folder, String fileName, String contentType, byte[] content);

    void delete(String storageKey);

    record StoredFile(String url, String storageKey) {
    }
}
