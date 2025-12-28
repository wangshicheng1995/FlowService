package com.flowservice.controller;

import com.flowservice.config.PromptConfig;
import com.flowservice.entity.MealRecord;
import com.flowservice.model.ApiResponse;
import com.flowservice.model.FoodAnalysisProcessResult;
import com.flowservice.model.FoodAnalysisResponse;
import com.flowservice.model.ProcessRequest;
import com.flowservice.model.UploadResponse;
import com.flowservice.service.AsyncTaskExecutorService;
import com.flowservice.service.ImageProcessService;
import com.flowservice.service.MealRecordService;
import com.flowservice.util.ImageCompressor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Record 页面控制器
 * 提供用餐记录相关的 API 接口（图片上传、记录管理）
 */
@Slf4j
@RestController
@RequestMapping("/record")
@RequiredArgsConstructor
@Tag(name = "用餐记录", description = "Record 页面数据接口，包括食物图片上传、用餐记录查询等")
public class RecordController {

    private final ImageProcessService imageProcessService;
    private final MealRecordService mealRecordService;
    private final AsyncTaskExecutorService asyncTaskExecutorService;

    /**
     * 上传食物图片进行营养分析（同步 + 异步模式）
     * 
     * 同步返回：食物识别和基础营养分析结果
     * 异步任务：血糖趋势预测、吃饭顺序建议等（通过 taskId 轮询获取）
     *
     * @param file   上传的图片文件
     * @param userId 用户 ID
     * @return 食物分析结果 + 异步任务 ID 映射
     */
    @Operation(summary = "上传食物图片", description = "上传食物图片，使用通义千问 AI 识别食物并分析营养成分。\n\n" +
            "**同步返回**：食物识别结果、热量和营养成分\n" +
            "**异步任务**：血糖趋势预测（glucoseTrend）、吃饭顺序建议（eatingOrder）\n\n" +
            "前端可通过 `/task/{taskId}` 接口轮询异步任务状态。\n\n" +
            "支持的图片格式: JPG, PNG, WEBP\n" +
            "最大文件大小: 10MB")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResponse> uploadImage(
            @Parameter(description = "食物图片文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "用户 ID（必填），支持 Apple ID 格式", required = true, example = "000514.xxx.1422") @RequestParam("userId") String userId) {

        try {
            log.info("接收到食物图片上传请求: fileName={}, size={}, contentType={}, userId={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType(), userId);

            if (file.isEmpty()) {
                return ApiResponse.error(400, "上传文件不能为空");
            }

            if (!isImageFile(file)) {
                return ApiResponse.error(400, "仅支持图片格式文件");
            }

            byte[] imageBytes = ImageCompressor.compress(file);
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            log.info("图片压缩后大小: {}KB, Base64长度: {}KB",
                    imageBytes.length / 1024, imageBase64.length() / 1024);

            // 使用食物营养分析 Prompt
            ProcessRequest request = new ProcessRequest();
            request.setPrompt(PromptConfig.FOOD_NUTRITION_ANALYSIS_PROMPT);
            request.setImageBase64(imageBase64);
            request.setFileName(file.getOriginalFilename());
            request.setMimeType(file.getContentType());

            // ===== 同步处理：食物图片分析 =====
            FoodAnalysisProcessResult processResult = imageProcessService.processFoodAnalysisWithRecordId(request,
                    userId);
            FoodAnalysisResponse analysisResult = processResult.getAnalysisResponse();
            Long mealRecordId = processResult.getMealRecordId();

            log.info("食物图片分析完成: foods={}, confidence={}, isBalanced={}, mealRecordId={}",
                    analysisResult.getFoods(), analysisResult.getConfidence(),
                    analysisResult.getIsBalanced(), mealRecordId);

            // ===== 异步任务：启动后续 AI 分析任务 =====
            Map<String, String> asyncTasks = asyncTaskExecutorService.startAsyncTasks(
                    analysisResult, userId, mealRecordId);

            log.info("已启动异步任务: {}", asyncTasks);

            // 构建响应
            UploadResponse response = UploadResponse.builder()
                    .analysisResult(analysisResult)
                    .asyncTasks(asyncTasks)
                    .mealRecordId(mealRecordId)
                    .build();

            return ApiResponse.success("食物分析成功", response);

        } catch (Exception e) {
            log.error("食物图片分析失败", e);
            return ApiResponse.error("食物图片分析失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的所有用餐记录
     *
     * @param userId 用户 ID（可选，默认使用 DEFAULT_USER_ID）
     * @return 用餐记录列表
     */
    @Operation(summary = "查询用餐记录", description = "查询指定用户的所有用餐记录，按时间倒序排列")
    @GetMapping("/meals")
    public ApiResponse<List<MealRecord>> getMealRecords(
            @Parameter(description = "用户 ID，支持 Apple ID 格式", example = "000514.xxx.1422") @RequestParam(value = "userId", required = false) String userId) {
        try {
            String queryUserId = userId != null ? userId : MealRecordService.getDefaultUserId();
            log.info("查询用户的用餐记录: userId={}", queryUserId);

            List<MealRecord> records = mealRecordService.getMealRecordsByUserId(queryUserId);
            return ApiResponse.success("查询成功", records);

        } catch (Exception e) {
            log.error("查询用餐记录失败", e);
            return ApiResponse.error("查询用餐记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 查询单条用餐记录
     *
     * @param id 记录 ID
     * @return 用餐记录
     */
    @Operation(summary = "查询单条用餐记录", description = "根据记录 ID 查询单条用餐记录的详细信息")
    @GetMapping("/meals/{id}")
    public ApiResponse<MealRecord> getMealRecordById(
            @Parameter(description = "记录 ID", required = true, example = "1") @PathVariable Long id) {
        try {
            log.info("查询用餐记录: id={}", id);
            MealRecord record = mealRecordService.getMealRecordById(id);
            return ApiResponse.success("查询成功", record);

        } catch (Exception e) {
            log.error("查询用餐记录失败: id={}", id, e);
            return ApiResponse.error("查询用餐记录失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的平均健康分数
     *
     * @param userId 用户 ID（可选）
     * @return 平均健康分数
     */
    @Operation(summary = "查询平均健康分数", description = "查询用户所有用餐记录的平均健康分数")
    @GetMapping("/meals/stats/average-score")
    public ApiResponse<Double> getAverageHealthScore(
            @Parameter(description = "用户 ID", example = "000514.xxx.1422") @RequestParam(value = "userId", required = false) String userId) {
        try {
            String queryUserId = userId != null ? userId : MealRecordService.getDefaultUserId();
            Double avgScore = mealRecordService.getAverageHealthScore(queryUserId);
            return ApiResponse.success("查询成功", avgScore);

        } catch (Exception e) {
            log.error("查询平均健康分数失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
}
