package com.flowservice.service;

import com.flowservice.entity.User;
import com.flowservice.model.UserProfileRequest;
import com.flowservice.model.UserProfileResponse;
import com.flowservice.repository.UserRepository;
import com.flowservice.util.NutritionCalculator;
import com.flowservice.util.NutritionCalculator.NutritionTargets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户服务
 * 处理用户资料的查询和保存/更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 根据用户ID查询用户资料
     *
     * @param userId Apple ID 用户标识符
     * @return 用户资料响应（Optional）
     */
    public Optional<UserProfileResponse> getUserProfile(String userId) {
        log.info("查询用户资料, userId: {}", userId);
        return userRepository.findByAppleId(userId)
                .map(UserProfileResponse::fromEntity);
    }

    /**
     * 保存或更新用户资料
     * 如果用户不存在则创建，存在则更新
     * 同时会根据用户身体数据动态计算营养目标
     *
     * @param request 用户资料请求
     * @return 保存后的用户资料响应
     */
    @Transactional
    public UserProfileResponse saveOrUpdateUserProfile(UserProfileRequest request) {
        log.info("保存/更新用户资料, userId: {}", request.getUserId());

        // 查找现有用户或创建新用户
        User user = userRepository.findByAppleId(request.getUserId())
                .orElseGet(() -> {
                    log.info("用户不存在，创建新用户, userId: {}", request.getUserId());
                    User newUser = new User();
                    newUser.setAppleId(request.getUserId());
                    return newUser;
                });

        // 更新用户基础资料
        user.setNickname(request.getNickname());
        user.setGender(request.getGender());
        user.setBirthYear(request.getBirthYear());
        user.setHeightCm(request.getHeightCm());
        user.setWeightKg(request.getWeightKg());
        user.setActivityLevel(request.getActivityLevel());
        user.setHealthGoal(request.getHealthGoal());

        // 动态计算营养目标
        NutritionTargets targets = NutritionCalculator.calculate(
                request.getGender(),
                request.getBirthYear(),
                request.getHeightCm(),
                request.getWeightKg(),
                request.getActivityLevel(),
                request.getHealthGoal());

        user.setTargetCalories(targets.getTargetCalories());
        user.setTargetProtein(targets.getTargetProtein());
        user.setTargetCarb(targets.getTargetCarb());
        user.setTargetFat(targets.getTargetFat());

        log.info("营养目标已计算: calories={}, protein={}g, carb={}g, fat={}g",
                targets.getTargetCalories(), targets.getTargetProtein(),
                targets.getTargetCarb(), targets.getTargetFat());

        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户资料保存成功, userId: {}", savedUser.getAppleId());

        return UserProfileResponse.fromEntity(savedUser);
    }
}
