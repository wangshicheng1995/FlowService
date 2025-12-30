package com.flowservice.model;

import com.flowservice.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户资料响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户资料响应")
public class UserProfileResponse {

    @Schema(description = "Apple ID 用户标识符", example = "001234.xxx...")
    private String userId;

    @Schema(description = "用户昵称", example = "小明")
    private String nickname;

    @Schema(description = "性别", example = "male")
    private String gender;

    @Schema(description = "出生年份", example = "1995")
    private Integer birthYear;

    @Schema(description = "身高(厘米)", example = "175.5")
    private Double heightCm;

    @Schema(description = "体重(公斤)", example = "70.0")
    private Double weightKg;

    @Schema(description = "活动水平", example = "moderate")
    private String activityLevel;

    @Schema(description = "健康目标", example = "loseWeight")
    private String healthGoal;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从 User 实体构建响应对象
     */
    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .userId(user.getAppleId())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .birthYear(user.getBirthYear())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .activityLevel(user.getActivityLevel())
                .healthGoal(user.getHealthGoal())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
