package br.com.cps.hae.application.port;

/** Gera tokens aleatórios (para e-mail) e seu hash para persistência. */
public interface SecureTokens {

    String generate();

    String hash(String token);
}
