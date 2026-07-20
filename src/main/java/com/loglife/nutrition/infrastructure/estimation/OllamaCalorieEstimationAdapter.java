package com.loglife.nutrition.infrastructure.estimation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.NutritionEstimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * A real local-agent estimator backed by an Ollama LLM running on the user's machine
 * ({@code POST /api/chat}). The response is constrained with Ollama's <em>structured outputs</em>
 * (a JSON Schema in the {@code format} field), so the model must return exactly the
 * {@link AgentDtos.EstimateResponse} shape, which {@link EstimateResponseMapper} maps to the domain.
 *
 * <p>Robustness: a short connect timeout (fail over to the mock fast if Ollama is down) with a long
 * read timeout (a cold model load takes tens of seconds), {@code keep_alive} so the model stays
 * warm between calls, and one retry if the model returns unparseable content.
 *
 * <p>Estimates are explicitly approximate (source {@link EstimationSource#OLLAMA}); the prompt
 * forbids medical advice. The overall confidence is derived from the per-item confidences (it is
 * <em>not</em> faked) when the model omits it.
 *
 * <p>Requires Ollama with structured-output support (v0.5+).
 */
public class OllamaCalorieEstimationAdapter implements CalorieEstimationPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaCalorieEstimationAdapter.class);

    private static final int MAX_ATTEMPTS = 2;

    private static final String SYSTEM_PROMPT = """
            Você é um assistente de estimativa nutricional do app LogLife.
            A partir de uma descrição livre de alimentos (geralmente em pt-BR), estime os valores
            nutricionais APROXIMADOS, item por item.
            Regras:
            - Separe a descrição em alimentos distintos: "bife + ovo + pão" são 3 itens.
            - Estime a porção provável a partir da descrição ("2 bifes médios" ≈ 2 × 120 g;
              "200g de arroz" = 200 g) e calcule calorias e macros para essa porção.
            - calories/proteinGrams/carbsGrams/fatGrams são por item, para a porção estimada.
            - confidence é um número entre 0 e 1; na dúvida sobre a porção ou o alimento, use
              confidence baixa. NÃO invente precisão; os valores são estimativas, não fatos.
            - Não dê conselho médico nem metas calóricas.
            - 'explanation' resume em uma frase como você estimou.
            Responda apenas os dados (o formato é validado automaticamente).""";

    /** JSON Schema passed to Ollama's {@code format} so the output always matches our DTO. */
    private static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "items": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string"},
                      "quantity": {"type": ["number", "null"]},
                      "unit": {"type": ["string", "null"]},
                      "calories": {"type": "number"},
                      "proteinGrams": {"type": "number"},
                      "carbsGrams": {"type": "number"},
                      "fatGrams": {"type": "number"},
                      "confidence": {"type": "number"}
                    },
                    "required": ["name", "calories", "proteinGrams", "carbsGrams", "fatGrams", "confidence"]
                  }
                },
                "total": {
                  "type": "object",
                  "properties": {
                    "calories": {"type": "number"},
                    "proteinGrams": {"type": "number"},
                    "carbsGrams": {"type": "number"},
                    "fatGrams": {"type": "number"}
                  }
                },
                "confidence": {"type": "number"},
                "explanation": {"type": "string"}
              },
              "required": ["items", "explanation"]
            }""";

    private final RestClient restClient;
    private final String model;
    private final String keepAlive;
    private final ObjectMapper objectMapper;
    private final Object responseFormat;

    public OllamaCalorieEstimationAdapter(RestClient restClient, String model, String keepAlive,
                                          ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.model = model;
        this.keepAlive = keepAlive;
        this.objectMapper = objectMapper;
        this.responseFormat = objectMapper.readValue(RESPONSE_SCHEMA, Object.class);
    }

    @Override
    public EstimationResult estimate(FoodDescription description) {
        OllamaChatRequest request = new OllamaChatRequest(
                model,
                false,
                responseFormat,
                keepAlive,
                List.of(
                        new OllamaMessage("system", SYSTEM_PROMPT),
                        new OllamaMessage("user", buildUserPrompt(description))),
                new OllamaOptions(0.2));

        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                OllamaChatResponse response = restClient.post()
                        .uri("/api/chat")
                        .body(request)
                        .retrieve()
                        .body(OllamaChatResponse.class);

                if (response == null || response.message() == null || response.message().content() == null) {
                    return EstimationResult.failure("ollama returned no message content");
                }

                AgentDtos.EstimateResponse parsed =
                        objectMapper.readValue(response.message().content(), AgentDtos.EstimateResponse.class);

                // Confidence is derived from the items by EstimateResponseMapper when the model
                // omits an overall value — never injected as a fixed placeholder.
                NutritionEstimate estimate = EstimateResponseMapper.toDomain(parsed, EstimationSource.OLLAMA);
                if (estimate == null) {
                    return EstimationResult.failure("ollama returned an empty estimate");
                }
                log.debug("Ollama produced estimate: items={} calories={} (attempt {})",
                        estimate.items().size(), estimate.nutrition().calories(), attempt);
                return EstimationResult.success(estimate);
            } catch (RestClientException ex) {
                // Transport/HTTP failure: Ollama is down, unreachable or broken. Retrying cannot
                // help and can hang for another full read timeout (120s), so fail immediately.
                log.warn("Ollama transport failure, not retrying: {}", ex.toString());
                return EstimationResult.failure("ollama transport error: " + ex.getMessage());
            } catch (Exception ex) {
                // Unparseable/invalid model output: one more attempt is cheap and often enough.
                last = ex;
                log.warn("Ollama estimation attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, ex.toString());
            }
        }
        return EstimationResult.failure(
                "ollama error after " + MAX_ATTEMPTS + " attempts: " + (last == null ? "unknown" : last.getMessage()));
    }

    private static String buildUserPrompt(FoodDescription description) {
        StringBuilder sb = new StringBuilder();
        sb.append("Descrição: ").append(description.rawText());
        if (description.quantity() != null && description.quantity().isPresent()) {
            sb.append("\nQuantidade informada: ");
            if (description.quantity().amount() != null) {
                sb.append(description.quantity().amount());
            }
            if (description.quantity().unit() != null) {
                sb.append(' ').append(description.quantity().unit());
            }
        }
        sb.append("\nIdioma: ").append(description.language());
        sb.append("\nData: ").append(description.date());
        return sb.toString();
    }

    record OllamaMessage(String role, String content) {
    }

    record OllamaOptions(double temperature) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OllamaChatRequest(String model, boolean stream, Object format,
                             @JsonProperty("keep_alive") String keepAlive,
                             List<OllamaMessage> messages, OllamaOptions options) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaChatResponse(OllamaMessage message) {
    }
}
