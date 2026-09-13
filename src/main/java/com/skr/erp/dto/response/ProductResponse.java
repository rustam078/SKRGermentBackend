package com.skr.erp.dto.response;

import com.skr.erp.common.constants.ProductSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
public class ProductResponse {

    private UUID id;

    private String name;
    private ProductSource source;

    private String iconName;

    private String description;

    private Boolean active;

    private Integer totalPieceCodes;

    private Integer activePieceCodes;

    private Integer inactivePieceCodes;

    // Current effective selling price (null when none configured).
    private BigDecimal sellingPrice;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}