package com.flowservice.service;

import com.flowservice.model.AsyncTaskInfo;
import com.flowservice.model.AsyncTaskInfo.TaskType;
import com.flowservice.model.FoodAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 异步任务执行服务
 * 负责执行各类异步 AI 分析任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskExecutorService {

    private final AsyncTaskStorageService taskStorageService;
    // TODO: 后续添加各种 AI 分析服务的依赖
    // private final GlucoseTrendService glucoseTrendService;
    // private final EatingOrderService eatingOrderService;

    /**
     * 启动所有异步任务
     *
     * @param analysisResult 食物分析结果（作为异步任务的输入）
     * @param userId         用户 ID
     * @param mealRecordId   用餐记录 ID
     * @return 任务类型代码 -> 任务 ID 的映射
     */
    public Map<String, String> startAsyncTasks(FoodAnalysisResponse analysisResult,
            String userId,
            Long mealRecordId) {
        Map<String, String> taskMap = new HashMap<>();

        // 创建血糖趋势预测任务
        AsyncTaskInfo glucoseTask = taskStorageService.createTask(
                TaskType.GLUCOSE_TREND, userId, mealRecordId);
        taskMap.put(TaskType.GLUCOSE_TREND.getCode(), glucoseTask.getTaskId());
        executeGlucoseTrendTask(glucoseTask.getTaskId(), analysisResult);

        // 创建吃饭顺序建议任务
        AsyncTaskInfo eatingOrderTask = taskStorageService.createTask(
                TaskType.EATING_ORDER, userId, mealRecordId);
        taskMap.put(TaskType.EATING_ORDER.getCode(), eatingOrderTask.getTaskId());
        executeEatingOrderTask(eatingOrderTask.getTaskId(), analysisResult);

        log.info("已启动 {} 个异步任务, userId={}, mealRecordId={}",
                taskMap.size(), userId, mealRecordId);

        return taskMap;
    }

    /**
     * 异步执行血糖趋势预测任务
     */
    @Async
    public void executeGlucoseTrendTask(String taskId, FoodAnalysisResponse analysisResult) {
        log.info("开始执行血糖趋势预测任务: taskId={}", taskId);
        taskStorageService.markRunning(taskId);

        try {
            // TODO: 调用真实的血糖趋势预测 AI 服务
            // 目前使用模拟数据
            Thread.sleep(2000); // 模拟 AI 调用耗时

            // 模拟返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("peakValue", 7.8);
            result.put("peakTime", "餐后 30-60 分钟");
            result.put("recoveryTime", "餐后 1-3 小时");
            result.put("trendData", new double[] { 5.5, 6.2, 7.8, 7.2, 6.5, 5.8 });
            result.put("trendLabels", new String[] { "餐前", "餐后0-30分钟", "餐后30-60分钟",
                    "餐后1小时", "餐后2小时", "餐后3小时" });
            result.put("impactLevel", "中影响"); // 低影响/中影响/高影响

            taskStorageService.markCompleted(taskId, result);
            log.info("血糖趋势预测任务完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("血糖趋势预测任务失败: taskId={}", taskId, e);
            taskStorageService.markFailed(taskId, e.getMessage());
        }
    }

    /**
     * 异步执行吃饭顺序建议任务
     */
    @Async
    public void executeEatingOrderTask(String taskId, FoodAnalysisResponse analysisResult) {
        log.info("开始执行吃饭顺序建议任务: taskId={}", taskId);
        taskStorageService.markRunning(taskId);

        try {
            // TODO: 调用真实的吃饭顺序建议 AI 服务
            // 目前使用模拟数据
            Thread.sleep(3000); // 模拟 AI 调用耗时

            // 模拟返回结果
            Map<String, Object> result = new HashMap<>();

            // 吃饭顺序建议列表
            java.util.List<Map<String, String>> tips = new java.util.ArrayList<>();

            Map<String, String> tip1 = new HashMap<>();
            tip1.put("order", "1");
            tip1.put("title", "先吃蔬菜");
            tip1.put("description", "膳食纤维可减缓碳水吸收，建议先吃蔬菜类食物");
            tips.add(tip1);

            Map<String, String> tip2 = new HashMap<>();
            tip2.put("order", "2");
            tip2.put("title", "再吃蛋白质");
            tip2.put("description", "蛋白质可延长饱腹感，稳定血糖水平");
            tips.add(tip2);

            Map<String, String> tip3 = new HashMap<>();
            tip3.put("order", "3");
            tip3.put("title", "最后吃主食");
            tip3.put("description", "将碳水化合物放在最后，可显著降低血糖峰值");
            tips.add(tip3);

            result.put("tips", tips);
            result.put("summary", "按建议调整后，血糖峰值预计可降低 15-20%");

            taskStorageService.markCompleted(taskId, result);
            log.info("吃饭顺序建议任务完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("吃饭顺序建议任务失败: taskId={}", taskId, e);
            taskStorageService.markFailed(taskId, e.getMessage());
        }
    }
}
