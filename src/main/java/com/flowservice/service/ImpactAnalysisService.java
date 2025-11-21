package com.flowservice.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowservice.config.PromptConfig;
import com.flowservice.config.QwenConfig;
import com.flowservice.entity.MealNutrition;
import com.flowservice.model.ImpactAnalysisResult;
import com.flowservice.model.NutritionTag;
import com.flowservice.util.HealthTagCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;

/**
 * 饮食影响分析服务
 * 调用 AI 分析饮食对身体的短中长期影响
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImpactAnalysisService {

        private final ObjectMapper objectMapper;
        private final QwenConfig qwenConfig;

        /**
         * 分析饮食影响
         *
         * @param nutrition 营养数据
         * @return 影响分析结果
         */
        public ImpactAnalysisResult analyzeImpact(MealNutrition nutrition) {
                try {
                        // 1. 计算标签
                        Set<NutritionTag> tags = HealthTagCalculator.calcTags(nutrition);

                        // 2. 准备 Prompt 数据
                        String nutritionJson = objectMapper.writeValueAsString(nutrition);
                        String tagsJson = objectMapper.writeValueAsString(tags);

                        // 3. 构建 Prompt
                        String userPrompt = PromptConfig.MEAL_IMPACT_ANALYSIS_PROMPT
                                        .replace("{{NUTRITION_JSON}}", nutritionJson)
                                        .replace("{{TAGS_JSON}}", tagsJson);

                        // 4. 调用 AI
                        Generation gen = new Generation();
                        Message systemMsg = Message.builder()
                                        .role(Role.SYSTEM.getValue())
                                        .content("你是一名专业的营养科医生。")
                                        .build();
                        Message userMsg = Message.builder()
                                        .role(Role.USER.getValue())
                                        .content(userPrompt)
                                        .build();

                        GenerationParam param = GenerationParam.builder()
                                        .apiKey(qwenConfig.getKey())
                                        .model("qwen-long") // 使用支持长文本的模型，或者 qwen-turbo
                                        .messages(Arrays.asList(systemMsg, userMsg))
                                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                                        .topP(0.8)
                                        .enableSearch(false)
                                        .build();

                        GenerationResult result = gen.call(param);
                        String content = result.getOutput().getChoices().get(0).getMessage().getContent();

                        // 5. 清理并解析 JSON
                        String cleanedJson = content.replaceAll("```json", "")
                                        .replaceAll("```", "")
                                        .trim();

                        log.info("AI 返回的影响分析 JSON: {}", cleanedJson);

                        return objectMapper.readValue(cleanedJson, ImpactAnalysisResult.class);

                } catch (Exception e) {
                        log.error("饮食影响分析失败", e);
                        // 返回空结果或错误提示，不阻断主流程
                        return null;
                }
        }
}
