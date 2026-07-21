package com.wms.util;

import com.wms.enums.AuditAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class AuditLogUtilTest {

    // ─── filterSensitiveFields ────────────────────────────────────────────────

    @Test
    @DisplayName("Xóa passwordHash khỏi map")
    void filterSensitiveFields_keepsPasswordHashWithNullValue() {
        Map<String, Object> input = new HashMap<>();
        input.put("passwordHash", "secret");
        input.put("email", "user@wms.com");

        Map<String, Object> result = AuditLogUtil.filterSensitiveFields(input);

        assertThat(result).containsEntry("passwordHash", null);
        assertThat(result).containsEntry("email", "user@wms.com");
    }

    @Test
    @DisplayName("Xóa tất cả sensitive fields: password, accessToken, refreshToken, token")
    void filterSensitiveFields_omitsAllSensitiveValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("password", "pass");
        input.put("password_hash", "hash");
        input.put("accessToken", "jwt");
        input.put("refreshToken", "rf");
        input.put("token", "tk");
        input.put("fullName", "Nguyen Van A");

        Map<String, Object> result = AuditLogUtil.filterSensitiveFields(input);

        assertThat(result)
                .containsEntry("password", null)
                .containsEntry("password_hash", null)
                .containsEntry("accessToken", null)
                .containsEntry("refreshToken", null)
                .containsEntry("token", null);
        assertThat(result).containsEntry("fullName", "Nguyen Van A");
    }

    @Test
    @DisplayName("Map null → trả về null")
    void filterSensitiveFields_nullInput_returnsNull() {
        assertThat(AuditLogUtil.filterSensitiveFields(null)).isNull();
    }

    @Test
    @DisplayName("Map rỗng → trả về map rỗng")
    void filterSensitiveFields_emptyMap_returnsEmpty() {
        Map<String, Object> result = AuditLogUtil.filterSensitiveFields(new HashMap<>());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Map không có sensitive field → trả về nguyên vẹn")
    void filterSensitiveFields_noSensitiveFields_returnsUnchanged() {
        Map<String, Object> input = new HashMap<>();
        input.put("role", "ADMIN");
        input.put("isActive", true);

        Map<String, Object> result = AuditLogUtil.filterSensitiveFields(input);

        assertThat(result).containsEntry("role", "ADMIN").containsEntry("isActive", true);
    }

    // ─── buildDiff ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildDiff khi oldValues null → old = null, new = newValues (CREATE action)")
    void buildDiff_oldNull_returnsFullNewValues() {
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("name", "Product A");
        newValues.put("price", 100);

        Map<String, Object>[] result = AuditLogUtil.buildDiff(null, newValues);

        assertThat(result[0]).isNull();
        assertThat(result[1]).containsEntry("name", "Product A");
    }

    @Test
    @DisplayName("buildDiff chỉ trả về các field đã thay đổi")
    void buildDiff_returnsOnlyChangedFields() {
        Map<String, Object> old = new HashMap<>();
        old.put("name", "Old Name");
        old.put("isActive", true);
        old.put("email", "a@wms.com");

        Map<String, Object> updated = new HashMap<>();
        updated.put("name", "New Name");
        updated.put("isActive", true);
        updated.put("email", "a@wms.com");

        Map<String, Object>[] result = AuditLogUtil.buildDiff(old, updated);

        assertThat(result[0]).containsOnlyKeys("name").containsEntry("name", "Old Name");
        assertThat(result[1]).containsOnlyKeys("name").containsEntry("name", "New Name");
    }

    @Test
    @DisplayName("buildDiff loại bỏ sensitive fields khỏi kết quả diff")
    void buildDiff_keepsSensitiveFieldNamesAndOmitsValues() {
        Map<String, Object> old = new HashMap<>();
        old.put("passwordHash", "old-hash");
        old.put("email", "a@wms.com");

        Map<String, Object> updated = new HashMap<>();
        updated.put("passwordHash", "new-hash");
        updated.put("email", "b@wms.com");

        Map<String, Object>[] result = AuditLogUtil.buildDiff(old, updated);

        assertThat(result[0]).containsEntry("passwordHash", null).containsEntry("email", "a@wms.com");
        assertThat(result[1]).containsEntry("passwordHash", null).containsEntry("email", "b@wms.com");
    }

    @Test
    @DisplayName("buildDiff khi không có thay đổi → trả về map rỗng cho cả 2")
    void buildDiff_noChanges_returnsEmptyDiff() {
        Map<String, Object> same = new HashMap<>();
        same.put("name", "Same");
        same.put("status", "ACTIVE");

        Map<String, Object>[] result = AuditLogUtil.buildDiff(same, new HashMap<>(same));

        assertThat(result[0]).isEmpty();
        assertThat(result[1]).isEmpty();
    }

    // ─── toJson / fromJson ────────────────────────────────────────────────────

    @Test
    @DisplayName("toJson serialize Map thành JSON string đúng")
    void toJson_serializesMapCorrectly() {
        Map<String, Object> input = new HashMap<>();
        input.put("status", "ACTIVE");

        String json = AuditLogUtil.toJson(input);

        assertThat(json).contains("\"status\"").contains("ACTIVE");
    }

    @Test
    @DisplayName("toJson serialize được giá trị LocalDate và OffsetDateTime cho audit snapshot")
    void toJson_serializesJavaTimeValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("documentDate", LocalDate.of(2026, 6, 21));
        input.put("updatedAt", OffsetDateTime.parse("2026-06-21T12:19:14+07:00"));

        String json = AuditLogUtil.toJson(input);

        assertThat(json)
                .contains("\"documentDate\":\"2026-06-21\"")
                .contains("\"updatedAt\":\"2026-06-21T12:19:14+07:00\"");
    }

    @Test
    @DisplayName("toJson với null → trả về null")
    void toJson_nullInput_returnsNull() {
        assertThat(AuditLogUtil.toJson(null)).isNull();
    }

    @Test
    @DisplayName("fromJson deserialize JSON string về Map đúng")
    void fromJson_deserializesCorrectly() {
        String json = "{\"status\":\"APPROVED\",\"entityId\":5}";

        Map<String, Object> result = AuditLogUtil.fromJson(json);

        assertThat(result).containsEntry("status", "APPROVED");
        assertThat(result).containsKey("entityId");
    }

    @Test
    @DisplayName("fromJson với null → trả về null")
    void fromJson_nullInput_returnsNull() {
        assertThat(AuditLogUtil.fromJson(null)).isNull();
    }

    @Test
    @DisplayName("fromJson với blank string → trả về null")
    void fromJson_blankInput_returnsNull() {
        assertThat(AuditLogUtil.fromJson("   ")).isNull();
    }

    // ─── generateDescription ─────────────────────────────────────────────────

    @Test
    @DisplayName("generateDescription tạo đúng format ACTION ENTITY_TYPE ENTITY_CODE")
    void generateDescription_correctFormat() {
        String desc = AuditLogUtil.generateDescription(AuditAction.STATUS_CHANGE, "Receipt", "R001");
        assertThat(desc).isEqualTo("STATUS_CHANGE Receipt R001");
    }

    @Test
    @DisplayName("generateDescription cho LOGIN action")
    void generateDescription_loginAction() {
        String desc = AuditLogUtil.generateDescription(AuditAction.LOGIN, "User", "admin@wms.com");
        assertThat(desc).isEqualTo("LOGIN User admin@wms.com");
    }

    @Test
    @DisplayName("generateDescription cho CREATE action")
    void generateDescription_createAction() {
        String desc = AuditLogUtil.generateDescription(AuditAction.CREATE, "Product", "P001");
        assertThat(desc).isEqualTo("CREATE Product P001");
    }

    @Test
    @DisplayName("generateDescription cháº¥p nháº­n entity null")
    void generateDescription_nullEntity_returnsActionOnly() {
        String desc = AuditLogUtil.generateDescription(AuditAction.LOGIN, null, null);
        assertThat(desc).isEqualTo("LOGIN");
    }
}
