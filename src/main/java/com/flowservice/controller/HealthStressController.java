package com.flowservice.controller;

import com.flowservice.model.HealthStressScoreResponse;
import com.flowservice.service.HealthStressService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class HealthStressController {

    private final HealthStressService healthStressService;

    @GetMapping("/health/stress-score")
    public HealthStressScoreResponse getHealthStressScore(
            // @AuthenticationPrincipal UserAuth auth, // 暂时注释掉，因为 UserAuth 类可能不存在，用 mock ID
            // 代替
            @RequestParam(required = false) Long userId, // 临时参数，用于测试
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        // 临时逻辑：如果没传 userId，使用默认 ID (例如 1L)
        Long targetUserId = (userId != null) ? userId : 1L;

        int score = healthStressService.calculateDailyScore(targetUserId, targetDate);
        return new HealthStressScoreResponse(String.valueOf(targetUserId), score);
    }
}
