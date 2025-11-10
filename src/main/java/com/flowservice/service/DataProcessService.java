package com.flowservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataProcessService {

    public String processQwenResponse(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return "无内容";
        }

        log.info("开始处理通义千问返回的文本, 长度: {}", originalText.length());

        String processed = originalText
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\r\\n]+", "\n");

        log.info("文本处理完成, 处理后长度: {}", processed.length());
        return processed;
    }

    public String generateSummary(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "无内容摘要";
        }

        log.info("生成文本摘要, 原文长度: {}", text.length());

        List<String> sentences = Arrays.stream(text.split("[。！？.!?]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (sentences.isEmpty()) {
            return "无有效内容";
        }

        String summary;
        if (sentences.size() <= 3) {
            summary = String.join("。", sentences) + "。";
        } else {
            List<String> firstThree = sentences.subList(0, 3);
            summary = String.join("。", firstThree) + "...";
        }

        log.info("摘要生成完成, 摘要长度: {}", summary.length());
        return summary;
    }

    public String extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String[] commonWords = {"的", "了", "在", "是", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"};

        String[] words = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ")
                .split("\\s+");

        return Arrays.stream(words)
                .filter(word -> word.length() > 1)
                .filter(word -> !Arrays.asList(commonWords).contains(word))
                .distinct()
                .limit(10)
                .collect(Collectors.joining(","));
    }

    public boolean validateImageData(String imageBase64) {
        if (imageBase64 == null || imageBase64.trim().isEmpty()) {
            return false;
        }

        try {
            java.util.Base64.getDecoder().decode(imageBase64);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("无效的Base64图片数据");
            return false;
        }
    }

    public String formatResponseForClient(String processedText, String summary) {
        StringBuilder result = new StringBuilder();

        if (summary != null && !summary.trim().isEmpty()) {
            result.append("【摘要】\n").append(summary).append("\n\n");
        }

        if (processedText != null && !processedText.trim().isEmpty()) {
            result.append("【详细内容】\n").append(processedText);
        }

        return result.toString().trim();
    }
}