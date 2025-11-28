package com.flowservice.controller;

import com.flowservice.model.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class RootController {

    @GetMapping
    public ApiResponse<Map<String, Object>> welcome() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "FlowService");
        data.put("version", "1.0.0");
        data.put("description", "图片处理服务，集成阿里通义千问模型");
        data.put("endpoints", Map.of(
                "图片上传", "POST /api/image/upload",
                "健康检查", "GET /api/status/health",
                "服务信息", "GET /api/status/info"
        ));

        return ApiResponse.success("欢迎使用 FlowService", data);
    }
}
