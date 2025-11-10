# FlowService 服务器部署检查清单

## 一、当前状态分析

### ✅ 已具备的功能（可以直接使用）

1. **核心接口完整**
   - ✅ POST `/api/image/upload` - 图片上传接口（支持 multipart/form-data）
   - ✅ POST `/api/image/process` - Base64 图片处理接口
   - ✅ GET `/api/status/health` - 健康检查接口
   - ✅ GET `/api/status/info` - 服务信息接口

2. **跨域配置完善**
   - ✅ 已配置 CORS，允许所有来源访问
   - ✅ 支持 GET、POST、PUT、DELETE、OPTIONS 方法
   - ✅ 允许所有请求头

3. **文件上传支持**
   - ✅ 支持最大 10MB 文件上传
   - ✅ 支持 HEIC、JPG、PNG、GIF 等格式

4. **错误处理完善**
   - ✅ 全局异常处理器
   - ✅ 统一的 API 响应格式
   - ✅ 详细的错误日志

5. **通义千问 AI 集成**
   - ✅ 使用官方 SDK
   - ✅ 支持图片+文本多模态输入

---

## 二、App 端调用测试

### 接口访问地址
假设服务器 IP 为 `192.168.1.100`，端口为 `8080`：

```
完整 URL: http://192.168.1.100:8080/api/image/upload
```

### 请求示例

#### 方式1：使用 multipart/form-data（推荐 App 使用）
```bash
curl -X POST "http://your-server:8080/api/image/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@image.jpg" \
  -F "prompt=请详细描述这张图片"
```

#### 方式2：使用 JSON + Base64
```bash
curl -X POST "http://your-server:8080/api/image/process" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "请描述这张图片",
    "imageBase64": "base64-encoded-image-data",
    "fileName": "test.jpg",
    "mimeType": "image/jpeg"
  }'
```

### 响应格式
```json
{
  "code": 200,
  "message": "图片处理成功",
  "data": {
    "taskId": "uuid-string",
    "originalPrompt": "请描述这张图片",
    "processedText": "AI 分析的完整文本",
    "summary": "摘要内容",
    "metadata": {
      "fileName": "image.jpg",
      "mimeType": "image/jpeg",
      "fileSize": 1234567,
      "model": "qwen-vl-plus",
      "tokensUsed": 150,
      "processingTimeMs": 2500
    },
    "processedAt": "2025-11-10T13:00:00"
  }
}
```

---

## 三、⚠️ 部署到服务器前必须修改的配置

### 1. 生产环境配置文件 ⭐⭐⭐⭐⭐（必须）

**问题**: 当前只有 `application.yml`，没有生产环境配置

**创建**: `src/main/resources/application-prod.yml`
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
  application:
    name: flowservice

# 生产环境 - API Key 必须通过环境变量注入
qwen:
  api:
    key: ${DASHSCOPE_API_KEY}
    model: qwen-vl-plus

# 生产日志配置
logging:
  level:
    com.flowservice: INFO
    org.springframework.web: WARN
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/flowservice/application.log
    max-size: 100MB
    max-history: 30
```

**启动命令**:
```bash
java -jar flowservice.jar --spring.profiles.active=prod
```

---

### 2. API Key 安全配置 ⭐⭐⭐⭐⭐（必须）

**问题**: 当前 API Key 硬编码在配置文件中

**解决方案**:

#### 方式1：环境变量（推荐）
```bash
export DASHSCOPE_API_KEY="sk-your-actual-api-key"
java -jar flowservice.jar --spring.profiles.active=prod
```

#### 方式2：启动参数
```bash
java -jar flowservice.jar \
  --spring.profiles.active=prod \
  --qwen.api.key=sk-your-actual-api-key
```

#### 方式3：外部配置文件
创建 `/etc/flowservice/application.yml`:
```yaml
qwen:
  api:
    key: sk-your-actual-api-key
```

启动时指定：
```bash
java -jar flowservice.jar \
  --spring.profiles.active=prod \
  --spring.config.location=/etc/flowservice/
```

---

### 3. CORS 安全配置 ⭐⭐⭐⭐（建议）

**问题**: 当前允许所有来源访问，存在安全风险

**修改**: `src/main/java/com/flowservice/config/WebConfig.java`

```java
@Configuration
public class WebConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // 生产环境应指定具体的 App 域名
        if ("*".equals(allowedOrigins)) {
            config.setAllowedOriginPatterns(Arrays.asList("*"));
        } else {
            config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

在 `application-prod.yml` 中配置：
```yaml
app:
  cors:
    allowed-origins: https://your-app-domain.com,https://another-domain.com
```

---

### 4. 日志输出到文件 ⭐⭐⭐⭐（建议）

**问题**: 当前日志仅输出到控制台

**修改**: 在 `application-prod.yml` 中添加
```yaml
logging:
  file:
    name: /var/log/flowservice/application.log
    max-size: 100MB
    max-history: 30
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**创建日志目录**:
```bash
sudo mkdir -p /var/log/flowservice
sudo chown your-user:your-user /var/log/flowservice
```

---

### 5. 防火墙和端口配置 ⭐⭐⭐⭐⭐（必须）

**需要开放的端口**: 8080

#### Ubuntu/Debian
```bash
sudo ufw allow 8080/tcp
sudo ufw reload
```

#### CentOS/RHEL
```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

---

### 6. 系统服务配置 ⭐⭐⭐⭐（建议）

**创建 systemd 服务**: `/etc/systemd/system/flowservice.service`

```ini
[Unit]
Description=FlowService - Image Processing with Qwen AI
After=network.target

[Service]
Type=simple
User=flowservice
Group=flowservice
WorkingDirectory=/opt/flowservice
Environment="DASHSCOPE_API_KEY=sk-your-actual-api-key"
Environment="JAVA_OPTS=-Xms512m -Xmx1024m"
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/flowservice/flowservice.jar --spring.profiles.active=prod
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

**使用方法**:
```bash
# 启用服务
sudo systemctl enable flowservice

# 启动服务
sudo systemctl start flowservice

# 查看状态
sudo systemctl status flowservice

# 查看日志
sudo journalctl -u flowservice -f
```

---

### 7. Nginx 反向代理（可选，推荐） ⭐⭐⭐

**创建 Nginx 配置**: `/etc/nginx/sites-available/flowservice`

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 客户端上传大小限制
    client_max_body_size 10M;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置（AI 处理可能需要较长时间）
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

**启用配置**:
```bash
sudo ln -s /etc/nginx/sites-available/flowservice /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

**App 端访问地址**: `http://your-domain.com/api/image/upload`

---

### 8. HTTPS 配置（生产环境强烈推荐） ⭐⭐⭐⭐⭐

**使用 Let's Encrypt 免费证书**:

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

Nginx 配置会自动更新，App 端访问：`https://your-domain.com/api/image/upload`

---

## 四、缺少的功能和文件清单

### ⚠️ 必须创建的文件

| 文件 | 优先级 | 说明 | 状态 |
|------|--------|------|------|
| `application-prod.yml` | ⭐⭐⭐⭐⭐ | 生产环境配置 | ❌ 缺失 |
| `Dockerfile` | ⭐⭐⭐⭐ | Docker 容器化支持 | ❌ 缺失 |
| `docker-compose.yml` | ⭐⭐⭐ | Docker 编排配置 | ❌ 缺失 |
| `deploy.sh` | ⭐⭐⭐⭐ | 部署脚本 | ❌ 缺失 |
| `.env.example` | ⭐⭐⭐⭐ | 环境变量示例 | ❌ 缺失 |
| `flowservice.service` | ⭐⭐⭐⭐ | systemd 服务配置 | ❌ 缺失 |

### ⚠️ 建议添加的功能

| 功能 | 优先级 | 说明 | 状态 |
|------|--------|------|------|
| 单元测试 | ⭐⭐⭐ | Controller/Service 层测试 | ❌ 缺失 |
| 接口文档 | ⭐⭐⭐⭐ | Swagger/OpenAPI 文档 | ❌ 缺失 |
| 请求限流 | ⭐⭐⭐⭐ | 防止 API 滥用 | ❌ 缺失 |
| 监控告警 | ⭐⭐⭐ | Prometheus/Grafana | ❌ 缺失 |
| API 密钥认证 | ⭐⭐⭐⭐ | App 端访问鉴权 | ❌ 缺失 |

---

## 五、App 端调用注意事项

### 1. 请求头设置
```
Content-Type: multipart/form-data
```

### 2. 超时时间
建议设置较长的超时时间（30-60秒），因为 AI 处理需要时间。

### 3. 错误处理
App 端应处理以下错误码：
- `400`: 参数错误（文件为空、格式不支持等）
- `500`: 服务内部错误（API 调用失败等）

### 4. 图片格式
支持的格式：
- ✅ JPG/JPEG
- ✅ PNG
- ✅ GIF
- ✅ HEIC（苹果格式）
- ❌ 大小限制：10MB

### 5. 网络要求
- 服务器需要能访问阿里云通义千问 API（`dashscope.aliyuncs.com`）
- 确保服务器出站网络正常

---

## 六、快速部署步骤（生产环境）

### Step 1: 编译打包
```bash
mvn clean package -DskipTests
```
生成文件：`target/flowservice-1.0.0.jar`

### Step 2: 上传到服务器
```bash
scp target/flowservice-1.0.0.jar user@server:/opt/flowservice/
```

### Step 3: 设置环境变量
```bash
export DASHSCOPE_API_KEY="sk-your-actual-api-key"
```

### Step 4: 启动服务
```bash
cd /opt/flowservice
nohup java -jar flowservice-1.0.0.jar --spring.profiles.active=prod > output.log 2>&1 &
```

### Step 5: 验证服务
```bash
curl http://localhost:8080/api/status/health
```

### Step 6: App 端测试
```bash
curl -X POST "http://your-server:8080/api/image/upload" \
  -F "file=@test.jpg" \
  -F "prompt=请描述这张图片"
```

---

## 七、总结

### ✅ 可以直接使用的功能
1. 核心图片上传和处理接口完整
2. 跨域配置已启用
3. 错误处理完善
4. 通义千问 AI 集成正常

### ⚠️ 必须解决的问题
1. **API Key 安全** - 必须通过环境变量注入
2. **生产配置** - 需要创建 `application-prod.yml`
3. **日志管理** - 需要配置日志文件输出
4. **防火墙** - 需要开放 8080 端口

### 📋 建议添加的功能
1. Docker 支持
2. 接口文档（Swagger）
3. 单元测试
4. API 访问认证
5. 请求限流

### 🎯 结论
**当前项目已经可以部署使用**，但需要：
- 创建生产环境配置文件
- 通过环境变量设置 API Key
- 配置服务器防火墙
- 建议添加 HTTPS 和反向代理

完成以上配置后，App 端即可正常调用 `/api/image/upload` 接口。