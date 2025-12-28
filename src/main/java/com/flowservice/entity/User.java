package com.flowservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Apple Sign In 的用户标识
     */
    @Column(name = "apple_id", nullable = false, unique = true, length = 64)
    private String appleId;

    /**
     * 用户昵称
     */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /**
     * 头像 URL
     */
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /**
     * 每日目标热量（千卡）
     */
    @Column(name = "target_calories")
    private Integer targetCalories = 2000;

    /**
     * 每日目标蛋白质（克）
     */
    @Column(name = "target_protein")
    private Integer targetProtein = 60;

    /**
     * 每日目标碳水（克）
     */
    @Column(name = "target_carb")
    private Integer targetCarb = 250;

    /**
     * 每日目标脂肪（克）
     */
    @Column(name = "target_fat")
    private Integer targetFat = 65;

    /**
     * 过敏食物列表（JSON 数组格式）
     */
    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    /**
     * 饮食偏好：NORMAL/VEGETARIAN/VEGAN/KETO 等
     */
    @Column(name = "dietary_preference", length = 50)
    private String dietaryPreference;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
