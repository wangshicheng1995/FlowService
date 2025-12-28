#!/bin/zsh

#############################################
# FlowService 批量上传脚本（增强版）
# 功能：批量上传食物图片进行 AI 分析
# 特点：
#   - 固定间隔触发请求（不等待响应）
#   - 并发执行，最后统一收集结果
#   - 支持本地/线上切换
#   - 结果按时间戳保存
#############################################

# ==================== 配置区域 ====================

# 图片目录
IMAGE_DIR="/Users/echo/Downloads/Food"

# 用户 ID
USER_ID="000514.a7d1133e26fa4fc490a32a9fb22abd9a.1422"

# 结果保存目录
RESULT_DIR="/Users/echo/Downloads/BatchUploadResult"

# 服务器配置（默认线上，可通过参数切换）
SERVER_ONLINE="http://139.196.221.226:8080/api"
SERVER_LOCAL="http://localhost:8080/api"
SERVER="$SERVER_ONLINE"

# 请求间隔（秒）
INTERVAL=3

# ==================== 颜色定义 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ==================== 函数定义 ====================

print_info() {
    echo "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo "${RED}[ERROR]${NC} $1"
}

show_help() {
    echo "使用方法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -l, --local           使用本地服务器 (localhost:8080)"
    echo "  -o, --online          使用线上服务器 (默认)"
    echo "  -d, --dir <目录>      指定图片目录 (默认: $IMAGE_DIR)"
    echo "  -u, --user <用户ID>   指定用户 ID"
    echo "  -i, --interval <秒>   请求间隔 (默认: 3)"
    echo "  -n, --dry-run         预览模式，不实际上传"
    echo "  -h, --help            显示帮助"
    echo ""
}

# ==================== 参数解析 ====================

DRY_RUN="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        -l|--local)
            SERVER="$SERVER_LOCAL"
            shift
            ;;
        -o|--online)
            SERVER="$SERVER_ONLINE"
            shift
            ;;
        -d|--dir)
            IMAGE_DIR="$2"
            shift 2
            ;;
        -u|--user)
            USER_ID="$2"
            shift 2
            ;;
        -i|--interval)
            INTERVAL="$2"
            shift 2
            ;;
        -n|--dry-run)
            DRY_RUN="true"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# ==================== 主逻辑 ====================

clear
echo "======================================================"
echo "${GREEN}FlowService 批量上传脚本（增强版）${NC}"
echo "======================================================"
echo ""

# 显示配置
print_info "配置信息:"
echo "  服务器:     $SERVER"
echo "  用户ID:     $USER_ID"
echo "  图片目录:   $IMAGE_DIR"
echo "  请求间隔:   ${INTERVAL}秒"
echo "  结果目录:   $RESULT_DIR"
if [[ "$DRY_RUN" = "true" ]]; then
    echo "  模式:       ${YELLOW}预览模式（不实际上传）${NC}"
fi
echo ""

# 检查图片目录
if [[ ! -d "$IMAGE_DIR" ]]; then
    print_error "图片目录不存在: $IMAGE_DIR"
    exit 1
fi

# 创建结果目录
mkdir -p "$RESULT_DIR"

# 生成本次运行的时间戳
RUN_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RUN_RESULT_DIR="$RESULT_DIR/$RUN_TIMESTAMP"
mkdir -p "$RUN_RESULT_DIR"

print_info "本次结果目录: $RUN_RESULT_DIR"
echo ""

# 收集所有图片文件（使用简单的方式）
IMAGE_FILES=()
for ext in jpg jpeg png webp heic JPG JPEG PNG WEBP HEIC; do
    for f in "$IMAGE_DIR"/*.$ext(N); do
        [[ -f "$f" ]] && IMAGE_FILES+=("$f")
    done
done

TOTAL=${#IMAGE_FILES[@]}

if [[ "$TOTAL" -eq 0 ]]; then
    print_error "目录中没有找到图片文件"
    exit 1
fi

print_success "找到 $TOTAL 张图片"
echo ""

# 列出将要上传的图片
print_info "图片列表:"
INDEX=0
for IMG in "${IMAGE_FILES[@]}"; do
    INDEX=$((INDEX+1))
    FILENAME=$(basename "$IMG")
    echo "  [$INDEX/$TOTAL] $FILENAME"
done
echo ""

# 预览模式下直接退出
if [[ "$DRY_RUN" = "true" ]]; then
    print_warning "预览模式，退出"
    exit 0
fi

# 确认
echo -n "按 Enter 开始上传，或 Ctrl+C 取消..."
read
echo ""

# ==================== 并发上传 ====================

print_info "开始上传..."
echo ""

# 存储进程 ID
typeset -A PID_TO_FILE
typeset -A PID_TO_INDEX
pids=()

START_TIME=$(date +%s)
INDEX=0

for IMG in "${IMAGE_FILES[@]}"; do
    INDEX=$((INDEX+1))
    FILENAME=$(basename "$IMG")
    
    # 结果文件名：序号_原文件名.json
    RESULT_FILE="$RUN_RESULT_DIR/${INDEX}_${FILENAME%.*}.json"
    
    echo "${CYAN}[$INDEX/$TOTAL]${NC} 触发上传: $FILENAME"
    
    # 后台执行 curl，不等待响应
    (
        # 执行上传并保存响应（包含 userId 参数）
        HTTP_CODE=$(curl -s -w "%{http_code}" -o "$RESULT_FILE.tmp" \
            -X POST "$SERVER/record/upload" \
            -F "file=@$IMG" \
            -F "userId=$USER_ID" \
            -H "Content-Type: multipart/form-data")
        
        # 记录完成时间
        END_TIME=$(date +"%Y-%m-%d %H:%M:%S")
        
        # 构建最终结果 JSON
        RESPONSE_BODY=$(cat "$RESULT_FILE.tmp" 2>/dev/null || echo '{}')
        
        cat > "$RESULT_FILE" << EOFINNER
{
  "index": $INDEX,
  "filename": "$FILENAME",
  "filepath": "$IMG",
  "http_code": $HTTP_CODE,
  "completed_at": "$END_TIME",
  "response": $RESPONSE_BODY
}
EOFINNER
        
        # 删除临时文件
        rm -f "$RESULT_FILE.tmp"
        
    ) &
    
    # 记录进程信息
    PID=$!
    pids+=($PID)
    PID_TO_FILE[$PID]="$FILENAME"
    PID_TO_INDEX[$PID]=$INDEX
    
    # 固定间隔（除了最后一张）
    if [[ $INDEX -lt $TOTAL ]]; then
        sleep $INTERVAL
    fi
done

echo ""
print_info "所有请求已触发，等待响应完成..."
echo ""

# ==================== 等待所有请求完成 ====================

COMPLETED=0
SUCCESS_COUNT=0
FAILED_COUNT=0

for pid in "${pids[@]}"; do
    wait $pid
    COMPLETED=$((COMPLETED+1))
    
    FILENAME="${PID_TO_FILE[$pid]}"
    IDX="${PID_TO_INDEX[$pid]}"
    
    # 检查结果文件
    RESULT_FILE="$RUN_RESULT_DIR/${IDX}_${FILENAME%.*}.json"
    
    if [[ -f "$RESULT_FILE" ]]; then
        HTTP_CODE=$(grep -o '"http_code": [0-9]*' "$RESULT_FILE" | grep -o '[0-9]*')
        if [[ "$HTTP_CODE" = "200" ]]; then
            echo "${GREEN}✓${NC} [$IDX] $FILENAME - 成功"
            SUCCESS_COUNT=$((SUCCESS_COUNT+1))
        else
            echo "${RED}✗${NC} [$IDX] $FILENAME - 失败 (HTTP $HTTP_CODE)"
            FAILED_COUNT=$((FAILED_COUNT+1))
        fi
    else
        echo "${RED}✗${NC} [$IDX] $FILENAME - 无响应"
        FAILED_COUNT=$((FAILED_COUNT+1))
    fi
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "======================================================"
print_success "批量上传完成！"
echo "======================================================"
echo ""
echo "统计信息:"
echo "  总数:       $TOTAL"
echo "  成功:       ${GREEN}$SUCCESS_COUNT${NC}"
echo "  失败:       ${RED}$FAILED_COUNT${NC}"
echo "  耗时:       ${DURATION}秒"
echo ""
echo "结果文件目录:"
echo "  $RUN_RESULT_DIR"
echo ""

# 生成汇总报告
SUMMARY_FILE="$RUN_RESULT_DIR/_summary.json"
cat > "$SUMMARY_FILE" << EOFSUMMARY
{
  "run_timestamp": "$RUN_TIMESTAMP",
  "server": "$SERVER",
  "user_id": "$USER_ID",
  "image_dir": "$IMAGE_DIR",
  "interval_seconds": $INTERVAL,
  "total_images": $TOTAL,
  "success_count": $SUCCESS_COUNT,
  "failed_count": $FAILED_COUNT,
  "duration_seconds": $DURATION,
  "completed_at": "$(date '+%Y-%m-%d %H:%M:%S')"
}
EOFSUMMARY

print_info "汇总报告已生成: $SUMMARY_FILE"
echo ""

# 询问是否查看结果
echo -n "是否打开结果目录？(y/n): "
read REPLY
if [[ "$REPLY" =~ ^[Yy]$ ]]; then
    open "$RUN_RESULT_DIR"
fi
