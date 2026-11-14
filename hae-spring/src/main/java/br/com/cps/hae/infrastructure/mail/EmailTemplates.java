package br.com.cps.hae.infrastructure.mail;

import org.springframework.web.util.HtmlUtils;

/** Templates HTML dos e-mails. Todo conteúdo dinâmico é escapado (evita injeção de HTML). */
final class EmailTemplates {

    private static final String BRAND = "#a6192e";

    private EmailTemplates() {
    }

    static String layout(String title, String bodyHtml) {
        return """
                <!doctype html>
                <html lang="pt-BR"><body style="margin:0;background:#f4f4f5;font-family:Arial,Helvetica,sans-serif;color:#18181b">
                  <div style="max-width:600px;margin:24px auto;background:#fff;border-radius:8px;overflow:hidden;border:1px solid #e4e4e7">
                    <div style="background:%s;color:#fff;padding:16px 24px;font-size:18px;font-weight:bold">Sistema de HAEs · Fatec</div>
                    <div style="padding:24px">
                      <h1 style="font-size:20px;margin:0 0 16px">%s</h1>
                      %s
                    </div>
                    <div style="padding:16px 24px;font-size:12px;color:#71717a;border-top:1px solid #e4e4e7">
                      Mensagem automática. Não responda este e-mail.
                    </div>
                  </div>
                </body></html>
                """.formatted(BRAND, esc(title), bodyHtml);
    }

    static String paragraph(String text) {
        return "<p style=\"line-height:1.5;margin:0 0 12px\">" + esc(text) + "</p>";
    }

    static String button(String label, String url) {
        return """
                <p style="margin:24px 0;text-align:center">
                  <a href="%s" style="background:%s;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;display:inline-block;font-weight:bold">%s</a>
                </p>
                <p style="font-size:12px;color:#71717a;word-break:break-all">Se o botão não funcionar, copie este endereço: %s</p>
                """.formatted(esc(url), BRAND, esc(label), esc(url));
    }

    static String field(String label, String value) {
        return "<p style=\"margin:0 0 8px\"><strong>" + esc(label) + ":</strong> " + esc(value) + "</p>";
    }

    static String preformatted(String text) {
        return "<div style=\"white-space:pre-wrap;background:#f4f4f5;padding:12px;border-radius:6px\">" + esc(text)
                + "</div>";
    }

    static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
