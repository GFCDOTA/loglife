# PROMPT DE KICKOFF — colar numa sessão nova do Claude Code

> Copie o bloco abaixo e cole como primeira mensagem de uma sessão aberta no
> workspace `E:\Claude` (ou direto em `apps\loglife`). Ele carrega o contexto,
> o método de trabalho e a missão. O detalhe vive em `HANDOFF.md` (estado
> auditado + roadmap de 14 itens) e `CLAUDE.md` (bootloader) — o prompt manda
> ler os dois antes de tocar em código.

---

Missão: **criar e melhorar o LogLife** (`E:\Claude\apps\loglife`) — meu backend
pessoal de nutrição (Java 25 / Spring Boot 4.1, hexagonal, PWA embutida) — até
ele virar um produto de log alimentar completo que eu uso todo dia no iPhone.

ANTES de qualquer código, leia nesta ordem:
1. `apps\loglife\CLAUDE.md` (bootloader + gotchas Boot 4)
2. `apps\loglife\HANDOFF.md` (estado AUDITADO + roadmap priorizado de 14 itens)
3. As regras da casa em `E:\Claude\.claude\rules\` (clean-architecture, testing,
   git-workflow, security) — elas mandam em qualquer conflito de estilo.

Execute o ROADMAP do HANDOFF §6 NA ORDEM (fundação #1–#4 primeiro, depois
produto #5→; hardening só quando a superfície exigir). Uma fatia por vez.

MÉTODO (não negociável):
- **Red→green:** bug ou comportamento novo começa com o teste que falha; o fix/
  feature vem depois. IT com Testcontainers pra persistência/API; mock só dos
  meus próprios ports. Nome de teste = comportamento.
- **Hexagonal puro:** domínio sem Spring/JPA; JPA entity separada + mapper;
  `@Transactional` na borda do use case; interface nova só com ≥2 impls reais
  ou fronteira de teste. Nada de event-driven/abstração especulativa.
- **Git:** branch `feat/*`|`fix/*`|`chore/*` off `main` → testes verdes → merge
  `--no-ff` → push → delete. Um commit = uma intenção. NUNCA deixar branch/PR
  aberta ao fim. (PAT não cria PR — merge via git.)
- **Evidência, não claim:** todo "pronto" vem com o comando + resultado real
  (testes, curl no endpoint, screenshot da PWA via headless Chrome). Teste que
  não rodou (ex.: IT sem Docker) é dito explicitamente, nunca presumido.
- **Bifurcação técnica:** decida consultando o LLM local (deepseek-r1 via
  Ollama, `num_predict>=1500`) ou o GPT — não me pergunte salvo mudança real
  de escopo. DONE IS NOT STOP: fechou uma fatia com ROI, emende a próxima do
  roadmap; pare apenas em RED real.

AMBIENTE (gotchas que já custaram tempo — HANDOFF §7):
- `JAVA_HOME` global = JDK 21, projeto = 25 → SEMPRE
  `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot" ./mvnw …`
- Boot live: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=Z:\nope`
- ITs exigem Docker Desktop up; CI Linux quebra se `mvnw` perder o +x.

DEFINIÇÃO DE PRONTO por fatia: testes verdes (incluindo o red→green novo) +
`./mvnw test` local + CI verde no GitHub + fatia landada na main + HANDOFF.md
atualizado (1–3 linhas no checkpoint). Ao fim da sessão: HANDOFF completo
atualizado (skill `handoff`).

Comece agora pelo roadmap #1 (CI: exec bit do mvnw) e siga a ordem.
