#!/bin/bash

echo "正在启动 FlowService..."

# 检查Java版本
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请安装Java 17或更高版本"
    exit 1
fi

java_version=$(java -version 2>&1 | grep -oP 'version "\\K[0-9]+')
if [ "$java_version" -lt 17 ]; then
    echo "错误: Java版本过低，需要Java 17或更高版本"
    exit 1
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请安装Maven"
    exit 1
fi

# 检查API密钥
if [ -z "$QWEN_API_KEY" ]; then
    echo "警告: 未设置QWEN_API_KEY环境变量"
    echo "请运行: export QWEN_API_KEY=\"your-api-key\""
    echo "或在application.yml中配置API密钥"
fi

# 编译项目
echo "正在编译项目..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

echo "编译完成，正在启动服务..."
echo "服务将在 http://localhost:8080 启动"
echo "按 Ctrl+C 停止服务"
echo ""

# 启动服务
mvn spring-boot:run