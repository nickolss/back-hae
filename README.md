# HAE API — Sistema de Controle de Horas de Atividades Específicas

API REST do sistema de HAEs da Fatec/CPS. Spring Boot 4 + Java 25, compilada como **imagem nativa GraalVM**
(inicialização em ~100 ms, ideal para hospedagem gratuita que "dorme").

## Arquitetura

Projeto Maven **multimódulo** seguindo Clean Architecture — a regra de dependência é verificada por testes
ArchUnit (`hae-bootstrap/src/test/.../ArchitectureTest.java`):

```
hae-bootstrap ──► hae-web ─────────┐
      │                            ▼
      └────────► hae-infrastructure ──► hae-application ──► hae-domain
```

| Módulo | Responsabilidade | Frameworks |
|---|---|---|
| `hae-domain` | Entidades, objetos de valor, regras (máquina de estados da HAE, limite de horas, semestre) | nenhum |
| `hae-application` | Casos de uso (`@UseCase`), portas de saída, autorização por papel/instituição | apenas `jakarta.transaction` |
| `hae-infrastructure` | JPA/Flyway (PostgreSQL), SMTP, Cloudinary (REST assinado), JWT, BCrypt | Spring Data, Spring Security crypto |
| `hae-web` | Controllers REST `/api/v1`, DTOs, segurança HTTP, Problem Details, OpenAPI | Spring MVC, Spring Security |
| `hae-bootstrap` | Composição, configuração, jobs, build nativo | Spring Boot |

### Fluxo da HAE

```
PENDENTE ──aprovar──► APROVADO ──fechamento──► FECHAMENTO_SOLICITADO ──aprovar──► COMPLETO
   └──reprovar──► REPROVADO          ▲                    └──recusar──► APROVADO
Edição pelo professor (PENDENTE|REPROVADO|APROVADO) volta para PENDENTE.
```

## Executando localmente

Requisitos: Java 25 (GraalVM 25 para build nativo), Docker.

```bash
./scripts/dev-env.sh                                  # gera .env com credenciais aleatórias (não versionado)
docker compose up -d                                  # Postgres + Mailpit (e-mails em http://localhost:8025)
set -a; . ./.env; set +a
./mvnw -pl hae-bootstrap -am spring-boot:run
```

- API: http://localhost:8080/api/v1 · Swagger: http://localhost:8080/swagger-ui.html
- Admin local: `admin@hae.local`, senha em `INITIAL_ADMIN_PASSWORD` no `.env`
- **Nenhum segredo fica no código**: sem `JWT_SECRET` a aplicação não sobe (inclusive no perfil `dev`).

### Testes

```bash
./mvnw verify          # unitários, ArchUnit e integração (Testcontainers — requer Docker)
```

### Imagem nativa

```bash
./mvnw -Pnative -DskipTests package && ./hae-bootstrap/target/hae-api
docker build -t hae-api .            # build nativo dentro do contêiner
```

## Segurança

- Sessão: JWT HS256 em cookie `__Host-hae_session` (HttpOnly, Secure, SameSite=Lax, 2 h); papel recarregado do
  banco a cada requisição.
- CSRF: double-submit cookie (`XSRF-TOKEN` → header `X-XSRF-TOKEN`).
- Senhas com BCrypt (custo 12); tokens de ativação/redefinição de 256 bits, guardados como SHA-256, de uso único.
- Autorização centralizada nos casos de uso (escopo por instituição; recursos de outra unidade retornam 404).
- Rate limit nos endpoints de autenticação; cabeçalhos de segurança (CSP, HSTS, frame-deny, no-referrer).
- Uploads validados pelo conteúdo (magic bytes: PDF/PNG/JPG), máx. 5 MB.

## Deploy

`push` em `main` → **CI** (testes, CodeQL, gitleaks) → **imagem nativa** → smoke test → **Grype** (falha em
HIGH/CRITICAL corrigíveis) → **Docker Hub** (`latest` + `sha-…`) → **Deploy Hook do Render** com o digest exato →
health check.

Configuração no GitHub (Settings → Secrets and variables → Actions):

| Tipo | Nome | Valor |
|---|---|---|
| Variable | `DOCKERHUB_USERNAME` | usuário do Docker Hub |
| Secret | `DOCKERHUB_TOKEN` | access token do Docker Hub (escopo Read & Write) |
| Secret | `RENDER_DEPLOY_HOOK_URL` | Render → serviço → Settings → Deploy Hook |
| Variable | `RENDER_SERVICE_URL` | ex.: `https://hae-api.onrender.com` |

Variáveis de ambiente da aplicação: veja [`env.example`](env.example).
