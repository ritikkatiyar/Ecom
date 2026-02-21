package com.ecom.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/frontend-flags")
public class FrontendFlagsController {

    @Value("${app.frontend-flags.beta-banner-enabled:true}")
    private boolean betaBannerEnabled;

    @Value("${app.frontend-flags.admin-console-enabled:false}")
    private boolean adminConsoleEnabled;

    @GetMapping
    public Map<String, Object> getFlags() {
        return Map.of(
                "betaBannerEnabled", betaBannerEnabled,
                "adminConsoleEnabled", adminConsoleEnabled
        );
    }
}
