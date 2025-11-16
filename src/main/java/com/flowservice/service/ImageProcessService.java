package com.flowservice.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowservice.model.FoodAnalysisResponse;
import com.flowservice.model.ProcessRequest;
import com.flowservice.model.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessService {

    private final QwenApiService qwenApiService;
    private final DataProcessService dataProcessService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcessResult processImage(ProcessRequest request) {
        long startTime = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString();

        log.info("开始处理图片任务: {}", taskId);

        try {
            if (!dataProcessService.validateImageData(request.getImageBase64())) {
                throw new IllegalArgumentException("无效的图片数据");
            }

            MultiModalConversationResult qwenResult = qwenApiService.callQwenVisionApi(
                    request.getPrompt(),
                    request.getImageBase64()
            );

            if (qwenResult == null || qwenResult.getOutput() == null) {
                throw new RuntimeException("通义千问API返回空响应");
            }

            String originalText = qwenResult.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text").toString();
            String processedText = dataProcessService.processQwenResponse(originalText);
            String summary = dataProcessService.generateSummary(processedText);

            ProcessResult result = new ProcessResult();
            result.setTaskId(taskId);
            result.setOriginalPrompt(request.getPrompt());
            result.setProcessedText(processedText);
            result.setSummary(summary);
            result.setProcessedAt(LocalDateTime.now());

            ProcessResult.ProcessMetadata metadata = new ProcessResult.ProcessMetadata();
            metadata.setFileName(request.getFileName());
            metadata.setMimeType(request.getMimeType());
            metadata.setModel(request.getOptions() != null ? request.getOptions().getModel() : "qwen-vl-plus");
            metadata.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            if (qwenResult.getUsage() != null) {
                metadata.setTokensUsed(qwenResult.getUsage().getOutputTokens());
            }

            if (request.getImageBase64() != null) {
                metadata.setFileSize((long) (request.getImageBase64().length() * 3 / 4));
            }

            result.setMetadata(metadata);

            log.info("图片处理任务完成: {}, 耗时: {}ms", taskId, metadata.getProcessingTimeMs());

            return result;

        } catch (Exception e) {
            log.error("图片处理任务失败: {}", taskId, e);
            throw new RuntimeException("图片处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理食物图片分析请求
     * 调用通义千问 AI 进行食物识别和营养评估，并解析返回的 JSON
     *
     * @param request 处理请求（包含图片 Base64 和 Prompt）
     * @return 食物分析结果
     */
    public FoodAnalysisResponse processFoodAnalysis(ProcessRequest request) {
        String taskId = UUID.randomUUID().toString();
        log.info("开始处理食物图片分析任务: {}", taskId);

        try {
            // 验证图片数据
            if (!dataProcessService.validateImageData(request.getImageBase64())) {
                throw new IllegalArgumentException("无效的图片数据");
            }

            // 调用通义千问 API
            MultiModalConversationResult qwenResult = qwenApiService.callQwenVisionApi(
                    request.getPrompt(),
                    request.getImageBase64()
            );

            if (qwenResult == null || qwenResult.getOutput() == null) {
                throw new RuntimeException("通义千问API返回空响应");
            }

            // 提取 AI 返回的文本
            String aiResponseText = qwenResult.getOutput().getChoices().get(0)
                    .getMessage().getContent().get(0).get("text").toString();

            log.info("AI 返回原始文本: {}", aiResponseText);

            // 清理文本：移除可能的 markdown 代码块标记
            String cleanedJson = aiResponseText
                    .trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();

            log.info("清理后的 JSON: {}", cleanedJson);

            // 解析 JSON 为 FoodAnalysisResponse 对象
            FoodAnalysisResponse response = objectMapper.readValue(cleanedJson, FoodAnalysisResponse.class);

            log.info("食物分析任务完成: {}, 识别到 {} 种食物, 确定度: {}, 营养均衡: {}",
                    taskId, response.getFoodItems().size(), response.getConfidence(), response.getIsBalanced());

            return response;

        } catch (Exception e) {
            log.error("食物图片分析任务失败: {}", taskId, e);
            throw new RuntimeException("食物图片分析失败: " + e.getMessage(), e);
        }
    }
}