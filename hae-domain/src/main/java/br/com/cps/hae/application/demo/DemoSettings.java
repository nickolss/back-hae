package br.com.cps.hae.application.demo;

/**
 * Configuração do modo demonstração.
 *
 * @param enabled  quando verdadeiro, a aplicação cria dados fictícios e o front exibe o aviso de demonstração
 * @param password senha das contas de demonstração (vem de variável de ambiente; nunca do código)
 */
public record DemoSettings(boolean enabled, String password) {

    public boolean canSeed() {
        return enabled && password != null && !password.isBlank();
    }
}
