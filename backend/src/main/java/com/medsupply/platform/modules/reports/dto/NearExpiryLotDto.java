package com.medsupply.platform.modules.reports.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class NearExpiryLotDto {
    private String batchId;
    private String productName;
    private String batchNumber;
    private LocalDate expiryDate;
    private long daysToExpiry;
    private int quantityOnHand;
}
