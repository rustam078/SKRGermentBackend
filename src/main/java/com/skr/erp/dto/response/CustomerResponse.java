package com.skr.erp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private UUID id;

    private String name;

    private String mobile;

    private String email;

    /**
     * true -> existing customer
     * false -> customer not found
     */
    private boolean existing;
}