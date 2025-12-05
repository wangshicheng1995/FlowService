package com.flowservice.repository;

import com.flowservice.entity.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用餐记录 Repository
 * 提供数据库 CRUD 操作
 */
@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    /**
     * 根据用户 ID 查询所有用餐记录
     * 按吃饭时间降序排列
     *
     * @param userId 用户 ID
     * @return 用餐记录列表
     */
    List<MealRecord> findByUserIdOrderByEatenAtDesc(Long userId);

    /**
     * 根据用户 ID 和时间范围查询用餐记录
     *
     * @param userId    用户 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 用餐记录列表
     */
    List<MealRecord> findByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户 ID 和风险等级查询用餐记录
     *
     * @param userId    用户 ID
     * @param riskLevel 风险等级
     * @return 用餐记录列表
     */
    List<MealRecord> findByUserIdAndRiskLevelOrderByEatenAtDesc(Long userId, String riskLevel);

    /**
     * 查询用户的平均健康分数
     *
     * @param userId 用户 ID
     * @return 平均健康分数
     */
    @Query("SELECT AVG(m.healthScore) FROM MealRecord m WHERE m.userId = :userId AND m.healthScore IS NOT NULL")
    Double calculateAverageHealthScore(@Param("userId") Long userId);

    /**
     * 查询用户最近 N 条用餐记录
     *
     * @param userId 用户 ID
     * @param limit  记录数量
     * @return 用餐记录列表
     */
    @Query("SELECT m FROM MealRecord m WHERE m.userId = :userId ORDER BY m.eatenAt DESC LIMIT :limit")
    List<MealRecord> findRecentMealsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 统计用户的用餐记录总数
     *
     * @param userId 用户 ID
     * @return 记录总数
     */
    long countByUserId(Long userId);

    /**
     * 查询用户营养均衡的用餐记录数量
     *
     * @param userId 用户 ID
     * @return 均衡用餐记录数量
     */
    long countByUserIdAndIsBalanced(Long userId, Boolean isBalanced);

    /**
     * 根据用户 ID 和时间范围查询用餐记录（用于热量统计）
     *
     * @param userId    用户 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 用餐记录列表
     */
    List<MealRecord> findByUserIdAndEatenAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
