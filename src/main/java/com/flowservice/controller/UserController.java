package com.flowservice.controller;

import com.flowservice.model.ApiResponse;
import com.flowservice.model.UserProfileRequest;
import com.flowservice.model.UserProfileResponse;
import com.flowservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 提供用户资料相关的 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户资料管理接口")
public class UserController {

    private final UserService userService;

    /**
     * 查询用户资料
     *
     * @param userId Apple ID 用户标识符
     * @return 用户资料响应
     */
    @Operation(summary = "查询用户资料", description = "根据用户ID查询用户资料\\n\\n" +
            "- 200 OK: 用户存在，返回资料\\n" +
            "- 404 Not Found: 用户不存在（新用户）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "用户存在，返回资料", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在（新用户）")
    })
    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @Parameter(description = "Apple ID 用户标识符", required = true, example = "001234.xxx...") @PathVariable String userId) {

        log.info("查询用户资料请求, userId: {}", userId);

        return userService.getUserProfile(userId)
                .map(profile -> {
                    log.info("用户资料查询成功, userId: {}", userId);
                    return ResponseEntity.ok(ApiResponse.success(profile));
                })
                .orElseGet(() -> {
                    log.info("用户不存在, userId: {}", userId);
                    return ResponseEntity.status(404)
                            .body(ApiResponse.error(404, "User not found"));
                });
    }

    /**
     * 保存/更新用户资料
     *
     * @param request       用户资料请求
     * @param bindingResult 参数校验结果
     * @return 保存后的用户资料响应
     */
    @Operation(summary = "保存/更新用户资料", description = "保存或更新用户资料\\n\\n" +
            "- 用户不存在时创建新用户\\n" +
            "- 用户存在时更新资料\\n\\n" +
            "参数校验规则：\\n" +
            "- userId: 必填\\n" +
            "- nickname: 必填，最长50字符\\n" +
            "- gender: 必填，枚举: male | female | other\\n" +
            "- birthYear: 必填，范围 1900-2025\\n" +
            "- heightCm: 必填，范围 50.0-300.0\\n" +
            "- weightKg: 必填，范围 10.0-500.0\\n" +
            "- activityLevel: 必填，枚举: sedentary | light | moderate | active | veryActive\\n" +
            "- healthGoal: 必填，枚举: loseWeight | maintain | gainWeight | improveHealth | controlBloodSugar")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "保存成功", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数校验失败")
    })
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> saveUserProfile(
            @Valid @RequestBody UserProfileRequest request,
            BindingResult bindingResult) {

        log.info("保存用户资料请求, userId: {}", request.getUserId());

        // 参数校验失败
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> "Validation failed: " + error.getDefaultMessage())
                    .orElse("Validation failed");

            log.warn("参数校验失败: {}", errorMessage);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, errorMessage));
        }

        // 保存或更新用户资料
        UserProfileResponse response = userService.saveOrUpdateUserProfile(request);
        log.info("用户资料保存成功, userId: {}", response.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
