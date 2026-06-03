package com.wms.util;

import com.wms.enums.AuditAction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogUtilTest {

    @Test
    void testFilterSensitiveFields_removesPasswordHash() {
        Map<String, Object> values = new HashMap<>();
        values.put("status", "ACTIVE");
        values.put("passwordHash", "bcrypt_hash_value");
        values.put("password_hash", "another_hash");
        values.put("password", "plaintext");

        Map<String, Object> filtered =
                AuditLogUtil.filterSensitiveFields(values);

        assertFalse(filtered.containsKey("passwordHash"));
        assertFalse(filtered.containsKey("password_hash"));
        assertFalse(filtered.containsKey("password"));
        assertTrue(filtered.containsKey("status"));
    }

    @Test
    void testFilterSensitiveFields_keepsNonSensitiveFields() {
        Map<String, Object> values = new HashMap<>();
        values.put("fullName", "Nguyen Van A");
        values.put("role", "STOREKEEPER");
        values.put("email", "a@wms.com");

        Map<String, Object> filtered =
                AuditLogUtil.filterSensitiveFields(values);

        assertEquals(3, filtered.size());
        assertEquals("Nguyen Van A", filtered.get("fullName"));
    }

    @Test
    void testFilterSensitiveFields_handlesNullAndEmpty() {
        assertNull(AuditLogUtil.filterSensitiveFields(null));
        Map<String, Object> empty = Map.of();
        assertEquals(empty, AuditLogUtil.filterSensitiveFields(empty));
    }

    @Test
    void testBuildDiff_returnsOnlyChangedFields() {
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("status", "DRAFT");
        oldValues.put("quantity", 100);
        oldValues.put("note", "test");

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("status", "APPROVED");
        newValues.put("quantity", 100);
        newValues.put("note", "updated");

        Map<String, Object>[] diff =
                AuditLogUtil.buildDiff(oldValues, newValues);

        assertEquals("DRAFT", diff[0].get("status"));
        assertEquals("APPROVED", diff[1].get("status"));
        assertEquals("test", diff[0].get("note"));
        assertEquals("updated", diff[1].get("note"));
        assertFalse(diff[0].containsKey("quantity"),
                "Unchanged field should not appear in diff");
    }

    @Test
    void testBuildDiff_returnsNullOldValueForCreate() {
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("status", "DRAFT");
        newValues.put("quantity", 50);

        Map<String, Object>[] diff =
                AuditLogUtil.buildDiff(null, newValues);

        assertNull(diff[0]);
        assertNotNull(diff[1]);
        assertEquals("DRAFT", diff[1].get("status"));
    }

    @Test
    void testBuildDiff_filtersSensitiveFieldsFromDiff() {
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("passwordHash", "old_hash");
        oldValues.put("status", "ACTIVE");

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("passwordHash", "new_hash");
        newValues.put("status", "INACTIVE");

        Map<String, Object>[] diff =
                AuditLogUtil.buildDiff(oldValues, newValues);

        assertFalse(diff[0].containsKey("passwordHash"));
        assertFalse(diff[1].containsKey("passwordHash"));
        assertTrue(diff[0].containsKey("status"));
    }

    @Test
    void testGenerateDescription_formatsCorrectly() {
        String desc = AuditLogUtil.generateDescription(
                AuditAction.APPROVE, "RECEIPT", "PN-2026-001");

        assertEquals("APPROVE RECEIPT PN-2026-001", desc);
    }

    @Test
    void testGenerateDescription_createAction() {
        String desc = AuditLogUtil.generateDescription(
                AuditAction.CREATE, "TRANSFER", "TC-2026-003");

        assertEquals("CREATE TRANSFER TC-2026-003", desc);
    }

    @Test
    void testToJson_serializesMapCorrectly() {
        Map<String, Object> values = Map.of("status", "APPROVED");
        String json = AuditLogUtil.toJson(values);

        assertNotNull(json);
        assertTrue(json.contains("APPROVED"));
    }

    @Test
    void testToJson_returnsNullForNullInput() {
        assertNull(AuditLogUtil.toJson(null));
    }

    @Test
    void testFromJson_deserializesCorrectly() {
        String json = "{\"status\":\"APPROVED\",\"quantity\":100}";
        Map<String, Object> result = AuditLogUtil.fromJson(json);

        assertNotNull(result);
        assertEquals("APPROVED", result.get("status"));
        assertEquals(100, result.get("quantity"));
    }

    @Test
    void testFromJson_returnsNullForNullInput() {
        assertNull(AuditLogUtil.fromJson(null));
        assertNull(AuditLogUtil.fromJson(""));
        assertNull(AuditLogUtil.fromJson("   "));
    }
}
