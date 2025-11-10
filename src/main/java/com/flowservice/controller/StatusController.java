package com.flowservice.controller;

import com.flowservice.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/status")
public class StatusController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "FlowService");
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("version", "1.0.0");

        return ApiResponse.success("服务运行正常", status);
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "FlowService");
        info.put("description", "图片处理服务，集成阿里通义千问模型");
        info.put("version", "1.0.0");
        info.put("endpoints", new String[]{
                "POST /api/image/upload - 上传图片并处理",
                "POST /api/image/process - 处理Base64图片",
                "GET /api/status/health - 健康检查",
                "GET /api/status/info - 服务信息"
        });

        return ApiResponse.success("服务信息", info);
    }
}