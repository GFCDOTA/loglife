package com.loglife.nutrition.infrastructure.estimation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for nutrition estimation, bound from {@code loglife.nutrition.estimation.*}.
 */
@ConfigurationProperties(prefix = "loglife.nutrition.estimation")
public class EstimationProperties {

    /** Primary estimator: {@code mock} | {@code local-agent} | {@code ollama}. */
    private String provider = "mock";

    private LocalAgent localAgent = new LocalAgent();

    private Ollama ollama = new Ollama();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalAgent getLocalAgent() {
        return localAgent;
    }

    public void setLocalAgent(LocalAgent localAgent) {
        this.localAgent = localAgent;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    /** Settings for the custom local HTTP agent ({@code POST /estimate-calories}). */
    public static class LocalAgent {
        private String baseUrl = "http://localhost:8787";
        private Duration timeout = Duration.ofSeconds(5);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    /** Settings for a local Ollama LLM. */
    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.1:8b";
        /** Read timeout: generous because a cold model load can take tens of seconds. */
        private Duration timeout = Duration.ofSeconds(120);
        /** Connect timeout: short — if Ollama is not listening we want to fall back fast. */
        private Duration connectTimeout = Duration.ofSeconds(5);
        /** How long Ollama keeps the model loaded after a call, avoiding repeated cold starts. */
        private String keepAlive = "30m";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public String getKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(String keepAlive) {
            this.keepAlive = keepAlive;
        }
    }
}
