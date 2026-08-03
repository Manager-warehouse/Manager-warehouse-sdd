package com.wms.dto.request;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateReceiptRequest {

    @NotNull
    @JsonProperty("supplierId")
    @JsonAlias("supplier_id")
    private Long supplierId;

    @Null
    private String type;

    @NotNull
    @JsonProperty("warehouseId")
    @JsonAlias("warehouse_id")
    private Long warehouseId;

    @NotNull
    @JsonProperty("documentDate")
    @JsonAlias("document_date")
    private LocalDate documentDate;

    @Valid
    @NotEmpty
    private List<CreateReceiptItemRequest> items;

    private String notes;

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public List<CreateReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateReceiptItemRequest> items) {
        this.items = items;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
