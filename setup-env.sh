#!/bin/bash
# FlowService 环境配置脚本
# 用途：设置 Java 17 环境变量并验证配置

echo "🔧 配置 FlowService 开发环境..."
echo ""

# 设置 Java 17
export JAVA_HOME=/Users/echo/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 验证 Java 版本
echo "✅ Java 版本:"
java -version
echo ""

# 验证 Maven 版本
echo "✅ Maven 版本:"
mvn -version
echo ""

# 提示
echo "🎉 环境配置完成！"
echo ""
echo "📝 使用方法："
echo "   source setup-env.sh"
echo ""
echo "🚀 快速命令："
echo "   mvn clean test              # 运行测试"
echo "   mvn spring-boot:run         # 启动应用（需要 MySQL）"
echo "   mvn clean package          # 打包应用"
echo ""
echo "💡 提示：要永久生效，请将以下内容添加到 ~/.zshrc 或 ~/.bash_profile："
echo "   export JAVA_HOME=/Users/echo/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home"
echo "   export PATH=\$JAVA_HOME/bin:\$PATH"
