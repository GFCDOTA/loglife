# HANDOFF / KICKOFF — LogLife (módulo nutrição)

> Fio da meada. Este é o **kickoff** da linha "criação e melhoria do LogLife":
> estado auditado (4 agentes lendo o código real, 28 achados) + roadmap priorizado.
> Prompt pronto pra abrir uma sessão de trabalho: `PROMPT.md` (ao lado).

- **Data:** 2026-07-20 · kickoff montado após auditoria multi-agente + validação de testes
- **Repo:** `E:\Claude\apps\loglife` · remote `origin` = `github.com/fmodesto30/loglife.git`
- **Status geral:** 🟢 verde local / ⚠️ CI presumido VERMELHO (item #1 do roadmap) —
  `mvnw test` **20 verdes** (com JDK 25; ver gotcha §7.1), `main` @ `561e3d4` pushado,
  working tree com apenas os docs deste kickoff.

## 1. Objetivo
Evoluir o LogLife de "backend que roda" pra **produto pessoal de nutrição completo**:
registrar refeições em texto livre, estimar calorias/macros via **Ollama local** (sem API
pública), e fechar o **loop de uso diário** — meta, edição, repetição de refeições,
tendências — consumido pela PWA no iPhone. Java 25 / Spring Boot 4.1 / hexagonal.

## 2. Branch / git
- **`main` @ `561e3d4`**, sync 0/0 com origin. A branch `feat/nutrition-improvements`
  (per-item logging, Ollama real com JSON Schema, CLAUDE.md) foi **validada (20 testes
  verdes) e MERGEADA na main em 2026-07-20**; deletada local+remoto.
- Fluxo: `feat/*`|`fix/*`|`chore/*` off `main` → merge `--no-ff` → push → delete.
  ⚠️ PAT da máquina não cria PR (`Contents:write` só) → merge via git ou PR por URL.

## 3. Estado auditado (2026-07-20, 4 dimensões — resumos honestos)
- **Arquitetura hexagonal: MUITO BOA.** Domínio 100% sem framework (records + invariantes
  em construtor compacto), ports em `application/port/out`, JPA entity separada + mapper,
  DTOs com Bean Validation na borda, use cases POJO com `Clock` injetado. Right-sizing
  correto (sem inbound-port cerimonial; `CalorieEstimationPort` justificado por 3+ impls +
  decorators Composite/Metrics). **Desvios:** borda transacional está no adapter de
  persistência (não no use case — parcialmente defensável: a chamada Ollama de 120s não
  pode ficar na transação); `IllegalArgumentException→400` genérico no handler esconde
  invariante quebrado como erro de cliente.
- **Testes: SAUDÁVEIS com buracos de borda.** ~20 unit/contract + 6 IT (Testcontainers PG
  real), pirâmide certa, nomes comportamentais, clock fixo, mock só dos próprios ports.
  **Buracos:** caminho de falha total → 503 nunca exercitado; atomicidade do saveAll sem
  teste; erros de transporte do Ollama sem cobertura; **as 2 classes de IT se poluem**
  (mesma data 2026-06-12, sem cleanup — passam pela ordem atual, flaky latente).
- **Produto: CRUD-menos-o-U.** POST (texto→estimativa→N logs), GET por data, daily-summary
  (com breakdown por refeição), DELETE. Domínio maduro (macros completos, MealType,
  confidence+source, per-item). **Faltam:** meta de referência, edição (erro = delete+
  recria re-pagando LLM), visão além de 1 dia, repetição de frequentes, entrada manual.
- **Infra/segurança: sólida pra app pessoal.** Config por env, zero segredo, CORS fechado,
  Flyway V1 = entity, métricas com tags, logs sem PII. **Graves:** `mvnw` sem +x no index
  → **CI quase certamente falha com exit 126**; retry do Ollama re-tenta em QUALQUER
  exceção (pior caso ~240s pendurado); API + Postgres expostos na LAN sem auth.

## 4. Decisões (vigentes)
- Estimativa é **port-driven** (mock|local-agent|ollama|composite) — trocar provider é config.
- Erro de agente = **Result-type** (`EstimationResult`), não exceção; composite cai pro mock.
- Sem inbound-ports cerimoniais; interface só com ≥2 impls ou fronteira de teste (regra da casa).
- **Nada de event-driven/abstração especulativa** — codificar quando o requisito existir.
- Data vem do CLIENTE (evita bug de timezone no servidor) — mudar só no roadmap #14.

## 5. Testes + evidências
- `JAVA_HOME=jdk-25 ./mvnw test` → **Tests run: 20, Failures: 0 — BUILD SUCCESS** (2026-07-20).
- ITs (Testcontainers) **não rodadas localmente nesta validação** (exigem Docker up); o CI
  roda `verify` no ubuntu — mas está presumido vermelho pelo +x (§3/roadmap #1). Conferir o
  Actions após o fix.
- Auditoria: 28 achados com arquivo:linha (4 agentes + síntese; workflow `wf_0a6753c2`).

## 6. ROADMAP (14 itens, ordem = rank; fundação PRIMEIRO)
**Fundação (destrava a confiança no verde):**
1. **CI:** `git update-index --chmod=+x mvnw` + commit; `push:` só main (mantém PR) no ci.yml.
2. **Isolamento das ITs:** truncate de `food_log` em `@BeforeEach` do
   `AbstractPostgresIntegrationTest`; assertar a ordenação real no `findsByDateOnlyAndOrders`.
3. **Falha de estimação:** retry SÓ em falha de parse; transporte/timeout → `failure`
   imediato (mata o pior caso de 240s); testes de transporte + IT do 503.
4. **Borda de erro honesta:** IAE genérica volta a ser 500+log; mealType inválido → 400
   via validação de DTO/mapper dedicado.

**Produto (o loop diário, em ordem de valor):**
5. **Edição:** `UpdateFoodLog` + PATCH sem re-estimar; `EstimationSource.USER_OVERRIDE`
   (confidence 1.0); de quebra move a borda `@Transactional` pro use case (fix do §3).
6. **Entrada manual de macros** (rótulo): bloco `nutrition{...}` opcional no POST →
   pula o LLM, `USER_PROVIDED`.
7. **Meta diária:** `NutritionGoal` + tabela `user_goal` (V2, single-user) + GET/PUT goal;
   summary ganha goal/remaining/percent; barra de progresso na PWA.
8. **Frequentes + repetir sem LLM:** GET frequent (GROUP BY últimos 30d) + POST repeat
   (clona nutrition persistida, zero Ollama); chips na PWA.
9. **Tendências:** `findByDateBetween` no port + `GetNutritionTrend` (7 dias, médias) +
   card na PWA.
10. **Export CSV** (reusa o between).
12. **Logs MOCK visíveis + re-estimar** (badge na PWA; POST re-estimate reusa o update).
14. **Date opcional** com timezone configurada (destrava log por atalho/voz).
15. **Grounding nutricional (P0 herdado de 2026-06-23, a auditoria não via):** tabela
    **TACO** local + lookup leve no prompt do Ollama pro 8b escalar valores reais por-100g
    em vez de inventar. Pré-requisitos: decidir a FONTE da TACO + golden-set de ~30 pratos
    pra medir antes/depois. Encaixar após o #6 (a entrada manual já dá o fallback humano).

**Hardening (quando a superfície importar):**
11. **Rede:** bind 127.0.0.1 por default; token estático `X-Api-Token` quando exposto na
    LAN (SEM puxar Spring Security inteiro); Postgres fora da LAN no compose.
13. **Pinos de contrato:** key-set completo das respostas; IT de rollback do saveAll;
    bordas de parâmetro.

## 7. Gotchas DESTA MÁQUINA (perder tempo aqui é retrabalho)
1. ⚠️ **`JAVA_HOME` global aponta pro JDK 21** e o projeto é Java 25 → surefire quebra com
   "class file version 69.0". SEMPRE:
   `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot" ./mvnw test`.
   (Conserto real = Felipe atualizar o JAVA_HOME do Windows.)
2. ⚠️ **Boot live** exige `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=Z:\nope`
   (loopback do java.exe bloqueado; provável AV).
3. **ITs** exigem Docker Desktop up (Testcontainers PG).
4. Gotchas Boot 4 (Jackson=`tools.jackson`, starter-flyway próprio, RestClient.builder
   estático, JDK25+Mockito argLine): ver `CLAUDE.md` + `.claude/rules/java-spring.md`.
5. **Windows dropa +x** de `mvnw` → CI Linux exit 126; fix `git update-index --chmod=+x`.

## 8. Próximos 5 passos (menor risco primeiro)
1. Roadmap #1 (CI) — 2 comandos, destrava a verificação de todo o resto.
2. Roadmap #2 (isolamento das ITs) — red→green: provar a poluição, consertar a raiz.
3. Roadmap #3 (falha de estimação) — o maior risco de runtime hoje.
4. Roadmap #4 (borda de erro) — pequeno, fecha a fundação.
5. Entrar no produto: #5 (edição) → #7 (meta) → #8 (frequentes).

## 9. Comandos úteis
```bash
REPO="E:/Claude/apps/loglife"; cd "$REPO"
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
./mvnw test                                   # unit/contract (20)
docker compose up -d && ./mvnw verify         # + ITs Testcontainers
JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=Z:\nope" ./mvnw spring-boot:run  # :8080 + PWA
```

## 10. O que NÃO fazer
- **Não** anotar o domínio com Spring/JPA; **não** criar inbound-port cerimonial.
- **Não** re-tentar transporte no adapter Ollama (retry é SÓ parse — decisão do roadmap #3).
- **Não** rodar Maven sem o JAVA_HOME do JDK 25 (§7.1) — o vermelho é ambiente, não código.
- **Não** puxar Spring Security inteiro pro item #11 (token estático basta; single-user).
- **Não** declarar teste que não rodou; ITs exigem Docker — dizer explicitamente se pulou.
- **Não** deixar branch/PR aberta ao fim da sessão (landar ou descartar).

## 11. Checkpoint
Kickoff montado em 2026-07-20: branch antiga validada e mergeada (`main @ 561e3d4`),
auditoria completa, roadmap priorizado, gotcha novo do JAVA_HOME documentado, CLAUDE.md
corrigido (remote existe; regra 5 antiga obsoleta). **Primeiro movimento da próxima
sessão:** colar o `PROMPT.md` e começar pelo roadmap #1. **Sinal de tudo de pé:**
`JAVA_HOME=jdk-25 ./mvnw test` → 20 verdes, e o Actions do GitHub verde após o fix do +x.
