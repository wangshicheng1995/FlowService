#!/bin/bash

#############################################
# FlowService 数据库一键安装脚本
# 功能：自动安装 MySQL/MariaDB，设置密码，初始化表结构
#############################################

set -e

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# ==================== 配置区域 ====================
SERVER_IP="139.196.221.226"
SERVER_USER="root"
DB_ROOT_PASSWORD="root" # 与 application.yml 保持一致
INIT_SQL_PATH="$SCRIPT_DIR/src/main/resources/db/init.sql"

# ==================== 颜色定义 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ==================== 开始执行 ====================

clear
echo -e "${GREEN}FlowService 数据库一键初始化脚本${NC}"
echo "=================================================="
echo "目标服务器: $SERVER_IP"
echo "数据库密码: $DB_ROOT_PASSWORD"
echo "=================================================="
echo ""

# 1. 检查本地 SQL 文件
if [ ! -f "$INIT_SQL_PATH" ]; then
    print_error "找不到初始化 SQL 文件: $INIT_SQL_PATH"
    exit 1
fi

# 2. 生成远程安装脚本
print_info "生成远程安装脚本..."
cat > install_mysql_remote.sh <<EOF
#!/bin/bash
set -e

GREEN='\033[0;32m'
NC='\033[0m'

echo "Checking OS..."
if [ -f /etc/debian_version ]; then
    OS="debian"
    PKG_MANAGER="apt-get"
elif [ -f /etc/redhat-release ]; then
    OS="centos"
    PKG_MANAGER="yum"
else
    echo "Unsupported OS"
    exit 1
fi

echo "Detected OS: \$OS"

# 安装 MySQL/MariaDB
if ! command -v mysql &> /dev/null; then
    echo "Installing MySQL/MariaDB..."
    if [ "\$OS" == "debian" ]; then
        \$PKG_MANAGER update
        \$PKG_MANAGER install -y mariadb-server
    elif [ "\$OS" == "centos" ]; then
        \$PKG_MANAGER install -y mariadb-server
    fi
else
    echo "MySQL/MariaDB already installed."
fi

# 启动服务
echo "Starting Database Service..."
systemctl start mariadb || systemctl start mysql || systemctl start mysqld
systemctl enable mariadb || systemctl enable mysql || systemctl enable mysqld

# 等待启动
sleep 5

# 设置 root 密码 & 允许远程连接 (如果需要)
# 注意：这里为了简单直接重置 root 密码，生产环境请更谨慎
echo "Configuring Database Security..."

# 尝试免密登录设置密码 (针对新安装)
mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$DB_ROOT_PASSWORD';" 2>/dev/null || \
mysql -e "SET PASSWORD FOR 'root'@'localhost' = PASSWORD('$DB_ROOT_PASSWORD');" 2>/dev/null || \
mysql -u root -p'$DB_ROOT_PASSWORD' -e "SELECT 1;" 2>/dev/null || \
echo "Warning: Could not set root password automatically. It might be already set."

# 开放 3306 端口 (如果防火墙开启)
if command -v ufw &> /dev/null; then
    ufw allow 3306/tcp
elif command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=3306/tcp
    firewall-cmd --reload
fi

echo -e "\${GREEN}Database installed and configured.\${NC}"
EOF

# 3. 上传文件到服务器
print_info "上传安装脚本和 SQL 文件..."
scp install_mysql_remote.sh "$SERVER_USER@$SERVER_IP:/tmp/install_mysql_remote.sh"
scp "$INIT_SQL_PATH" "$SERVER_USER@$SERVER_IP:/tmp/init.sql"

# 4. 执行远程安装
print_info "开始在服务器上执行安装..."
ssh "$SERVER_USER@$SERVER_IP" "bash /tmp/install_mysql_remote.sh"

# 5. 执行 SQL 初始化
print_info "执行数据库初始化 SQL..."
ssh "$SERVER_USER@$SERVER_IP" "mysql -u root -p'$DB_ROOT_PASSWORD' < /tmp/init.sql"

# 6. 清理临时文件
print_info "清理临时文件..."
rm install_mysql_remote.sh
ssh "$SERVER_USER@$SERVER_IP" "rm /tmp/install_mysql_remote.sh /tmp/init.sql"

echo ""
print_success "数据库安装与初始化完成！"
print_success "您现在可以运行 deploy.command 部署应用了。"
