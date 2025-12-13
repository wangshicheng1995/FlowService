package com.flowservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康压力值记录实体类
 * 存储用户每日的健康压力评分
 */
@Entity
@Table(name = "food_stress_score", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "score_days" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodStressScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "score_days", nullable = false)
    private LocalDate scoreDays;

    @Column(name = "score", nullable = false)
    private Integer score;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
