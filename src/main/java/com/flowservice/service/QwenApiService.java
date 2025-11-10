package com.flowservice.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.flowservice.config.QwenConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenApiService {

    private final QwenConfig qwenConfig;

    public MultiModalConversationResult callQwenVisionApi(String prompt, String imageBase64) {
        try {
            log.info("调用通义千问VL API, prompt: {}", prompt);

            MultiModalConversation conv = new MultiModalConversation();

            List<Map<String, Object>> content = new ArrayList<>();

            if (imageBase64 != null && !imageBase64.isEmpty()) {
                content.add(Collections.singletonMap("image", "data:image/jpeg;base64," + imageBase64));
            }

            content.add(Collections.singletonMap("text", prompt));

            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(content)
                    .build();

            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(qwenConfig.getKey())
                    .model("qwen-vl-plus")
                    .message(userMessage)
                    .build();

            MultiModalConversationResult result = conv.call(param);

            if (result != null && result.getOutput() != null) {
                log.info("通义千问API调用成功");
            } else {
                log.warn("通义千问API返回空响应");
            }

            return result;

        } catch (NoApiKeyException e) {
            log.error("API Key未配置或无效", e);
            throw new RuntimeException("API Key未配置或无效", e);
        } catch (ApiException e) {
            log.error("通义千问API异常: code={}, message={}", e.getStatus().getStatusCode(), e.getMessage(), e);
            throw new RuntimeException("调用通义千问API失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("调用通义千问API失败", e);
            throw new RuntimeException("调用通义千问API失败: " + e.getMessage(), e);
        }
    }
}