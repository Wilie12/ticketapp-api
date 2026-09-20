package com.nn.ticketapp_api.shared.security.ratelimit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-rate-limit")
public class RateLimitTestController {

    @GetMapping
    public String ping() {
        return "OK";
    }
}
