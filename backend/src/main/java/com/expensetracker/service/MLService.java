package com.expensetracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class MLService {

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Call Python ML service to predict expense category
     */
    public String predictCategory(String description) {
        try {
            String url = mlServiceUrl + "/predict/category";
            Map<String, String> body = new HashMap<>();
            body.put("description", description);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() != null) {
                return (String) response.getBody().get("category");
            }
        } catch (Exception e) {
            // Fallback: rule-based categorization
            return fallbackCategorize(description);
        }
        return "Other";
    }

    /**
     * Get expense prediction for next month from ML service
     */
    public Map<String, Object> getPrediction(Long userId) {
        try {
            String url = mlServiceUrl + "/predict/next-month/" + userId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Map.of("predicted_amount", 0.0, "message", "Not enough data for prediction");
        }
    }

    /**
     * Get anomaly detection results from ML service
     */
    public List<Map<String, Object>> detectAnomalies(Long userId) {
        try {
            String url = mlServiceUrl + "/detect/anomalies/" + userId;
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Extract receipt data via OCR from ML service
     */
    public Map<String, Object> extractReceiptData(String imageBase64) {
        try {
            String url = mlServiceUrl + "/ocr/receipt";
            Map<String, String> body = new HashMap<>();
            body.put("image", imageBase64);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Map.of("error", "OCR service unavailable");
        }
    }

    /**
     * Fallback rule-based category prediction
     */
    private String fallbackCategorize(String description) {
        String lower = description.toLowerCase();
        if (lower.matches(".*(pizza|burger|restaurant|food|cafe|coffee|lunch|dinner|breakfast|zomato|swiggy).*")) return "Food";
        if (lower.matches(".*(uber|ola|taxi|flight|hotel|travel|bus|train|petrol|fuel).*")) return "Travel";
        if (lower.matches(".*(amazon|flipkart|shopping|clothes|shirt|shoes|mall).*")) return "Shopping";
        if (lower.matches(".*(electricity|water|wifi|internet|broadband|phone|mobile|recharge).*")) return "Bills";
        if (lower.matches(".*(hospital|doctor|medicine|pharmacy|health|gym).*")) return "Health";
        if (lower.matches(".*(movie|netflix|spotify|game|entertainment|party).*")) return "Entertainment";
        if (lower.matches(".*(rent|house|apartment|maintenance).*")) return "Housing";
        return "Other";
    }
}