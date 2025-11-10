package com.flowservice.controller;

import com.flowservice.model.ApiResponse;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProcessResult> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", defaultValue = "请描述这张图片") String prompt,
            @RequestParam(value = "temperature", defaultValue = "0.7") Double temperature,
            @RequestParam(value = "maxTokens", defaultValue = "1000") Integer maxTokens) {

        try {
            log.info("接收到图片上传请求: fileName={}, size={}, contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            if (file.isEmpty()) {
                return ApiResponse.error(400, "上传文件不能为空");
            }

            if (!isImageFile(file)) {
                return ApiResponse.error(400, "仅支持图片格式文件");
            }

            byte[] imageBytes = file.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            ProcessRequest request = new ProcessRequest();
            request.setPrompt(prompt);
            request.setImageBase64(imageBase64);
            request.setFileName(file.getOriginalFilename());
            request.setMimeType(file.getContentType());

            ProcessRequest.ProcessOptions options = new ProcessRequest.ProcessOptions();
            options.setTemperature(temperature);
            options.setMaxTokens(maxTokens);
            request.setOptions(options);

            ProcessResult result = imageProcessService.processImage(request);

            log.info("图片处理完成: taskId={}", result.getTaskId());
            return ApiResponse.success("图片处理成功", result);

        } catch (Exception e) {
            log.error("图片处理失败", e);
            return ApiResponse.error("图片处理失败: " + e.getMessage());
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