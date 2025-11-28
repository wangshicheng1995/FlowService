-- 创建主业务数据库（flow_db）
CREATE DATABASE IF NOT EXISTS flow_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE flow_db;

DROP TABLE IF EXISTS meal_nutrition;
DROP TABLE IF EXISTS meal_records;

-- 创建 meal_records 表
CREATE TABLE IF NOT EXISTS meal_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '属于哪个用户（前期可以先写死一个 ID）',
    eaten_at DATETIME NOT NULL COMMENT '吃这顿饭的时间（前端上传）',
    source_type VARCHAR(20) NOT NULL COMMENT '来源：PHOTO / TEXT / VOICE 等，方便后面分析',
    image_url VARCHAR(512) NULL COMMENT '食物照片在你对象存储里的地址，没图就 NULL',
    health_score INT NULL COMMENT '0-100，用来画趋势图、做平均值',
    risk_level VARCHAR(20) NULL COMMENT 'LOW / MEDIUM / HIGH，快速过滤高风险餐',
    note VARCHAR(255) NULL COMMENT '用户备注，可选',
    ai_result_json TEXT NULL COMMENT '模型返回的结构化结果原文（包括受影响器官、短中长期影响等）',
    food_items TEXT NULL COMMENT '识别出的食物列表（JSON 数组格式）',
    confidence DOUBLE NULL COMMENT '识别确定程度（0-1之间的小数）',
    is_balanced BOOLEAN NULL COMMENT '营养是否均衡',
    nutrition_summary VARCHAR(255) NULL COMMENT '营养评价概括（20字以内）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_eaten_at (eaten_at),
    INDEX idx_health_score (health_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用餐记录表';

-- 创建 meal_nutrition 表
CREATE TABLE IF NOT EXISTS meal_nutrition (
    meal_id BIGINT PRIMARY KEY COMMENT '关联 meal_records 主键',
    energy_kcal INT NULL COMMENT '总热量（千卡）',
    protein_g INT NULL COMMENT '蛋白质（克）',
    fat_g INT NULL COMMENT '总脂肪（克）',
    carb_g INT NULL COMMENT '碳水化合物（克）',
    fiber_g INT NULL COMMENT '膳食纤维（克）',
    sodium_mg INT NULL COMMENT '钠（毫克）',
    sugar_g INT NULL COMMENT '糖（克）',
    sat_fat_g DOUBLE NULL COMMENT '饱和脂肪（克）',
    CONSTRAINT fk_meal_nutrition_meal_id FOREIGN KEY (meal_id) REFERENCES meal_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用餐营养信息表';

-- 创建测试数据库（flow_test），结构与 flow_db 完全一致
CREATE DATABASE IF NOT EXISTS flow_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 若测试库尚未建表，则复制主库表结构
CREATE TABLE IF NOT EXISTS flow_test.meal_records LIKE flow_db.meal_records;

-- 创建 food_stress_score 表
CREATE TABLE IF NOT EXISTS food_stress_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    score_days DATE NOT NULL COMMENT '评分日期',
    score INT NOT NULL COMMENT '健康压力值 (0-100)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY idx_user_date (user_id, score_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康压力值记录表';

-- 同步测试库结构
CREATE TABLE IF NOT EXISTS flow_test.food_stress_score LIKE flow_db.food_stress_score;
