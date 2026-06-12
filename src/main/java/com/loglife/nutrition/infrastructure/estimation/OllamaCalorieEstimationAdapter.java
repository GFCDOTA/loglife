package com.loglife.nutrition.infrastructure.estimation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loglife.nutrition.application.port.out.CalorieEstimationPort;
import com.loglife.nutrition.application.port.out.EstimationResult;
import com.loglife.nutrition.domain.EstimationSource;
import com.loglife.nutrition.domain.FoodDescription;
import com.loglife.nutrition.domain.NutritionEstimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * A real local-agent estimator backed by an Ollama LLM running on the user's machine
 * ({@code POST /api/chat} with {@code format: json}). The model is asked to return the same
 * JSON shape as the custom HTTP agent, which is then mapped by {@link EstimateResponseMapper}.
 *
 * <p>Estimates are explicitly approximate (source {@link EstimationSource#OLLAMA}); the prompt
 * forbids medical advice and the confidence stays moderate so nothing is treated as fact.
 */
public class OllamaCalorieEstimationAdapter implements CalorieEstimationPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaCalorieEstimationAdapter.class);

    private static final double DEFAULT_CONFIDENCE = 0.5;

    private static final String SYSTEM_PROMPT = """
            Você é um assistente de estimativa nutricional do app LogLife.
            A partir de uma descrição livre de alimentos (geralmente em pt-BR), estime os valores
            nutricionais aproximados. Responda SOMENTE com um objeto JSON, sem texto fora do JSON,
            exatamente neste formato:
            {
              "items": [
                {"name": "string", "quantity": number, "unit": "string",
                 "calories": number, "proteinGrams": number, "carbsGrams": number,
                 "fatGrams": number, "confidence": number}
              ],
              "total": {"calories": number, "proteinGrams": number, "carbsGrams": number, "fatGrams": number},
              "confidence": number,
              "explanation": "string"
            }
            Regras: valores são estimativas aproximadas, nunca números exatos ou autoritativos.
            confidence é um número entre 0 e 1. Não dê conselho médico nem metas calóricas.
            Não invente precisão: na dúvida, use confidence baixa.""";

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public OllamaCalorieEstimationAdapter(RestClient restClient, String model, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    public EstimationResult estimate(FoodDescription description) {
        try {
            OllamaChatRequest request = new OllamaChatRequest(
                    model,
                    false,
                    "json",
                    List.of(
                            new OllamaMessage("system", SYSTEM_PROMPT),
                            new OllamaMessage("user", buildUserPrompt(description))),
                    new OllamaOptions(0.2));

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

            // LLMs often omit an overall confidence; default to a moderate value rather than 0.
            if (parsed.confidence() == null) {
                parsed = new AgentDtos.EstimateResponse(
                        parsed.items(), parsed.total(), parsed.source(), DEFAULT_CONFIDENCE, parsed.explanation());
            }

            NutritionEstimate estimate = EstimateResponseMapper.toDomain(parsed, EstimationSource.OLLAMA);
            if (estimate == null) {
                return EstimationResult.failure("ollama returned an empty estimate");
            }
            log.debug("Ollama produced estimate: items={} calories={}",
                    estimate.items().size(), estimate.nutrition().calories());
            return EstimationResult.success(estimate);
        } catch (Exception ex) {
            log.warn("Ollama estimation failed: {}", ex.toString());
            return EstimationResult.failure("ollama error: " + ex.getMessage());
        }
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

    record OllamaChatRequest(String model, boolean stream, String format,
                             List<OllamaMessage> messages, OllamaOptions options) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaChatResponse(OllamaMessage message) {
    }
}
