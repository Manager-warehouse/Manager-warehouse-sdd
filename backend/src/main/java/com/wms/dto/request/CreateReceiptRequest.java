package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wms.enums.ReceiptSourceChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateReceiptRequest {

    @NotNull
    @JsonProperty("supplier_id")
    private Long supplierId;

    @Null
    private String type;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("contact_person")
    private String contactPerson;

    @NotNull
    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @NotBlank
    @Size(max = 100)
    @JsonProperty("source_reference")
    private String sourceReference;

    @NotNull
    @JsonProperty("source_channel")
    private ReceiptSourceChannel sourceChannel;

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

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public ReceiptSourceChannel getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(ReceiptSourceChannel sourceChannel) {
        this.sourceChannel = sourceChannel;
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
