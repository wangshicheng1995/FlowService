package com.flowservice.model;

import lombok.Data;

@Data
public class ProcessRequest {
    private String prompt;
    private String imageBase64;
    private String fileName;
    private String mimeType;
    private ProcessOptions options;

    @Data
    public static class ProcessOptions {
        private Double temperature = 0.7;
        private Integer maxTokens = 1000;
        private String model = "qwen-vl-plus";
    }
}