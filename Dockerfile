# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建日志目录
RUN mkdir -p /var/log/flowservice && \
    addgroup -S flowservice && \
    adduser -S flowservice -G flowservice && \
    chown -R flowservice:flowservice /var/log/flowservice

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/flowservice-*.jar /app/flowservice.jar

# 切换到非 root 用户
USER flowservice

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/status/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/flowservice.jar", "--spring.profiles.active=prod"]