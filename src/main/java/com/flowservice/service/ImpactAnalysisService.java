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
        private final ImpactDecisionService impactDecisionService;

        /**
         * 分析饮食影响
         *
         * @param nutrition        营养数据
         * @param isBalanced       是否均衡（AI 初步判断）
         * @param nutritionSummary 营养概括
         * @return 完整的影响分析结果
         */
        public com.flowservice.model.FullImpactAnalysisResult analyzeImpact(MealNutrition nutrition, boolean isBalanced,
                        String nutritionSummary) {
                try {
                        // 1. 计算标签
                        Set<NutritionTag> tags = HealthTagCalculator.calcTags(nutrition);

                        // 2. 决策策略
                        ImpactDecisionService.MealHealthContext context = new ImpactDecisionService.MealHealthContext(
                                        isBalanced, tags);
                        ImpactDecisionService.ImpactDecision decision = impactDecisionService.decide(context);

                        // 3. 构建 OverallEvaluation
                        com.flowservice.model.FoodAnalysisResponse.OverallEvaluation overall = new com.flowservice.model.FoodAnalysisResponse.OverallEvaluation();
                        overall.setAiIsBalanced(isBalanced);
                        overall.setRiskLevel(decision.getRiskLevel());
                        overall.setImpactStrategy(decision.getStrategy());
                        overall.setOverallScore(decision.getOverallScore());
                        // TODO: tagSummaries 可以后续完善，这里先留空或简单生成

                        // 4. 准备 AI Prompt
                        String promptTemplate;
                        if (decision.getStrategy() == ImpactDecisionService.ImpactStrategy.FULL_RISK_ANALYSIS) {
                                promptTemplate = PromptConfig.FULL_RISK_ANALYSIS_PROMPT;
                        } else if (decision.getStrategy() == ImpactDecisionService.ImpactStrategy.LIGHT_TIPS) {
                                promptTemplate = PromptConfig.LIGHT_TIPS_PROMPT;
                        } else {
                                // NONE 策略：直接返回，不调 AI
                                com.flowservice.model.FullImpactAnalysisResult result = new com.flowservice.model.FullImpactAnalysisResult();
                                result.setOverallEvaluation(overall);

                                // 构造默认的 Impact
                                com.flowservice.model.FoodAnalysisResponse.Impact impact = new com.flowservice.model.FoodAnalysisResponse.Impact();
                                impact.setPrimaryText("饮食整体健康，请继续保持！");
                                impact.setRiskTags(new java.util.ArrayList<>(tags));
                                result.setImpact(impact);

                                // 构造默认的 LegacyResult
                                ImpactAnalysisResult legacy = new ImpactAnalysisResult();
                                legacy.setShortTerm("无明显风险");
                                legacy.setMidTerm("无明显风险");
                                legacy.setLongTerm("无明显风险");
                                legacy.setRiskTags(tags.stream().map(Enum::name)
                                                .collect(java.util.stream.Collectors.toList()));
                                result.setLegacyResult(legacy);

                                return result;
                        }

                        // 5. 调用 AI
                        // 构造输入 JSON
                        java.util.Map<String, Object> inputMap = new java.util.HashMap<>();
                        inputMap.put("ai_is_balanced", isBalanced);
                        inputMap.put("nutrition_summary", nutritionSummary);
                        inputMap.put("nutrition_values", nutrition);
                        inputMap.put("risk_tags", tags);
                        inputMap.put("overall_risk_level", decision.getRiskLevel());
                        inputMap.put("impact_strategy", decision.getStrategy());

                        String inputJson = objectMapper.writeValueAsString(inputMap);
                        String userPrompt = promptTemplate.replace("{{INPUT_JSON}}", inputJson);

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
                                        .model("qwen-long")
                                        .messages(Arrays.asList(systemMsg, userMsg))
                                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                                        .topP(0.8)
                                        .enableSearch(false)
                                        .build();

                        GenerationResult genResult = gen.call(param);
                        String content = genResult.getOutput().getChoices().get(0).getMessage().getContent();

                        // 6. 清理并解析 JSON
                        String cleanedJson = content.replaceAll("```json", "")
                                        .replaceAll("```", "")
                                        .trim();

                        log.info("AI 返回的影响分析 JSON: {}", cleanedJson);

                        com.flowservice.model.FoodAnalysisResponse.Impact impact = objectMapper.readValue(
                                        cleanedJson, com.flowservice.model.FoodAnalysisResponse.Impact.class);

                        // 补全 Impact 的 riskTags (如果 AI 没返回或返回了字符串)
                        // 这里假设 AI 返回的 JSON 能映射到 Impact，如果 riskTags 是字符串列表，可能需要处理
                        // FoodAnalysisResponse.Impact 定义 riskTags 为 List<NutritionTag>，这可能导致反序列化失败如果 AI 返回字符串
                        // 实际上 Prompt 里的 riskTags 示例是字符串数组。
                        // 我们需要让 Jackson 能反序列化字符串到枚举，或者 Impact 用 String 接收然后转换。
                        // 为了稳妥，我们手动设置 riskTags
                        impact.setRiskTags(new java.util.ArrayList<>(tags));

                        // 7. 构造结果
                        com.flowservice.model.FullImpactAnalysisResult result = new com.flowservice.model.FullImpactAnalysisResult();
                        result.setOverallEvaluation(overall);
                        result.setImpact(impact);

                        // 构造 LegacyResult
                        ImpactAnalysisResult legacy = new ImpactAnalysisResult();
                        legacy.setShortTerm(impact.getShortTerm() != null ? impact.getShortTerm()
                                        : impact.getPrimaryText());
                        legacy.setMidTerm(impact.getMidTerm() != null ? impact.getMidTerm()
                                        : impact.getPrimaryText());
                        legacy.setLongTerm(impact.getLongTerm() != null ? impact.getLongTerm()
                                        : impact.getPrimaryText());
                        legacy.setRiskTags(
                                        impact.getRiskTags() != null
                                                        ? impact.getRiskTags().stream().map(Enum::name)
                                                                        .collect(java.util.stream.Collectors.toList())
                                                        : null);
                        result.setLegacyResult(legacy);

                        return result;

                } catch (Exception e) {
                        log.error("饮食影响分析失败", e);
                        return null;
                }
        }
}
