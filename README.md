# LogLife — módulo de Alimentação / Calorias

App pessoal para registro de vida. Este repositório é o **backend do módulo de alimentação**:
registrar o que se come, estimar calorias e macros **consultando agentes locais** na própria
máquina (sem APIs públicas de nutrição), e ver totais por dia. Inclui uma **PWA** mobile-first
para usar no iPhone.

> ⚠️ As estimativas são **aproximadas** e servem só para registro. Isto **não** é aconselhamento
> médico, dieta ou meta calórica.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | **Java 25** |
| Framework | **Spring Boot 4.1.0** (Spring MVC, Spring Data JPA) |
| Build | **Maven** (via Maven Wrapper `./mvnw`) |
| Banco | **PostgreSQL** + **Flyway** (migrations) |
| Validação | Bean Validation (Jakarta) |
| JSON | Jackson 3 (`tools.jackson`) |
| Observabilidade | Actuator + Micrometer (Prometheus) |
| Testes | JUnit 6, AssertJ, Mockito, **Testcontainers** |
| Frontend | PWA estática (HTML/CSS/JS) servida pelo Spring |

---

## Arquitetura (Hexagonal / Clean, pragmática)

```
com.loglife
├── nutrition.domain          # núcleo puro — SEM Spring, SEM JPA
│   FoodLog, MealType, NutritionFacts, FoodQuantity, Confidence,
│   FoodDescription, NutritionEstimate, DailyNutritionSummary, ...
├── nutrition.application      # casos de uso + PORTS (interfaces) — SEM Spring
│   usecase/  CreateFoodLog, ListFoodLogsByDate, GetDailyNutritionSummary, DeleteFoodLog
│   port/out/ FoodLogRepository, CalorieEstimationPort, EstimationResult
├── nutrition.infrastructure  # adapters concretos + wiring Spring
│   persistence/  FoodLogJpaEntity, *RepositoryAdapter (port → JPA)
│   estimation/   Mock | LocalAgent | Ollama | Composite (Strategy + fallback)
│   observability/ NutritionMetrics
│   config/       EstimationConfiguration, UseCaseConfiguration, WebCorsConfig
└── nutrition.api             # inbound adapter REST
    FoodLogController, NutritionSummaryController, DTOs, GlobalExceptionHandler
```

Regras respeitadas:
- **Domínio não depende de Spring nem do banco nem do agente.**
- **Controllers não têm regra de negócio** — só adaptam HTTP ↔ casos de uso.
- A estimativa fica **atrás do port `CalorieEstimationPort`**; trocar de agente é trocar adapter.
- Erros via modelo explícito (`EstimationResult` para "agente caiu") + hierarquia de exceptions
  mapeada por `@RestControllerAdvice` (nunca um `500` cru com stack trace).

---

## Como rodar localmente

### Pré-requisitos
- **Java 25** (`java -version`)
- **Docker** + Docker Compose (para o PostgreSQL e para os testes de integração)

### 1. Subir o PostgreSQL
```bash
docker compose up -d
```
Sobe `postgres:17` em `localhost:5432` (db/usuário/senha: `loglife`/`loglife`/`loglife`).

### 2. Subir a aplicação
```bash
./mvnw spring-boot:run
```
(no Windows: `mvnw.cmd spring-boot:run`). O Flyway cria a tabela `food_logs` no primeiro start.
A app sobe em `http://localhost:8080` e já serve a PWA na raiz.

> O `./mvnw` baixa o Maven 3.9.16 na primeira execução. Não é preciso ter Maven instalado.

> **Troubleshooting (Windows):** se o start falhar com `Unable to establish loopback connection` /
> `java.net.SocketException: Invalid argument: connect`, o problema **não é a aplicação** — é o
> SO/antivírus bloqueando conexões de loopback do `java.exe` (afeta qualquer servidor Java NIO e o
> Testcontainers). Soluções: liberar o `java.exe`/JDK no antivírus/firewall, ou rodar
> `netsh winsock reset` (admin) e reiniciar.

---

## Endpoints

Base: `http://localhost:8080`

| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/api/v1/food-logs` | cria um registro a partir de texto livre |
| `GET`  | `/api/v1/food-logs?date=YYYY-MM-DD` | lista registros do dia |
| `GET`  | `/api/v1/nutrition/daily-summary?date=YYYY-MM-DD` | totais do dia |
| `DELETE` | `/api/v1/food-logs/{id}` | remove um registro |

### Exemplos (curl)
```bash
# criar
curl -s -X POST http://localhost:8080/api/v1/food-logs \
  -H 'Content-Type: application/json' \
  -d '{"date":"2026-06-12","mealType":"LUNCH","description":"2 bifes médios e 200g de arroz"}'

# listar o dia
curl -s "http://localhost:8080/api/v1/food-logs?date=2026-06-12"

# resumo do dia
curl -s "http://localhost:8080/api/v1/nutrition/daily-summary?date=2026-06-12"

# excluir
curl -s -X DELETE http://localhost:8080/api/v1/food-logs/<id>
```

Validação: `date` e `mealType` obrigatórios; `description` mínimo 2 caracteres; `quantity`/macros
não-negativos; `confidence` entre 0 e 1. Erros voltam como `ApiError` estruturado.

---

## Estimativa por agentes locais

A estimativa é selecionada por `loglife.nutrition.estimation.provider`:

| provider | adapter | observação |
|---|---|---|
| `mock` (default) | `MockCalorieEstimationAdapter` | valores de exemplo, `source=MOCK`, confiança 0.2 — **nunca finge ser real**. App sobe sem nenhum agente. |
| `local-agent` | `LocalAgentCalorieEstimationAdapter` | fala o contrato HTTP abaixo; com **fallback** automático para o mock. |
| `ollama` | `OllamaCalorieEstimationAdapter` | usa um LLM **Ollama real** rodando na sua máquina; fallback para o mock. |

Quando `local-agent`/`ollama` falham (timeout, offline, resposta vazia), o
`CompositeCalorieEstimationAdapter` cai no mock, **loga** e **conta métricas**.

### Trocar de provider
```bash
# Ollama (LLM local real em :11434)
LOGLIFE_ESTIMATION_PROVIDER=ollama LOGLIFE_OLLAMA_MODEL=llama3.1:8b ./mvnw spring-boot:run

# agente HTTP custom em :8787
LOGLIFE_ESTIMATION_PROVIDER=local-agent LOGLIFE_LOCAL_AGENT_URL=http://localhost:8787 ./mvnw spring-boot:run
```

### Contrato do agente HTTP local (onde plugar o agente real)
`POST {base-url}/estimate-calories`

Request:
```json
{ "description": "2 bifes médios e 200g de arroz", "language": "pt-BR", "date": "2026-06-12" }
```
Response:
```json
{
  "items": [
    { "name": "bife bovino grelhado", "quantity": 2, "unit": "unidade média",
      "calories": 420, "proteinGrams": 52, "carbsGrams": 0, "fatGrams": 22, "confidence": 0.72 },
    { "name": "arroz cozido", "quantity": 200, "unit": "g",
      "calories": 260, "proteinGrams": 5, "carbsGrams": 56, "fatGrams": 1, "confidence": 0.85 }
  ],
  "total": { "calories": 680, "proteinGrams": 57, "carbsGrams": 56, "fatGrams": 23 },
  "source": "LOCAL_AGENT", "confidence": 0.78,
  "explanation": "Estimativa baseada nos alimentos informados pelo usuário."
}
```

Para plugar **seu** agente real: implemente esse endpoint (qualquer linguagem) e aponte
`LOGLIFE_LOCAL_AGENT_URL` para ele — nada mais muda. O contrato está fixado por teste
(`LocalAgentCalorieEstimationAdapterTest`).

---

## Testes

```bash
./mvnw test      # unit + contrato (sem Docker)
./mvnw verify    # + integração com Testcontainers (precisa do Docker rodando)
```

- **Unit**: `CreateFoodLogTest`, `GetDailyNutritionSummaryTest`, `CompositeCalorieEstimationAdapterTest`
- **Contrato**: `LocalAgentCalorieEstimationAdapterTest` (fixa o JSON do agente via `MockRestServiceServer`)
- **Integração** (Testcontainers PostgreSQL): `FoodLogRepositoryAdapterIT`, `FoodLogApiIT` (fluxo HTTP completo)

---

## Observabilidade

Actuator (`/actuator`): `health`, `info`, `metrics`, `prometheus`.

Métricas custom:
- `loglife.nutrition.estimates{source=LOCAL_AGENT|OLLAMA|MOCK}` — estimativas por fonte (conta em **todos** os providers, inclusive o mock)
- `loglife.nutrition.primary.failures{provider=local-agent|ollama}` — falhas do agente local primário, por provider
- `loglife.nutrition.estimation.fallbacks` — vezes que o fallback foi usado

Logs estruturados nos pontos-chave: criação do food log, chamada ao agente, fallback acionado,
erro de validação.

---

## Segurança e privacidade

- **Tudo é local neste MVP.** Nenhum dado de alimentação sai para serviço externo.
- O **texto livre do alimento não é logado em nível INFO** (só `DEBUG`), por ser dado pessoal.
- Sem login no MVP. O design já isola o domínio para acrescentar autenticação depois sem
  refatorar regra de negócio.

---

## PWA / iPhone

A PWA é servida pelo próprio Spring em `http://<host>:8080/` (mesma origem da API → no iPhone,
**não há `localhost` hardcoded**; os caminhos são relativos).

### Acessar do iPhone (mesma rede Wi-Fi)
1. Descubra o IP da máquina: `ipconfig` (Windows) → ex. `192.168.0.10`.
2. No Safari do iPhone abra `http://192.168.0.10:8080`.
3. Compartilhar → **Adicionar à Tela de Início** (vira app standalone com ícone).
4. Como a PWA é **same-origin**, **não precisa de CORS**. O default é vazio (nenhuma origem
   cross-origin liberada). Só se for hospedar a UI em outra origem, libere explicitamente:
   `LOGLIFE_CORS_ORIGINS=http://192.168.0.10:8080` — evite `*` (API sem auth e com escrita).

Telas: registrar alimento/refeição, ver resumo do dia, ver lista do dia, excluir item.

> Se a PWA for hospedada em outra origem, ela usa caminhos relativos por padrão; para apontar
> para um backend específico: no console do navegador
> `localStorage.setItem('loglife.apiBase','http://192.168.0.10:8080')`.

---

## Próximos passos

- **Plugar o agente real**: implementar o endpoint `/estimate-calories` (ou um modelo Ollama
  afinado) e medir confiança real por item.
- Edição de registro (hoje é criar/listar/excluir).
- Autenticação (multiusuário) — o domínio já está pronto para isolar por usuário.
- App nativo iOS (Swift) — só depois, se necessário; a PWA cobre o MVP no iPhone.
