package com.flowservice.controller;

import com.flowservice.config.PromptConfig;
import com.flowservice.model.ApiResponse;
import com.flowservice.model.FoodAnalysisResponse;
import com.flowservice.model.ProcessRequest;
import com.flowservice.model.ProcessResult;
import com.flowservice.service.ImageProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageProcessService imageProcessService;

    /**
     * 上传食物图片进行营养分析
     * 使用通义千问 AI 识别食物并评估营养
     *
     * @param file 上传的图片文件
     * @return 食物分析结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FoodAnalysisResponse> uploadImage(@RequestParam("file") MultipartFile file) {

        try {
            log.info("接收到食物图片上传请求: fileName={}, size={}, contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            if (file.isEmpty()) {
                return ApiResponse.error(400, "上传文件不能为空");
            }

            if (!isImageFile(file)) {
                return ApiResponse.error(400, "仅支持图片格式文件");
            }

            byte[] imageBytes = file.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            // 使用食物营养分析 Prompt
            ProcessRequest request = new ProcessRequest();
            request.setPrompt(PromptConfig.FOOD_NUTRITION_ANALYSIS_PROMPT);
            request.setImageBase64(imageBase64);
            request.setFileName(file.getOriginalFilename());
            request.setMimeType(file.getContentType());

            // 处理食物图片并解析 AI 返回的 JSON
            FoodAnalysisResponse result = imageProcessService.processFoodAnalysis(request);

            log.info("食物图片分析完成: foodItems={}, confidence={}, isBalanced={}",
                    result.getFoodItems(), result.getConfidence(), result.getIsBalanced());
            return ApiResponse.success("食物分析成功", result);

        } catch (Exception e) {
            log.error("食物图片分析失败", e);
            return ApiResponse.error("食物图片分析失败: " + e.getMessage());
        }
    }

    @PostMapping("/process")
    public ApiResponse<ProcessResult> processImageWithBase64(@RequestBody ProcessRequest request) {
        try {
            log.info("接收到Base64图片处理请求");

            if (request.getImageBase64() == null || request.getImageBase64().isEmpty()) {
                return ApiResponse.error(400, "图片数据不能为空");
            }

            ProcessResult result = imageProcessService.processImage(request);

            log.info("图片处理完成: taskId={}", result.getTaskId());
            return ApiResponse.success("图片处理成功", result);

        } catch (Exception e) {
            log.error("图片处理失败", e);
            return ApiResponse.error("图片处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务运行正常");
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
}