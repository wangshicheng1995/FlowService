package com.flowservice.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowservice.model.FoodAnalysisResponse;
import com.flowservice.model.ProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessService {

    private final QwenApiService qwenApiService;
    private final DataProcessService dataProcessService;
    private final MealRecordService mealRecordService;
    private final ImpactAnalysisService impactAnalysisService;
    private final QualityProteinService qualityProteinService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理食物图片分析请求
     * 调用通义千问 AI 进行食物识别和营养评估，并解析返回的 JSON
     * 同时将分析结果保存到数据库
     *
     * @param request 处理请求（包含图片 Base64 和 Prompt）
     * @param userId  用户 ID（可选，用于关联用餐记录）
     * @return 食物分析结果
     */
    public FoodAnalysisResponse processFoodAnalysis(ProcessRequest request, String userId) {
        String taskId = UUID.randomUUID().toString();
        log.info("开始处理食物图片分析任务: {}, userId: {}", taskId, userId);

        try {
            // 验证图片数据
            if (!dataProcessService.validateImageData(request.getImageBase64())) {
                throw new IllegalArgumentException("无效的图片数据");
            }

            // 调用通义千问 API
            MultiModalConversationResult qwenResult = qwenApiService.callQwenVisionApi(
                    request.getPrompt(),
                    request.getImageBase64());

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
                    taskId, response.getFoods().size(), response.getConfidence(), response.getIsBalanced());

            // ---------------------------------------------------------
            // 新增：识别优质蛋白来源
            // ---------------------------------------------------------
            try {
                if (response.getFoods() != null && !response.getFoods().isEmpty()) {
                    // 提取食物名称列表
                    List<String> foodNames = response.getFoods().stream()
                            .map(FoodAnalysisResponse.FoodItem::getName)
                            .collect(Collectors.toList());

                    // 调用优质蛋白识别服务
                    List<String> highQualityProteins = qualityProteinService.identifyHighQualityProteins(foodNames);
                    response.setHighQualityProteins(highQualityProteins);

                    log.info("优质蛋白识别完成: taskId={}, proteins={}", taskId, highQualityProteins);
                }
            } catch (Exception e) {
                log.error("优质蛋白识别失败，忽略错误", e);
            }
            // ---------------------------------------------------------

            // ---------------------------------------------------------
            // 新增：调用饮食影响分析服务
            // ---------------------------------------------------------
            try {
                if (response.getNutrition() != null) {
                    // 转换 Nutrition 对象为 MealNutrition 实体对象（或者直接让 ImpactAnalysisService 接收 DTO）
                    // 这里为了方便，我们直接让 ImpactAnalysisService 接收 DTO 转成的实体，或者修改 ImpactAnalysisService 接收
                    // DTO
                    // 简单起见，我们手动映射一下，或者修改 ImpactAnalysisService 入参。
                    // 鉴于 ImpactAnalysisService 定义的是接收 MealNutrition 实体，我们这里先构造一个临时的实体对象
                    com.flowservice.entity.MealNutrition mealNutrition = new com.flowservice.entity.MealNutrition();
                    mealNutrition.setEnergyKcal(response.getNutrition().getEnergyKcal());
                    mealNutrition.setProteinG(response.getNutrition().getProteinG());
                    mealNutrition.setFatG(response.getNutrition().getFatG());
                    mealNutrition.setCarbG(response.getNutrition().getCarbG());
                    mealNutrition.setFiberG(response.getNutrition().getFiberG());
                    mealNutrition.setSodiumMg(response.getNutrition().getSodiumMg());
                    mealNutrition.setSugarG(response.getNutrition().getSugarG());
                    mealNutrition.setSatFatG(response.getNutrition().getSatFatG());

                    com.flowservice.model.FullImpactAnalysisResult fullResult = impactAnalysisService
                            .analyzeImpact(mealNutrition, response.getIsBalanced(), response.getNutritionSummary());

                    if (fullResult != null) {
                        response.setOverallEvaluation(fullResult.getOverallEvaluation());
                        response.setImpact(fullResult.getImpact());
                    }
                }
            } catch (Exception e) {
                log.error("调用饮食影响分析服务失败，忽略错误", e);
            }
            // ---------------------------------------------------------

            // 保存分析结果到数据库
            try {
                mealRecordService.saveMealRecord(response, null, userId);
                log.info("食物分析结果已保存到数据库, userId: {}", userId);
            } catch (Exception e) {
                log.error("保存用餐记录到数据库失败，但继续返回 AI 分析结果", e);
                // 不抛出异常，继续返回 AI 分析结果
            }

            return response;

        } catch (Exception e) {
            log.error("食物图片分析任务失败: {}", taskId, e);
            throw new RuntimeException("食物图片分析失败: " + e.getMessage(), e);
        }
    }
}
