# HANDOFF — LogLife (módulo nutrição)

> Fio da meada. Atualizado ao FIM da sessão de execução de 2026-07-20, que pegou o
> kickoff (auditoria + roadmap de 14 itens) e **landou #1–#14 (exceto #15) na main**.
> Prompt de sessão: `PROMPT.md` (ao lado). Bootloader: `CLAUDE.md`.

- **Data:** 2026-07-20 (fim da sessão de execução)
- **Repo:** `E:\Claude\apps\loglife` · remote `origin` = `github.com/fmodesto30/loglife.git`
- **Status geral:** 🟢 **main @ `f205c81`, working tree limpa, CI VERDE no GitHub**
  (primeiro verde da história do repo nesta sessão; depois verde em TODOS os merges).
  Suíte: **44 unit/contract + 22 IT** (Testcontainers PG real).

## 1. Objetivo
Evoluir o LogLife de "backend que roda" pra **produto pessoal de nutrição completo** no
iPhone. **O loop diário FECHOU nesta sessão:** registrar (LLM ou rótulo manual), editar sem
re-pagar LLM, meta com barra de progresso, repetir frequentes com 1 toque, tendência de 7
dias, export CSV, re-estimar MOCKs, date opcional por voz. Falta grounding (#15).

## 2. Branch / git
- **`main` @ `f205c81`**, sync 0/0 com origin, working tree limpa, zero branch órfã.
- 13 fatias landadas hoje via `feat/*`|`fix/*`|`test/*` → verify verde → merge `--no-ff` →
  push → delete. ⚠️ PAT sem `Pull requests:write` → merge via git (sem PR).

## 3. O que foi feito (sessão 2026-07-20)
**Fundação #1–#4:** exec bit do mvnw (CI destravou); **descoberta real no #2**: além da
poluição de dados, o `@Container` static parava o PG após a 1ª classe de IT — a suíte de
ITs NUNCA tinha sido verde inteira → container singleton (static block) + `TRUNCATE` em
`@BeforeEach` + ordenação assertada; #3 transporte do Ollama NÃO re-tenta (fim do pior caso
240s) + flag `fallback-to-mock` (default true; `false` → 503 real, agora com IT); #4 IAE
nua = 500 (bug nosso), `InvalidRequestException` tipada = 400 com fieldError.

**Produto #5–#10, #12, #14:** PATCH sem re-estimar (`USER_OVERRIDE`, confidence 1.0,
`@Transactional` no use case); POST com bloco `nutrition{...}` = MANUAL sem LLM;
meta diária (V2 `user_goal` single-row, GET=204 quando unset, PUT upsert, summary com
goal/remaining/percent honestos — negativo/>100% no estouro — e barra na PWA);
frequentes (GROUP BY 30d) + `POST /{id}/repeat` clonando nutrition persistida (zero
Ollama, chips na PWA); tendência 7 dias (buckets zero-filled, média SÓ sobre dias com
log, card na PWA); export CSV RFC-4180; re-estimar in-place (badge MOCK ganhou ação);
date opcional resolvida em `loglife.nutrition.timezone` (edge 01:15 UTC = ontem em SP,
testado).

**Hardening #11 + #13:** bind `127.0.0.1` default + filtro `X-Api-Token` (constant-time,
só com `LOGLIFE_API_TOKEN` setado; PWA manda de `localStorage['loglife.apiToken']`);
Postgres loopback-only no compose; key-set completo de TODAS as respostas pinado
(`ResponseContractIT`); IT de rollback do `saveAll` (1e9 kcal estoura NUMERIC → batch
inteiro reverte).

**Bônus (bug real achado em verificação viva):** service worker era cache-first com
cache congelado v1 → shell nunca atualizava no iPhone. Agora network-first com fallback
offline (v2).

## 4. Decisões (vigentes)
- Estimativa **port-driven**; erro = Result-type; composite→mock **configurável**
  (`fallback-to-mock`, default mantém comportamento).
- Retry do Ollama **SÓ em parse** (output inparseável). Transporte/HTTP → failure imediato.
- `MANUAL` = entrada de rótulo (novo caminho no create); `USER_OVERRIDE` = correção de
  log estimado (novo valor de enum; coluna é VARCHAR, sem migration).
- Repeat **preserva proveniência** (source/confidence do original) — os números continuam
  vindo de onde vieram.
- Média de tendência **ignora dias vazios** (dado ausente ≠ 0 kcal).
- Sem Spring Security: token estático de filtro é o right-size (single-user).
- Data vem do cliente; **ausente** → hoje na timezone configurada (#14 feito).

## 5. Testes + evidências
- `JAVA_HOME=jdk-25 ./mvnw verify` → **44 unit/contract + 22 IT, BUILD SUCCESS** (local,
  Docker up). CI ubuntu roda o mesmo `verify` — verde em todos os merges de hoje.
- Prova VIVA no `:8080` (workaround AF_UNIX): 204→PUT goal→POST manual→summary 33%;
  chip repeat 1→2 logs e barra 33%→65%; trend com 7 barras e hoje destacada; tudo sem
  erro de console. (Screenshot do pane travou por bug do renderer; evidência via
  read_page + JS no Chrome pane.)

## 6. ROADMAP — estado
✅ #1 CI · ✅ #2 isolamento ITs · ✅ #3 falha de estimação · ✅ #4 borda de erro ·
✅ #5 edição · ✅ #6 entrada manual · ✅ #7 meta · ✅ #8 frequentes/repeat ·
✅ #9 tendências · ✅ #10 CSV · ✅ #11 rede/token · ✅ #12 MOCK re-estimar ·
✅ #13 pinos de contrato · ✅ #14 date opcional
**⬜ #15 (ÚNICO restante): grounding TACO** — tabela local + lookup no prompt do Ollama.
Pré-requisitos ANTES de codar: (a) decidir a FONTE da TACO (CSV oficial UNICAMP vs
dataset derivado), (b) montar golden-set de ~30 pratos BR com valores esperados pra
medir antes/depois. Sem golden-set, "melhorou" seria opinião.

## 7. Gotchas DESTA MÁQUINA
1. ⚠️ `JAVA_HOME` global = JDK 21; projeto = 25 → SEMPRE
   `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot" ./mvnw …`
   (senão "class file version 69.0").
2. ⚠️ Boot live exige `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=Z:\nope`.
3. ITs exigem Docker Desktop up (Testcontainers PG).
4. ⚠️ **NOVO (#11): bind default agora é `127.0.0.1`.** Pro iPhone na LAN:
   `LOGLIFE_BIND=0.0.0.0` + `LOGLIFE_API_TOKEN=<segredo>` e no Safari do iPhone
   `localStorage.setItem('loglife.apiToken','<segredo>')` uma vez.
5. ⚠️ **NOVO: service worker.** Se a PWA parecer velha após deploy, é o SW v1 antigo;
   um reload duplo resolve (o v2 network-first assume). Não voltar a cache-first.
6. Gotchas Boot 4 (tools.jackson, starter-flyway, RestClient.builder, argLine): `CLAUDE.md`.
7. Windows dropa +x de `mvnw` — o bit está no index agora; não reintroduzir via
   checkout esperto.

## 8. Próximos 5 passos (menor risco primeiro)
1. **#15a:** decidir fonte TACO (recomendo CSV oficial TACO 4ª ed. UNICAMP; ~600 alimentos)
   — bifurcação técnica: resolver com GPT-Docker :8899, não com o Felipe.
2. **#15b:** golden-set de ~30 pratos BR (texto livre → kcal/macros esperados) como
   fixture de teste + script de score (antes/depois do grounding).
3. **#15c:** tabela `taco_food` (V3) + lookup leve (normalização + LIKE/trigram) injetado
   no prompt do Ollama ("valores por 100g: …") — flag pra ligar/desligar.
4. **#15d:** rodar o golden-set com Ollama real (llama3.1:8b local) sem/com grounding;
   só declarar vitória com o score melhor.
5. Felipe (ambiente): atualizar JAVA_HOME global pro JDK 25; setar LOGLIFE_BIND/TOKEN
   se for usar do iPhone.

## 9. Comandos úteis
```bash
cd /e/Claude/apps/loglife
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
./mvnw test                                    # 44 unit/contract
docker compose up -d && ./mvnw verify          # + 22 ITs
JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=Z:\nope" ./mvnw spring-boot:run
# LAN: LOGLIFE_BIND=0.0.0.0 LOGLIFE_API_TOKEN=... (PWA: localStorage loglife.apiToken)
```

## 10. O que NÃO fazer
- **Não** re-tentar transporte no adapter Ollama (decisão #3; teste pina 1 tentativa).
- **Não** anotar domínio com Spring/JPA; `@Transactional` só em use case SEM chamada LLM
  dentro (Create/Reestimate ficam fora por design).
- **Não** mudar key de resposta sem atualizar `ResponseContractIT` — o pin é intencional.
- **Não** voltar o service worker pra cache-first.
- **Não** começar o #15 sem o golden-set (#15b) — sem medida, grounding é fé.
- **Não** rodar Maven sem JAVA_HOME do 25; **não** declarar IT que não rodou.

## 11. Checkpoint
**Onde parei:** roadmap #1–#14 completo e landado; main @ `f205c81` verde local+CI;
working tree limpa; nenhuma branch/PR aberta. App ficou de pé no `:8080` da máquina
(instância anterior ao #11, ainda bind 0.0.0.0 — morre no próximo reboot).
**Primeiro movimento da próxima sessão:** #15a — consultar o GPT-Docker sobre a fonte
TACO e desenhar o golden-set (§8).
**Sinal de tudo de pé:** `mvnw verify` → 44+22 verdes; Actions do GitHub verde na main.
