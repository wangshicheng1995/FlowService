#!/bin/bash

#############################################
# FlowService 自动部署脚本 for Mac（保留版本号）
# 功能：停止旧服务 → 清理文件 → 上传新版本 → 启动服务 → 查看日志
# 使用：双击运行或执行 ./deploy-flowservice.command
# 更新：保留 JAR 包版本号，使用符号链接
#############################################

set -e

# ==================== 配置区域 ====================
# 请根据实际情况修改以下配置

# 服务器信息
SERVER_IP="139.196.221.226"              # 例如: 47.98.123.45
SERVER_USER="root"                    # SSH 用户名，通常是 root

# 本地 JAR 包路径
LOCAL_JAR_DIR="/Users/echo/Downloads/playground/FlowService/target"

# 服务器路径
SERVER_JAR_DIR="/opt/flowservice"     # 服务器上 JAR 包存放目录
SERVICE_NAME="flowservice"            # systemd 服务名称

# 历史版本保留数量（0 表示保留所有）
KEEP_VERSIONS=1                       # 保留最近 1 个版本

# 服务停止等待时间（秒）
STOP_WAIT_TIMEOUT=60                  # 最大等待 60 秒

# ==================== 数据库更新配置 ====================
# 设置为 true 时会自动执行 init.sql 更新数据库结构
# 设置为 false 时跳过数据库更新
UPDATE_DATABASE=true

# 本地 init.sql 路径
LOCAL_INIT_SQL="/Users/echo/Downloads/playground/FlowService/src/main/resources/db/init.sql"

# 服务器上 init.sql 临时存放路径
SERVER_INIT_SQL="/tmp/init.sql"

# MySQL 用户名和密码
MYSQL_USER="root"
MYSQL_PASSWORD="root"

# ==================== 颜色定义 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# ==================== 函数定义 ====================

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_db() {
    echo -e "${PURPLE}[DATABASE]${NC} $1"
}

# 打印分隔线
print_separator() {
    echo "=================================================="
}

# ==================== 开始部署 ====================

clear
print_separator
echo -e "${GREEN}FlowService 自动部署脚本（版本保留模式）${NC}"
print_separator
echo ""

# 显示数据库更新状态
if [ "$UPDATE_DATABASE" == "true" ]; then
    print_db "数据库更新: ${GREEN}已启用${NC} - 本次部署将执行 init.sql"
else
    print_db "数据库更新: ${YELLOW}已禁用${NC} - 本次部署将跳过数据库更新"
fi
echo ""

# 步骤 1: 检查配置
print_info "步骤 1/8: 检查配置..."

if [ "$SERVER_IP" == "你的服务器IP" ]; then
    print_error "请先在脚本中配置 SERVER_IP（服务器 IP 地址）"
    echo ""
    echo "打开脚本文件，修改第 11 行："
    echo "SERVER_IP=\"你的服务器IP\"  →  SERVER_IP=\"47.98.123.45\""
    echo ""
    read -p "按 Enter 键退出..." -r
    exit 1
fi

print_success "服务器 IP: $SERVER_IP"
echo ""

# 自动编译与版本递增
print_info "正在处理版本号并编译..."
export JAVA_HOME="/Users/echo/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home"
cd "/Users/echo/Downloads/playground/FlowService"

# 1. 获取当前版本
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
print_info "当前版本: $CURRENT_VERSION"

# 2. 递增版本号 (使用 build-helper-maven-plugin)
# 这会将 x.y.z 自动更新为 x.y.(z+1)
print_info "正在递增版本号..."
mvn build-helper:parse-version versions:set \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion} \
    -DgenerateBackupPoms=false

# 3. 获取新版本号用于展示
NEW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
print_success "版本已更新: $CURRENT_VERSION -> $NEW_VERSION"

# 4. 执行打包
print_info "开始打包..."
if mvn clean package -DskipTests; then
    print_success "编译打包成功"
    echo ""
else
    print_error "编译失败"
    # 如果编译失败，尝试回滚版本（可选，这里简单处理提示用户）
    print_warning "提示：版本号已修改为 $NEW_VERSION，如需回滚请手动处理 pom.xml"
    read -p "按 Enter 键退出..." -r
    exit 1
fi

# 步骤 2: 查找本地 JAR 包
print_info "步骤 2/8: 查找本地 JAR 包..."

if [ ! -d "$LOCAL_JAR_DIR" ]; then
    print_error "本地目录不存在: $LOCAL_JAR_DIR"
    echo ""
    read -p "按 Enter 键退出..." -r
    exit 1
fi

# 查找最新的 JAR 包（按修改时间排序），排除 original
LOCAL_JAR=$(find "$LOCAL_JAR_DIR" -name "flowservice-*.jar" -type f | grep -v 'original' | sort -r | head -n 1)

if [ -z "$LOCAL_JAR" ]; then
    print_error "在 $LOCAL_JAR_DIR 中未找到 flowservice-*.jar 文件"
    echo ""
    echo "请先编译项目："
    echo "  cd /Users/echo/Downloads/playground/FlowService"
    echo "  mvn clean package -DskipTests"
    echo ""
    read -p "按 Enter 键退出..." -r
    exit 1
fi

JAR_FILENAME=$(basename "$LOCAL_JAR")
JAR_SIZE=$(du -h "$LOCAL_JAR" | cut -f1)

# 提取版本号（例如 flowservice-1.0.1.jar → 1.0.1）
VERSION=$(echo "$JAR_FILENAME" | sed -n 's/flowservice-\(.*\)\.jar/\1/p')

if [ -z "$VERSION" ]; then
    print_error "无法从文件名中提取版本号: $JAR_FILENAME"
    echo "文件名格式应为: flowservice-x.y.z.jar"
    echo ""
    read -p "按 Enter 键退出..." -r
    exit 1
fi

print_success "找到 JAR 包: $JAR_FILENAME (版本: $VERSION, 大小: $JAR_SIZE)"
echo ""

# 步骤 3: 测试服务器连接
print_info "步骤 3/8: 测试服务器连接..."

if ! ssh -o ConnectTimeout=5 -o BatchMode=yes "$SERVER_USER@$SERVER_IP" "echo 连接成功" > /dev/null 2>&1; then
    print_error "无法连接到服务器 $SERVER_IP"
    echo ""
    echo "可能的原因："
    echo "1. 服务器 IP 地址错误"
    echo "2. 服务器未开启或防火墙阻止"
    echo "3. SSH 密钥未配置（需要输入密码）"
    echo ""
    echo "尝试手动连接测试："
    echo "  ssh $SERVER_USER@$SERVER_IP"
    echo ""
    read -p "按 Enter 键退出..." -r
    exit 1
fi

print_success "服务器连接正常"
echo ""

# 步骤 3.5: 检查数据库状态
print_info "步骤 3.5: 检查数据库状态..."

# 检查 MySQL/MariaDB 服务状态
DB_STATUS=$(ssh "$SERVER_USER@$SERVER_IP" "systemctl is-active mysql || systemctl is-active mysqld || systemctl is-active mariadb || echo 'inactive'")

if [ "$DB_STATUS" == "active" ]; then
    print_success "数据库服务运行正常"
else
    print_warning "数据库服务似乎未运行 (状态: $DB_STATUS)"
    print_warning "尝试启动数据库..."
    ssh "$SERVER_USER@$SERVER_IP" "systemctl start mysql || systemctl start mysqld || systemctl start mariadb"
    sleep 5
fi

# 检查 3306 端口
PORT_CHECK=$(ssh "$SERVER_USER@$SERVER_IP" "netstat -tln | grep 3306 || echo 'no_port'")
if [ "$PORT_CHECK" == "no_port" ]; then
    print_error "检测到 3306 端口未开启！应用可能无法连接数据库。"
    echo "请登录服务器检查 MySQL 配置 (bind-address) 或防火墙设置。"
    read -p "是否继续部署？(y/n): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    print_success "3306 端口监听正常"
fi
echo ""

# 步骤 4: 停止旧服务
print_info "步骤 4/8: 停止旧服务..."

# 检查服务是否存在
SERVICE_EXISTS=$(ssh "$SERVER_USER@$SERVER_IP" "systemctl list-units --full --all | grep -c '$SERVICE_NAME.service' || true")

if [ "$SERVICE_EXISTS" -gt 0 ]; then
    # 检查服务状态
    SERVICE_STATUS=$(ssh "$SERVER_USER@$SERVER_IP" "systemctl is-active $SERVICE_NAME || echo 'inactive'")
    
    if [ "$SERVICE_STATUS" == "active" ]; then
        print_warning "检测到服务正在运行，准备停止..."
        
        # 使用单次 SSH 连接执行停止和等待（避免多次 SSH 连接开销）
        STOP_RESULT=$(ssh "$SERVER_USER@$SERVER_IP" "
            systemctl stop $SERVICE_NAME
            
            # 在远程服务器上循环检查，最多等待 30 秒
            for i in \$(seq 1 15); do
                STATUS=\$(systemctl is-active $SERVICE_NAME 2>/dev/null || echo 'inactive')
                if [ \"\$STATUS\" == 'inactive' ] || [ \"\$STATUS\" == 'failed' ]; then
                    echo 'STOPPED'
                    exit 0
                fi
                sleep 2
            done
            echo 'TIMEOUT'
        ")
        
        if [ "$STOP_RESULT" == "STOPPED" ]; then
            print_success "服务已停止"
        else
            print_warning "服务停止超时，尝试强制终止..."
            
            # 使用更精确的进程匹配，只杀掉 flowservice.jar 进程
            ssh "$SERVER_USER@$SERVER_IP" "
                # 获取 flowservice 进程 PID
                PID=\$(pgrep -f '/opt/flowservice/flowservice.jar' || true)
                if [ -n \"\$PID\" ]; then
                    kill -9 \$PID 2>/dev/null || true
                    sleep 2
                fi
            "
            
            # 再次检查
            FINAL_STATUS=$(ssh "$SERVER_USER@$SERVER_IP" "systemctl is-active $SERVICE_NAME || echo 'inactive'")
            if [ "$FINAL_STATUS" == "inactive" ] || [ "$FINAL_STATUS" == "failed" ]; then
                print_success "服务已强制停止"
            else
                print_error "无法停止服务，请手动检查服务器状态"
                read -p "按 Enter 键退出..." -r
                exit 1
            fi
        fi
    else
        print_info "服务未运行（状态: $SERVICE_STATUS），无需停止"
    fi
else
    print_info "服务不存在（首次部署）"
fi

echo ""

# ==================== 数据库更新步骤 ====================
if [ "$UPDATE_DATABASE" == "true" ]; then
    print_separator
    print_db "开始执行数据库更新..."
    print_separator
    echo ""
    
    # 检查本地 init.sql 是否存在
    if [ ! -f "$LOCAL_INIT_SQL" ]; then
        print_error "本地 init.sql 文件不存在: $LOCAL_INIT_SQL"
        read -p "是否跳过数据库更新继续部署？(y/n): " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
        print_warning "跳过数据库更新，继续部署..."
    else
        print_db "找到 init.sql 文件: $LOCAL_INIT_SQL"
        
        # 上传 init.sql 到服务器
        print_db "正在上传 init.sql 到服务器..."
        scp "$LOCAL_INIT_SQL" "$SERVER_USER@$SERVER_IP:$SERVER_INIT_SQL"
        print_success "init.sql 已上传到服务器: $SERVER_INIT_SQL"
        
        # 执行 init.sql
        print_db "正在执行 init.sql 更新数据库结构..."
        
        if ssh "$SERVER_USER@$SERVER_IP" "mysql -u $MYSQL_USER -p'$MYSQL_PASSWORD' < $SERVER_INIT_SQL 2>/dev/null"; then
            print_success "数据库更新成功！"
            
            # 清理临时文件
            ssh "$SERVER_USER@$SERVER_IP" "rm -f $SERVER_INIT_SQL"
            print_db "已清理服务器临时文件"
        else
            print_error "数据库更新失败！"
            read -p "是否继续部署？(y/n): " -n 1 -r
            echo ""
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
            print_warning "继续部署，但数据库可能未正确更新"
        fi
    fi
    
    echo ""
    print_separator
    print_db "数据库更新步骤完成"
    print_separator
    echo ""
else
    print_db "UPDATE_DATABASE=false，跳过数据库更新步骤"
    echo ""
fi

# 步骤 5: 检查服务器目录和历史版本
print_info "步骤 5/8: 检查服务器目录..."

# 检查目录是否存在
DIR_EXISTS=$(ssh "$SERVER_USER@$SERVER_IP" "[ -d '$SERVER_JAR_DIR' ] && echo 'yes' || echo 'no'")

if [ "$DIR_EXISTS" == "no" ]; then
    print_info "目录不存在，创建目录: $SERVER_JAR_DIR"
    ssh "$SERVER_USER@$SERVER_IP" "mkdir -p $SERVER_JAR_DIR"
fi

# 列出现有版本
EXISTING_VERSIONS=$(ssh "$SERVER_USER@$SERVER_IP" "ls -1 $SERVER_JAR_DIR/flowservice-*.jar 2>/dev/null | sort -V || true")

if [ -n "$EXISTING_VERSIONS" ]; then
    print_info "服务器现有版本："
    echo "$EXISTING_VERSIONS" | while read -r jar; do
        if [ -n "$jar" ]; then
            jar_basename=$(basename "$jar")
            # 检查是否为当前符号链接指向的版本
            CURRENT_LINK=$(ssh "$SERVER_USER@$SERVER_IP" "readlink $SERVER_JAR_DIR/flowservice.jar 2>/dev/null || echo ''")
            if [ "$(basename "$CURRENT_LINK")" == "$jar_basename" ]; then
                echo "  - $jar_basename ${GREEN}(当前)${NC}"
            else
                echo "  - $jar_basename"
            fi
        fi
    done
else
    print_info "服务器上无历史版本"
fi

echo ""

# 步骤 6: 上传新 JAR 包
print_info "步骤 6/8: 上传新 JAR 包到服务器..."

# 检查服务器上是否已存在该版本
VERSION_EXISTS=$(ssh "$SERVER_USER@$SERVER_IP" "[ -f '$SERVER_JAR_DIR/$JAR_FILENAME' ] && echo 'yes' || echo 'no'")

if [ "$VERSION_EXISTS" == "yes" ]; then
    print_warning "服务器上已存在相同版本: $JAR_FILENAME"
    read -p "是否覆盖？(y/n): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "取消部署"
        read -p "按 Enter 键退出..." -r
        exit 0
    fi
    print_info "准备覆盖现有版本..."
fi

# 上传文件并显示进度（保留原文件名）
echo ""
scp "$LOCAL_JAR" "$SERVER_USER@$SERVER_IP:$SERVER_JAR_DIR/$JAR_FILENAME"

echo ""
print_success "JAR 包上传完成: $JAR_FILENAME"

# 设置文件权限
ssh "$SERVER_USER@$SERVER_IP" "chown flowservice:flowservice $SERVER_JAR_DIR/$JAR_FILENAME"
print_success "文件权限已设置"

echo ""

# 步骤 7: 更新符号链接
print_info "步骤 7/8: 更新符号链接..."

# 删除旧的符号链接（如果存在）
ssh "$SERVER_USER@$SERVER_IP" "rm -f $SERVER_JAR_DIR/flowservice.jar"

# 创建新的符号链接
ssh "$SERVER_USER@$SERVER_IP" "ln -s $SERVER_JAR_DIR/$JAR_FILENAME $SERVER_JAR_DIR/flowservice.jar"

# 验证符号链接
LINK_TARGET=$(ssh "$SERVER_USER@$SERVER_IP" "readlink $SERVER_JAR_DIR/flowservice.jar")
print_success "符号链接已创建: flowservice.jar -> $(basename "$LINK_TARGET")"

# 清理旧版本（如果配置了保留数量）
if [ "$KEEP_VERSIONS" -gt 0 ]; then
    print_info "清理旧版本（保留最近 $KEEP_VERSIONS 个版本）..."
    
    # 获取所有版本，按时间排序（最新的在前）
    ALL_VERSIONS=$(ssh "$SERVER_USER@$SERVER_IP" "ls -1t $SERVER_JAR_DIR/flowservice-*.jar 2>/dev/null || true")
    
    if [ -n "$ALL_VERSIONS" ]; then
        VERSION_COUNT=$(echo "$ALL_VERSIONS" | wc -l | tr -d ' ')
        
        if [ "$VERSION_COUNT" -gt "$KEEP_VERSIONS" ]; then
            # 跳过前 N 个，删除其余的
            VERSIONS_TO_DELETE=$(echo "$ALL_VERSIONS" | tail -n +$((KEEP_VERSIONS + 1)))
            
            if [ -n "$VERSIONS_TO_DELETE" ]; then
                print_warning "准备删除以下旧版本："
                echo "$VERSIONS_TO_DELETE" | while read -r old_jar; do
                    if [ -n "$old_jar" ]; then
                        echo "  - $(basename "$old_jar")"
                    fi
                done
                
                # 执行删除
                echo "$VERSIONS_TO_DELETE" | while read -r old_jar; do
                    if [ -n "$old_jar" ]; then
                        ssh "$SERVER_USER@$SERVER_IP" "rm -f '$old_jar'"
                    fi
                done
                
                print_success "旧版本已清理"
            fi
        else
            print_info "版本数量未超过保留限制，无需清理"
        fi
    fi
fi

echo ""

# 步骤 8: 启动服务
print_info "步骤 8/8: 启动服务..."

# 重新加载 systemd 配置
ssh "$SERVER_USER@$SERVER_IP" "systemctl daemon-reload"

# 启动服务
ssh "$SERVER_USER@$SERVER_IP" "systemctl start $SERVICE_NAME"

# 等待服务启动
print_info "等待服务启动（5秒）..."
sleep 5

# 检查服务状态
SERVICE_STATUS=$(ssh "$SERVER_USER@$SERVER_IP" "systemctl is-active $SERVICE_NAME || echo 'failed'")

if [ "$SERVICE_STATUS" == "active" ]; then
    print_success "服务启动成功！"
else
    print_error "服务启动失败"
    echo ""
    echo "查看错误日志："
    ssh "$SERVER_USER@$SERVER_IP" "journalctl -u $SERVICE_NAME -n 50 --no-pager"
    echo ""
    read -p "按 Enter 键退出..." -r
    exit 1
fi

echo ""

# 验证应用是否正常（测试健康检查接口）
print_info "验证应用健康状态..."
sleep 3

HEALTH_CHECK=$(ssh "$SERVER_USER@$SERVER_IP" "curl -s http://localhost:8080/api/home/health" || echo "fail")

if echo "$HEALTH_CHECK" | grep -q "200"; then
    print_success "应用健康检查通过！"
    echo ""
    echo "访问地址："
    echo "  http://$SERVER_IP:8080/api/home/health"
else
    print_warning "健康检查未通过，但服务已启动"
    echo "可能原因：应用还在初始化，稍后会恢复正常"
fi

echo ""
print_separator
print_success "部署完成！当前版本: $VERSION"
print_separator
echo ""

# 显示当前部署信息
echo "部署信息："
echo "  版本号:     $VERSION"
echo "  文件名:     $JAR_FILENAME"
echo "  符号链接:   flowservice.jar -> $JAR_FILENAME"
if [ "$UPDATE_DATABASE" == "true" ]; then
    echo -e "  数据库更新: ${GREEN}已执行${NC}"
else
    echo -e "  数据库更新: ${YELLOW}已跳过${NC}"
fi
echo ""

# 显示服务器上的所有版本
echo "服务器版本列表："
FINAL_VERSIONS=$(ssh "$SERVER_USER@$SERVER_IP" "ls -1t $SERVER_JAR_DIR/flowservice-*.jar 2>/dev/null || true")
if [ -n "$FINAL_VERSIONS" ]; then
    echo "$FINAL_VERSIONS" | while read -r jar; do
        if [ -n "$jar" ]; then
            jar_basename=$(basename "$jar")
            jar_version=$(echo "$jar_basename" | sed -n 's/flowservice-\(.*\)\.jar/\1/p')
            jar_size=$(ssh "$SERVER_USER@$SERVER_IP" "du -h '$jar' | cut -f1")
            
            # 标记当前版本
            if [ "$jar_version" == "$VERSION" ]; then
                echo "  - $jar_basename (${jar_size}) ${GREEN}← 当前${NC}"
            else
                echo "  - $jar_basename (${jar_size})"
            fi
        fi
    done
else
    echo "  无版本信息"
fi

echo ""

# 提示查看日志
echo "接下来会实时显示应用日志（按 Ctrl+C 退出）"
echo ""
echo "其他常用命令："
echo "  查看服务状态:  ssh $SERVER_USER@$SERVER_IP 'systemctl status $SERVICE_NAME'"
echo "  停止服务:      ssh $SERVER_USER@$SERVER_IP 'systemctl stop $SERVICE_NAME'"
echo "  重启服务:      ssh $SERVER_USER@$SERVER_IP 'systemctl restart $SERVICE_NAME'"
echo "  查看版本:      ssh $SERVER_USER@$SERVER_IP 'ls -lh $SERVER_JAR_DIR/flowservice*.jar'"
echo "  切换版本:      ssh $SERVER_USER@$SERVER_IP 'ln -sf $SERVER_JAR_DIR/flowservice-x.y.z.jar $SERVER_JAR_DIR/flowservice.jar && systemctl restart $SERVICE_NAME'"
echo ""

read -p "按 Enter 键开始查看实时日志..." -r
echo ""

# 显示实时日志
print_info "开始显示实时日志..."
print_separator
echo ""

ssh "$SERVER_USER@$SERVER_IP" "journalctl -u $SERVICE_NAME -f"
