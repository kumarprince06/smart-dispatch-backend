package com.smartdispatch.config.service;

import com.smartdispatch.config.entity.PlatformConfig;
import com.smartdispatch.config.repository.PlatformConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformConfigService {

    private final PlatformConfigRepository configRepository;
    private static final String CONFIG_KEY = "GLOBAL_SETTINGS";

    // Caching temporarily disabled to prevent spring-boot-devtools RestartClassLoader cast exception
    // @Cacheable(value = "platformConfig", key = "'GLOBAL_SETTINGS'")
    public PlatformConfig getConfig() {
        return configRepository.findByConfigKey(CONFIG_KEY).orElseGet(this::createDefaultConfig);
    }

    private PlatformConfig createDefaultConfig() {
        log.info("Creating default global platform configuration");
        PlatformConfig config = PlatformConfig.builder()
                .configKey(CONFIG_KEY)
                .platformFee(5.0)
                .taxRate(18.0)
                .baseFare(30.0) // previously hardcoded
                .perKmRate(12.0) // previously hardcoded
                .surgeEnabled(true)
                .autoAssign(true)
                .build();
        return configRepository.save(config);
    }

    @Transactional
    // @CacheEvict(value = "platformConfig", key = "'GLOBAL_SETTINGS'")
    public PlatformConfig updateConfig(PlatformConfig newConfig) {
        PlatformConfig existing = getConfig();
        existing.setPlatformFee(newConfig.getPlatformFee());
        existing.setTaxRate(newConfig.getTaxRate());
        existing.setBaseFare(newConfig.getBaseFare());
        existing.setPerKmRate(newConfig.getPerKmRate());
        existing.setSurgeEnabled(newConfig.getSurgeEnabled());
        existing.setAutoAssign(newConfig.getAutoAssign());
        return configRepository.save(existing);
    }
}
