-- 创建主业务数据库（flow_db）
CREATE DATABASE IF NOT EXISTS flow_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE flow_db;

-- 先删除所有表（按依赖关系倒序删除）
DROP TABLE IF EXISTS meal_nutrition;
DROP TABLE IF EXISTS meal_records;
DROP TABLE IF EXISTS food_stress_score;
DROP TABLE IF EXISTS users;

-- 创建 users 表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    apple_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'Apple Sign In 的用户标识',
    nickname VARCHAR(50) NULL COMMENT '用户昵称',
    avatar_url VARCHAR(512) NULL COMMENT '头像 URL',
    target_calories INT NULL DEFAULT 2000 COMMENT '每日目标热量（千卡）',
    target_protein INT NULL DEFAULT 60 COMMENT '每日目标蛋白质（克）',
    target_carb INT NULL DEFAULT 250 COMMENT '每日目标碳水（克）',
    target_fat INT NULL DEFAULT 65 COMMENT '每日目标脂肪（克）',
    allergies TEXT NULL COMMENT '过敏食物列表（JSON 数组格式）',
    dietary_preference VARCHAR(50) NULL COMMENT '饮食偏好：NORMAL/VEGETARIAN/VEGAN/KETO 等',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_apple_id (apple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建 meal_records 表
CREATE TABLE IF NOT EXISTS meal_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户 ID（支持 Apple ID 格式，如 000514.xxx.1422）',
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
    INDEX idx_user_eaten (user_id, eaten_at),
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
    high_quality_proteins TEXT NULL COMMENT '优质蛋白来源列表（JSON 数组格式，如 ["鸡蛋", "鲈鱼", "虾仁"]）',
    CONSTRAINT fk_meal_nutrition_meal_id FOREIGN KEY (meal_id) REFERENCES meal_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用餐营养信息表';

-- 创建 food_stress_score 表
CREATE TABLE IF NOT EXISTS food_stress_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户 ID（支持 Apple ID 格式）',
    score_days DATE NOT NULL COMMENT '评分日期',
    score INT NOT NULL COMMENT '健康压力值 (0-100)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY idx_user_date (user_id, score_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康压力值记录表';

-- ==================== 测试数据库 ====================
CREATE DATABASE IF NOT EXISTS flow_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 复制主库表结构到测试库
CREATE TABLE IF NOT EXISTS flow_test.users LIKE flow_db.users;
CREATE TABLE IF NOT EXISTS flow_test.meal_records LIKE flow_db.meal_records;
CREATE TABLE IF NOT EXISTS flow_test.meal_nutrition LIKE flow_db.meal_nutrition;
CREATE TABLE IF NOT EXISTS flow_test.food_stress_score LIKE flow_db.food_stress_score;

-- ==================== 初始化测试数据 ====================

-- 插入用户数据
INSERT INTO users (apple_id, nickname, target_calories, dietary_preference) VALUES
('000514.a7d1133e26fa4fc490a32a9fb22abd9a.1422', 'Echo', 2000, 'NORMAL');

-- 插入最近一周的用餐记录（每天三餐）
-- 使用变量设置用户 ID
SET @user_id = '000514.a7d1133e26fa4fc490a32a9fb22abd9a.1422';

-- 第 1 天（7天前）
INSERT INTO meal_records (user_id, eaten_at, source_type, health_score, risk_level, food_items, confidence, is_balanced, nutrition_summary) VALUES
(@user_id, DATE_SUB(CURDATE(), INTERVAL 6 DAY) + INTERVAL 8 HOUR, 'PHOTO', 85, 'LOW', '["燕麦粥", "鸡蛋", "牛奶"]', 0.92, true, '营养均衡的早餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 6 DAY) + INTERVAL 12 HOUR, 'PHOTO', 75, 'LOW', '["米饭", "红烧肉", "炒青菜"]', 0.88, true, '热量适中的午餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 6 DAY) + INTERVAL 18 HOUR, 'PHOTO', 70, 'MEDIUM', '["面条", "卤蛋", "小菜"]', 0.85, false, '碳水偏高');

-- 第 2 天（6天前）
INSERT INTO meal_records (user_id, eaten_at, source_type, health_score, risk_level, food_items, confidence, is_balanced, nutrition_summary) VALUES
(@user_id, DATE_SUB(CURDATE(), INTERVAL 5 DAY) + INTERVAL 7 HOUR + INTERVAL 30 MINUTE, 'PHOTO', 80, 'LOW', '["全麦面包", "煎蛋", "果汁"]', 0.90, true, '健康早餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 5 DAY) + INTERVAL 12 HOUR + INTERVAL 30 MINUTE, 'PHOTO', 65, 'MEDIUM', '["汉堡", "薯条", "可乐"]', 0.95, false, '高热量快餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 5 DAY) + INTERVAL 19 HOUR, 'PHOTO', 88, 'LOW', '["清蒸鱼", "蔬菜沙拉", "糙米饭"]', 0.87, true, '低脂高蛋白晚餐');

-- 第 3 天（5天前）
INSERT INTO meal_records (user_id, eaten_at, source_type, health_score, risk_level, food_items, confidence, is_balanced, nutrition_summary) VALUES
(@user_id, DATE_SUB(CURDATE(), INTERVAL 4 DAY) + INTERVAL 8 HOUR, 'PHOTO', 78, 'LOW', '["豆浆", "油条", "小笼包"]', 0.89, false, '传统中式早餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 4 DAY) + INTERVAL 12 HOUR, 'PHOTO', 82, 'LOW', '["鸡胸肉沙拉", "全麦面包"]', 0.91, true, '低卡健康午餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 4 DAY) + INTERVAL 18 HOUR + INTERVAL 30 MINUTE, 'PHOTO', 72, 'LOW', '["火锅", "蔬菜", "豆腐"]', 0.86, true, '蛋白质丰富的晚餐');

-- 第 4 天（4天前）
INSERT INTO meal_records (user_id, eaten_at, source_type, health_score, risk_level, food_items, confidence, is_balanced, nutrition_summary) VALUES
(@user_id, DATE_SUB(CURDATE(), INTERVAL 3 DAY) + INTERVAL 7 HOUR + INTERVAL 45 MINUTE, 'PHOTO', 90, 'LOW', '["酸奶", "水果", "坚果"]', 0.93, true, '营养丰富的早餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 3 DAY) + INTERVAL 12 HOUR + INTERVAL 15 MINUTE, 'PHOTO', 68, 'MEDIUM', '["炸鸡", "米饭", "可乐"]', 0.92, false, '高脂高糖午餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 3 DAY) + INTERVAL 19 HOUR + INTERVAL 30 MINUTE, 'PHOTO', 85, 'LOW', '["寿司", "味噌汤", "毛豆"]', 0.88, true, '日式健康晚餐');

-- 第 5 天（3天前）
INSERT INTO meal_records (user_id, eaten_at, source_type, health_score, risk_level, food_items, confidence, is_balanced, nutrition_summary) VALUES
(@user_id, DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 8 HOUR + INTERVAL 15 MINUTE, 'PHOTO', 82, 'LOW', '["煎饼果子", "豆浆"]', 0.87, false, '碳水较多的早餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 12 HOUR, 'PHOTO', 78, 'LOW', '["牛肉面", "卤蛋", "小菜"]', 0.90, true, '蛋白质适中的午餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 18 HOUR, 'PHOTO', 75, 'LOW', '["烤鸡腿", "蔬菜", "米饭"]', 0.89, true, '均衡的晚餐');

-- 第 6 天（2天前）
INSERT INTO meal_records (user_id, eaten_at, source_type, health_score, risk_level, food_items, confidence, is_balanced, nutrition_summary) VALUES
(@user_id, DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 7 HOUR + INTERVAL 30 MINUTE, 'PHOTO', 88, 'LOW', '["牛奶", "全麦吐司", "牛油果"]', 0.94, true, '健康的西式早餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 12 HOUR + INTERVAL 30 MINUTE, 'PHOTO', 72, 'MEDIUM', '["披萨", "沙拉"]', 0.91, false, '热量较高的午餐'),
(@user_id, DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 19 HOUR, 'PHOTO', 80, 'LOW', '["三文鱼", "藜麦", "烤蔬菜"]', 0.92, true, '高蛋白健康晚餐');

-- 第 7 天（今天）
INSERT INTO meal_records (user_id, eaten_at, source_type, health_score, risk_level, food_items, confidence, is_balanced, nutrition_summary) VALUES
(@user_id, CURDATE() + INTERVAL 8 HOUR, 'PHOTO', 85, 'LOW', '["粥", "咸鸭蛋", "榨菜"]', 0.88, true, '清淡的中式早餐'),
(@user_id, CURDATE() + INTERVAL 12 HOUR, 'PHOTO', 76, 'LOW', '["黄焖鸡米饭"]', 0.93, false, '碳水较多的午餐'),
(@user_id, CURDATE() + INTERVAL 18 HOUR, 'PHOTO', 82, 'LOW', '["白灼虾", "清炒时蔬", "米饭"]', 0.90, true, '低脂高蛋白晚餐');

-- 插入对应的营养信息
-- 获取刚插入的记录 ID 并插入营养信息
INSERT INTO meal_nutrition (meal_id, energy_kcal, protein_g, fat_g, carb_g, fiber_g, sodium_mg, sugar_g, sat_fat_g, high_quality_proteins)
SELECT id, 
    CASE 
        WHEN HOUR(eaten_at) < 10 THEN FLOOR(300 + RAND() * 200)  -- 早餐 300-500
        WHEN HOUR(eaten_at) < 15 THEN FLOOR(500 + RAND() * 300)  -- 午餐 500-800
        ELSE FLOOR(400 + RAND() * 300)  -- 晚餐 400-700
    END as energy_kcal,
    FLOOR(15 + RAND() * 25) as protein_g,
    FLOOR(10 + RAND() * 20) as fat_g,
    FLOOR(30 + RAND() * 50) as carb_g,
    FLOOR(3 + RAND() * 7) as fiber_g,
    FLOOR(300 + RAND() * 800) as sodium_mg,
    FLOOR(5 + RAND() * 15) as sugar_g,
    ROUND(3 + RAND() * 7, 1) as sat_fat_g,
    CASE 
        WHEN food_items LIKE '%鸡%' OR food_items LIKE '%鱼%' OR food_items LIKE '%虾%' OR food_items LIKE '%蛋%' OR food_items LIKE '%牛%' THEN '["鸡肉", "鱼", "鸡蛋"]'
        ELSE '["豆制品"]'
    END as high_quality_proteins
FROM meal_records 
WHERE user_id = @user_id;
