package com.flowservice.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户资料请求")
public class UserProfileRequest {

    @NotBlank(message = "userId is required")
    @Schema(description = "Apple ID 用户标识符", example = "001234.xxx...", required = true)
    private String userId;

    @NotBlank(message = "nickname is required")
    @Size(max = 50, message = "nickname must be at most 50 characters")
    @Schema(description = "用户昵称", example = "小明", required = true)
    private String nickname;

    @NotBlank(message = "gender is required")
    @Pattern(regexp = "^(male|female|other)$", message = "gender must be one of: male, female, other")
    @Schema(description = "性别：male | female | other", example = "male", required = true)
    private String gender;

    @NotNull(message = "birthYear is required")
    @Min(value = 1900, message = "birthYear must be at least 1900")
    @Max(value = 2025, message = "birthYear must be at most 2025")
    @Schema(description = "出生年份 (1900-2025)", example = "1995", required = true)
    private Integer birthYear;

    @NotNull(message = "heightCm is required")
    @DecimalMin(value = "50.0", message = "heightCm must be at least 50.0")
    @DecimalMax(value = "300.0", message = "heightCm must be at most 300.0")
    @Schema(description = "身高(厘米) (50.0-300.0)", example = "175.5", required = true)
    private Double heightCm;

    @NotNull(message = "weightKg is required")
    @DecimalMin(value = "10.0", message = "weightKg must be at least 10.0")
    @DecimalMax(value = "500.0", message = "weightKg must be at most 500.0")
    @Schema(description = "体重(公斤) (10.0-500.0)", example = "70.0", required = true)
    private Double weightKg;

    @NotBlank(message = "activityLevel is required")
    @Pattern(regexp = "^(sedentary|light|moderate|active|veryActive)$", message = "activityLevel must be one of: sedentary, light, moderate, active, veryActive")
    @Schema(description = "活动水平：sedentary | light | moderate | active | veryActive", example = "moderate", required = true)
    private String activityLevel;

    @NotBlank(message = "healthGoal is required")
    @Pattern(regexp = "^(loseWeight|maintain|gainWeight|improveHealth|controlBloodSugar)$", message = "healthGoal must be one of: loseWeight, maintain, gainWeight, improveHealth, controlBloodSugar")
    @Schema(description = "健康目标：loseWeight | maintain | gainWeight | improveHealth | controlBloodSugar", example = "loseWeight", required = true)
    private String healthGoal;
}
