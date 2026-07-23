package com.medsupply.platform.modules.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    @NotBlank(message = "Batch/Lot number is required")
    private String batchNumber;

    @NotNull(message = "Manufacturing date is required")
    private LocalDate manufacturingDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.00", message = "MRP cannot be negative")
    private BigDecimal mrp;

    @NotNull(message = "B2B price is required")
    @DecimalMin(value = "0.00", message = "B2B price cannot be negative")
    private BigDecimal b2bPrice;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantityOnHand;

    private boolean coldChainMonitored;

    private BigDecimal tempReadingCelsius;

    @Builder.Default
    private String status = "ACTIVE";
}
