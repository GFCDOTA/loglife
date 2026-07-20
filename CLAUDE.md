# Claude Operating Instructions — loglife

> Bootloader curto. LogLife = app pessoal de logging de vida; este repo é o
> **módulo de nutrição/calorias** (Java 25 / Spring Boot 4.1, hexagonal).
> Padrões de engenharia transversais vivem em `E:\Claude\.claude\rules\`
> (`java-spring.md`, `clean-architecture.md`, `testing.md`, `git-workflow.md`,
> `security.md`) — carregam quando a sessão roda do workspace.

## Stack
Java **25** · Spring Boot **4.1.0** · Maven · MVC + Data JPA + Validation +
Actuator + Micrometer/Prometheus + Flyway + PostgreSQL. Package raiz
`com.loglife.nutrition`.

## Build / test
- `mvn test` — unitários (surefire).
- `mvn verify` — integração `*IT` (failsafe) + **Testcontainers PG real**.
- `mvn spring-boot:run` — sobe a app (Tomcat :8080).
- ⚠️ **`JAVA_HOME` da máquina aponta pro JDK 21** (Temurin 21.0.11), mas o projeto
  é **Java 25** → surefire quebra com "class file version 69.0". Rodar SEMPRE com
  `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"` no comando
  (verificado 2026-07-20: 20 testes verdes assim). Conserto real = atualizar o
  JAVA_HOME do Windows (decisão do Felipe; muda o ambiente global).
- ⚠️ **`java.exe` loopback BLOQUEADO nesta máquina** (`Selector.open()` EINVAL;
  `netsh winsock reset`+reboot não resolve). A app **não sobe live aqui** sem
  `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=Z:\nope` (força TCP loopback).
  Raiz provável = antivírus; conserto real = whitelist do `java.exe`. Já rodou
  live (POST/GET/summary/DELETE + PWA no Chrome) com o workaround.

## Arquitetura (hexagonal — ports & adapters)
```
com.loglife.nutrition
├── domain          ← records, value objects, exceções de domínio. SEM Spring/JPA.
├── application
│   ├── port.out    ← CalorieEstimationPort, FoodLogRepository (interfaces)
│   └── usecase     ← CreateFoodLog, ListFoodLogsByDate, GetDailyNutritionSummary…
├── infrastructure
│   ├── persistence ← JpaEntity + mapper + RepositoryAdapter
│   ├── estimation  ← adapters mock | local-agent | ollama | composite | metrics
│   ├── config      ← @ConfigurationProperties, CORS
│   └── observability
└── api             ← controllers + DTOs + GlobalExceptionHandler
```

## Hard rules
1. **Domínio livre de framework** — sem `@Entity`/`@Component` no agregado.
   Persistência é entity separada + mapper. (Ver `clean-architecture.md`.)
2. **Estimativa de calorias é port-driven** — trocar provider (mock/local/ollama)
   é config (`EstimationProperties`), não código de domínio.
3. **`@Transactional` na borda do use case**, nunca no domínio/controller.
4. **Develop-first** — branch `feat/`|`fix/`|`chore/`; nunca direto em main.
5. **Remote = `github.com/fmodesto30/loglife.git`** — toda fatia landada termina
   pushada na main (CI do GitHub roda as ITs Testcontainers que esta máquina
   não roda).

Detalhe técnico e gotchas Boot 4 (Jackson=`tools.jackson`, `spring-boot-starter-flyway`,
sem bean `RestClient.Builder`, JDK 25 + Mockito argLine): `java-spring.md`.
