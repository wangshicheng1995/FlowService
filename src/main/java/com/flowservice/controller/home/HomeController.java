package com.flowservice.controller.home;

import com.flowservice.model.ApiResponse;
import com.flowservice.model.CalorieStatisticsResponse;
import com.flowservice.service.HomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 首页控制器
 * 提供首页相关的 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 获取用户在指定时间范围内的食物总热量
     * 默认获取当天的食物总热量
     *
     * @param userId    用户 ID（必填）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd，默认当天）
     * @param endDate   结束日期（可选，格式：yyyy-MM-dd，默认当天）
     * @return 热量统计结果
     */
    @GetMapping("/calories")
    public ApiResponse<CalorieStatisticsResponse> getTotalCalories(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            log.info("接收到热量统计请求: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

            CalorieStatisticsResponse result = homeService.getTotalCalories(userId, startDate, endDate);

            return ApiResponse.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询热量统计失败: userId={}", userId, e);
            return ApiResponse.error("查询热量统计失败: " + e.getMessage());
        }
    }
}
