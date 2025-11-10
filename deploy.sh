#!/bin/bash

set -e

echo "========================================="
echo "FlowService 部署脚本"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 API Key
if [ -z "$DASHSCOPE_API_KEY" ]; then
    echo -e "${RED}错误: DASHSCOPE_API_KEY 环境变量未设置${NC}"
    echo "请运行: export DASHSCOPE_API_KEY=\"your-api-key\""
    exit 1
fi

# 检查部署方式
echo ""
echo "请选择部署方式:"
echo "1. Docker 部署（推荐）"
echo "2. 直接 JAR 部署"
read -p "请输入选项 [1-2]: " deploy_option

case $deploy_option in
    1)
        echo -e "${GREEN}使用 Docker 部署...${NC}"

        # 检查 Docker
        if ! command -v docker &> /dev/null; then
            echo -e "${RED}错误: Docker 未安装${NC}"
            exit 1
        fi

        # 检查 docker-compose
        if ! command -v docker-compose &> /dev/null; then
            echo -e "${YELLOW}警告: docker-compose 未安装，尝试使用 docker compose${NC}"
            DOCKER_COMPOSE="docker compose"
        else
            DOCKER_COMPOSE="docker-compose"
        fi

        # 停止已有容器
        echo "停止现有容器..."
        $DOCKER_COMPOSE down || true

        # 构建镜像
        echo "构建 Docker 镜像..."
        docker build -t flowservice:latest .

        # 启动服务
        echo "启动服务..."
        $DOCKER_COMPOSE up -d

        # 等待服务启动
        echo "等待服务启动..."
        sleep 10

        # 检查服务状态
        echo "检查服务状态..."
        $DOCKER_COMPOSE ps

        # 健康检查
        echo "执行健康检查..."
        max_attempts=10
        attempt=0

        while [ $attempt -lt $max_attempts ]; do
            if curl -s http://localhost:8080/api/status/health > /dev/null; then
                echo -e "${GREEN}✓ 服务启动成功！${NC}"
                echo ""
                echo "访问地址:"
                echo "  - 健康检查: http://localhost:8080/api/status/health"
                echo "  - 图片上传: http://localhost:8080/api/image/upload"
                echo "  - 测试页面: http://localhost:8080/api/test.html"
                echo ""
                echo "查看日志: $DOCKER_COMPOSE logs -f"
                exit 0
            fi
            attempt=$((attempt + 1))
            echo "等待服务启动... ($attempt/$max_attempts)"
            sleep 3
        done

        echo -e "${RED}✗ 服务启动失败，请查看日志${NC}"
        $DOCKER_COMPOSE logs
        exit 1
        ;;

    2)
        echo -e "${GREEN}使用 JAR 直接部署...${NC}"

        # 检查 Java
        if ! command -v java &> /dev/null; then
            echo -e "${RED}错误: Java 未安装${NC}"
            exit 1
        fi

        java_version=$(java -version 2>&1 | grep -oP 'version "\\K[0-9]+')
        if [ "$java_version" -lt 17 ]; then
            echo -e "${RED}错误: Java 版本过低，需要 Java 17 或更高版本${NC}"
            exit 1
        fi

        # 检查 Maven
        if ! command -v mvn &> /dev/null; then
            echo -e "${RED}错误: Maven 未安装${NC}"
            exit 1
        fi

        # 编译打包
        echo "编译打包..."
        mvn clean package -DskipTests

        # 创建日志目录
        echo "创建日志目录..."
        sudo mkdir -p /var/log/flowservice
        sudo chown $USER:$USER /var/log/flowservice

        # 停止现有进程
        echo "停止现有进程..."
        pkill -f flowservice.jar || true

        # 启动服务
        echo "启动服务..."
        nohup java -jar target/flowservice-*.jar --spring.profiles.active=prod > /var/log/flowservice/nohup.log 2>&1 &

        # 等待服务启动
        echo "等待服务启动..."
        sleep 10

        # 健康检查
        echo "执行健康检查..."
        max_attempts=10
        attempt=0

        while [ $attempt -lt $max_attempts ]; do
            if curl -s http://localhost:8080/api/status/health > /dev/null; then
                echo -e "${GREEN}✓ 服务启动成功！${NC}"
                echo ""
                echo "访问地址:"
                echo "  - 健康检查: http://localhost:8080/api/status/health"
                echo "  - 图片上传: http://localhost:8080/api/image/upload"
                echo "  - 测试页面: http://localhost:8080/api/test.html"
                echo ""
                echo "查看日志: tail -f /var/log/flowservice/application.log"
                exit 0
            fi
            attempt=$((attempt + 1))
            echo "等待服务启动... ($attempt/$max_attempts)"
            sleep 3
        done

        echo -e "${RED}✗ 服务启动失败，请查看日志${NC}"
        tail -100 /var/log/flowservice/nohup.log
        exit 1
        ;;

    *)
        echo -e "${RED}无效选项${NC}"
        exit 1
        ;;
esac