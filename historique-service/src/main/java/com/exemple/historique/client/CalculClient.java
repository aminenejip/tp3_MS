package com.exemple.historique.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CalculClient {
    private final RestClient restClient;

    public CalculClient(@Value("${calcul.service.url}") String calculServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(calculServiceUrl)
                .build();
    }

    public double calculer(String operateur, int a, int b) {
        String corps = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/calcul/{operateur}")
                        .queryParam("a", a)
                        .queryParam("b", b)
                        .build(operateur))
                .retrieve()
                .body(String.class);
        if (corps == null || corps.isBlank()) {
            throw new IllegalStateException("Réponse vide du calcul-service");
        }
        return Double.parseDouble(corps.trim());
    }
}
