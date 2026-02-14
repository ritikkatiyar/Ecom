package com.ecom.notification.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotificationTemplateService {

    private final ObjectMapper objectMapper;
    private final Resource templatesResource;

    public NotificationTemplateService(
            ObjectMapper objectMapper,
            @Value("${app.notification.templates-resource:classpath:notification-templates.json}") Resource templatesResource) {
        this.objectMapper = objectMapper;
        this.templatesResource = templatesResource;
    }

    public String renderSubject(String eventType, Map<String, Object> payload) {
        return render(eventType, payload, "subject");
    }

    public String renderBody(String eventType, Map<String, Object> payload) {
        return render(eventType, payload, "body");
    }

    private String render(String eventType, Map<String, Object> payload, String field) {
        Map<String, Map<String, String>> templates = readTemplates();
        Map<String, String> template = templates.getOrDefault(eventType, templates.get("default"));
        if (template == null || template.get(field) == null) {
            return "";
        }
        return apply(template.get(field), payload);
    }

    private String apply(String raw, Map<String, Object> payload) {
        if (raw == null) {
            return "";
        }
        String value = raw;
        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String token = "{{" + entry.getKey() + "}}";
                String replacement = entry.getValue() == null ? "" : entry.getValue().toString();
                value = value.replace(token, replacement);
            }
        }
        return value.replace("{{orderId}}", payload == null ? "N/A" : String.valueOf(payload.getOrDefault("orderId", "N/A")))
                .replace("{{reason}}", payload == null ? "Unknown reason" : String.valueOf(payload.getOrDefault("reason", "Unknown reason")));
    }

    private Map<String, Map<String, String>> readTemplates() {
        try (var in = templatesResource.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<Map<String, Map<String, String>>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Could not load notification templates", ex);
        }
    }
}
