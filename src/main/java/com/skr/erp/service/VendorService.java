package com.skr.erp.service;

import com.skr.erp.dto.request.CreateVendorRequest;
import com.skr.erp.dto.request.UpdateVendorRequest;
import com.skr.erp.dto.response.VendorDetailsResponse;
import com.skr.erp.dto.response.VendorResponse;

import java.util.List;
import java.util.UUID;

public interface VendorService {

    VendorResponse create(CreateVendorRequest request);

    VendorResponse update(
            UUID id,
            UpdateVendorRequest request
    );

    List<VendorResponse> search(
            String name,
            String mobile,
            Boolean active
    );

    VendorDetailsResponse getById(UUID id);

    VendorResponse toggleStatus(UUID id);

}