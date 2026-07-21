package com.wms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wms.enums.AuditAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility for audit log operations: sensitive field filtering,
 * diff building, and description generation.
 */
public final class AuditLogUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final ObjectMapper registeredMAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "passwordHash", "password_hash", "password",
            "refreshToken", "accessToken", "token");

    private AuditLogUtil() {
    }

    public static Map<String, Object> filterSensitiveFields(
            Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        Map<String, Object> filtered = new HashMap<>(values);
        SENSITIVE_FIELDS.forEach(field -> {
            if (filtered.containsKey(field)) {
                filtered.put(field, null);
            }
        });
        return filtered;
    }

    /**
     * Compares two field maps and returns only the fields that differ.
     *
     * @return two-element array: [0] = old changed fields, [1] = new changed
     *         fields.
     *         For CREATE (oldValues is null), returns [null, newValues].
     */
    public static Map<String, Object>[] buildDiff(
            Map<String, Object> oldValues,
            Map<String, Object> newValues) {
        @SuppressWarnings("unchecked")
        Map<String, Object>[] result = new Map[2];

        if (oldValues == null) {
            result[0] = null;
            result[1] = filterSensitiveFields(newValues);
            return result;
        }

        Map<String, Object> oldDiff = new HashMap<>();
        Map<String, Object> newDiff = new HashMap<>();

        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            String key = entry.getKey();
            Object newVal = entry.getValue();
            Object oldVal = oldValues.get(key);

            if (!java.util.Objects.equals(oldVal, newVal)) {
                oldDiff.put(key, oldVal);
                newDiff.put(key, newVal);
            }
        }

        result[0] = filterSensitiveFields(oldDiff);
        result[1] = filterSensitiveFields(newDiff);
        return result;
    }

    /**
     * Auto-generates the audit log description.
     * Format: "{ACTION} {ENTITY_TYPE} {ENTITY_CODE}"
     */
    public static String generateDescription(
            AuditAction action,
            String entityType,
            String entityCode) {
        StringBuilder description = new StringBuilder(action.name());
        if (entityType != null && !entityType.isBlank()) {
            description.append(" ").append(entityType);
        }
        if (entityCode != null && !entityCode.isBlank()) {
            description.append(" ").append(entityCode);
        }
        return description.toString();
    }

    /**
     * Serializes a Map to JSON string for JSONB storage.
     * Returns null if the input is null.
     */
    public static String toJson(Map<String, Object> values) {
        if (values == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize audit log values", e);
        }
    }

    /**
     * Deserializes a JSON string to Map for API response.
     * Returns null if the input is null or empty.
     */
    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json,
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize audit log values", e);
        }
    }
}
