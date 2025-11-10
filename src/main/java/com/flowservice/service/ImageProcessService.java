package com.flowservice.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
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
}