package com.skr.erp.service;

import com.skr.erp.dto.response.SystemSettingResponse;
import com.skr.erp.entity.SystemSetting;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingServiceImpl
        implements SystemSettingService {

    private final SystemSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponse get(String key) {

        SystemSetting setting = repository.findBySettingKey(key)
                .orElseThrow(() ->
                        new BusinessException("Setting not found."));

        return SystemSettingResponse.builder()
                .key(setting.getSettingKey())
                .value(setting.getSettingValue())
                .description(setting.getDescription())
                .build();
    }

    @Override
    public SystemSettingResponse update(String key, String value) {

        SystemSetting setting = repository.findBySettingKey(key)
                .orElseThrow(() ->
                        new BusinessException("Setting not found."));

        setting.setSettingValue(value);

        repository.save(setting);

        return SystemSettingResponse.builder()
                .key(setting.getSettingKey())
                .value(setting.getSettingValue())
                .description(setting.getDescription())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getLowStockThreshold() {

        return repository.findBySettingKey("LOW_STOCK_THRESHOLD")
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(50);
    }
}