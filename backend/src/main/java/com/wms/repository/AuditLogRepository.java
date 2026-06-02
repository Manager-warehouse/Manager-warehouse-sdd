package com.wms.repository;

import com.wms.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * First page query (no cursor) with all optional filters.
     * Filters are applied only when the parameter is non-null.
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :start AND :end " +
           "AND (:actorId IS NULL OR a.actor.id = :actorId) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR a.action = " +
           "com.wms.enums.AuditAction.valueOf(:action)) " +
           "AND (:warehouseId IS NULL OR a.warehouse.id = :warehouseId) " +
           "ORDER BY a.id DESC")
    List<AuditLog> findByFilters(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("actorId") Long actorId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);

    /**
     * Cursor-based next page query with all optional filters.
     * Returns records with id < cursor (older entries).
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.id < :cursor " +
           "AND a.timestamp BETWEEN :start AND :end " +
           "AND (:actorId IS NULL OR a.actor.id = :actorId) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR a.action = " +
           "com.wms.enums.AuditAction.valueOf(:action)) " +
           "AND (:warehouseId IS NULL OR a.warehouse.id = :warehouseId) " +
           "ORDER BY a.id DESC")
    List<AuditLog> findByCursorAndFilters(
            @Param("cursor") Long cursor,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("actorId") Long actorId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);

    /**
     * RBAC query: first page filtered by warehouse IDs
     * (for WAREHOUSE_MANAGER who can only see assigned warehouses).
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :start AND :end " +
           "AND a.warehouse.id IN :warehouseIds " +
           "AND (:actorId IS NULL OR a.actor.id = :actorId) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR a.action = " +
           "com.wms.enums.AuditAction.valueOf(:action)) " +
           "ORDER BY a.id DESC")
    List<AuditLog> findByFiltersAndWarehouseIds(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("actorId") Long actorId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("warehouseIds") List<Long> warehouseIds,
            Pageable pageable);

    /**
     * RBAC query: cursor-based next page filtered by warehouse IDs.
     */
    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.id < :cursor " +
           "AND a.timestamp BETWEEN :start AND :end " +
           "AND a.warehouse.id IN :warehouseIds " +
           "AND (:actorId IS NULL OR a.actor.id = :actorId) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR a.action = " +
           "com.wms.enums.AuditAction.valueOf(:action)) " +
           "ORDER BY a.id DESC")
    List<AuditLog> findByCursorAndFiltersAndWarehouseIds(
            @Param("cursor") Long cursor,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("actorId") Long actorId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("warehouseIds") List<Long> warehouseIds,
            Pageable pageable);
}
