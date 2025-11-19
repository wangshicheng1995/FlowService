-- 创建主业务数据库（flow_db）
CREATE DATABASE IF NOT EXISTS flow_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE flow_db;

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

-- 创建测试数据库（flow_test），结构与 flow_db 完全一致
CREATE DATABASE IF NOT EXISTS flow_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 若测试库尚未建表，则复制主库表结构
CREATE TABLE IF NOT EXISTS flow_test.meal_records LIKE flow_db.meal_records;
