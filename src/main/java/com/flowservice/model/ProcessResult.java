package com.flowservice.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProcessResult {
    private String taskId;
    private String originalPrompt;
    private String processedText;
    private String summary;
    private ProcessMetadata metadata;
    private LocalDateTime processedAt;

    @Data
    public static class ProcessMetadata {
        private String fileName;
        private String mimeType;
        private Long fileSize;
        private String model;
        private Integer tokensUsed;
        private Long processingTimeMs;
    }
}