#!/bin/bash

# API 测试脚本

set -e

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 默认服务器地址
SERVER=${1:-"http://localhost:8080"}

echo "========================================="
echo "FlowService API 测试脚本"
echo "========================================="
echo "服务器地址: $SERVER"
echo ""

# 测试1: 健康检查
echo "测试1: 健康检查接口"
echo "请求: GET $SERVER/api/home/health"
response=$(curl -s -w "\n%{http_code}" "$SERVER/api/home/health")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n -1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ 健康检查通过${NC}"
    echo "响应: $body"
else
    echo -e "${RED}✗ 健康检查失败 (HTTP $http_code)${NC}"
    echo "响应: $body"
    exit 1
fi

echo ""

# 测试2: 首页仪表盘接口
echo "测试2: 首页仪表盘接口"
echo "请求: GET $SERVER/api/home/dashboard?userId=test_user"
response=$(curl -s -w "\n%{http_code}" "$SERVER/api/home/dashboard?userId=test_user")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n -1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ 首页仪表盘获取成功${NC}"
    echo "响应: $body"
else
    echo -e "${RED}✗ 首页仪表盘获取失败 (HTTP $http_code)${NC}"
    echo "响应: $body"
    exit 1
fi

echo ""

# 测试3: 图片上传接口（需要测试图片）
echo "测试3: 图片上传接口"
echo -e "${YELLOW}此测试需要提供测试图片${NC}"
read -p "是否有测试图片? (y/n): " has_image

if [ "$has_image" = "y" ] || [ "$has_image" = "Y" ]; then
    read -p "请输入图片路径: " image_path

    if [ ! -f "$image_path" ]; then
        echo -e "${RED}✗ 文件不存在: $image_path${NC}"
        exit 1
    fi

    echo "上传图片: $image_path"
    echo "请求: POST $SERVER/api/record/upload"

    response=$(curl -s -w "\n%{http_code}" -X POST "$SERVER/api/record/upload" \
        -F "file=@$image_path" \
        -F "userId=test_user")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ 图片上传成功${NC}"
        echo "响应: $body" | python3 -m json.tool 2>/dev/null || echo "$body"
    else
        echo -e "${RED}✗ 图片上传失败 (HTTP $http_code)${NC}"
        echo "响应: $body"
        exit 1
    fi
else
    echo -e "${YELLOW}跳过图片上传测试${NC}"
fi

echo ""
echo "========================================="
echo -e "${GREEN}所有测试完成！${NC}"
echo "========================================="
echo ""
echo "App 端调用示例："
echo ""
echo "curl -X POST '$SERVER/api/record/upload' \\"
echo "  -F 'file=@/path/to/image.jpg' \\"
echo "  -F 'userId=your_user_id'"
echo ""