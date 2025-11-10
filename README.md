# FlowService

FlowService 是一个基于 Spring Boot 的图片处理服务，集成了阿里通义千问模型，用于接收图片、调用AI模型分析图片内容，并返回处理后的结果。

## 功能特性

- 📷 **图片接收**: 支持多种格式的图片上传（JPG、PNG、GIF等）
- 🤖 **AI分析**: 集成阿里通义千问视觉模型，智能分析图片内容
- 📝 **文本处理**: 对AI返回的结果进行清理、摘要生成等处理
- 🔄 **RESTful API**: 提供标准的REST接口供Flow App调用
- 🛡️ **异常处理**: 完善的异常处理和错误响应机制
- 🌐 **跨域支持**: 支持跨域请求，便于前端集成

## 技术栈

- **框架**: Spring Boot 3.1.5
- **语言**: Java 17
- **HTTP客户端**: Spring WebFlux
- **JSON处理**: Jackson
- **构建工具**: Maven
- **AI模型**: 阿里通义千问-VL

## 快速开始

### 1. 环境要求

- Java 17 或更高版本
- Maven 3.6 或更高版本
- 阿里云通义千问API密钥

### 2. 配置API密钥

在运行前，需要设置阿里通义千问的API密钥：

```bash
export QWEN_API_KEY="your-actual-api-key"
```

或者在 `application.yml` 中修改：

```yaml
qwen:
  api:
    key: your-actual-api-key
```

### 3. 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

## API 接口

### 1. 图片上传接口

**POST** `/api/image/upload`

使用 multipart/form-data 上传图片：

```bash
curl -X POST \\
  http://localhost:8080/api/image/upload \\
  -F "file=@/path/to/image.jpg" \\
  -F "prompt=请描述这张图片"
```

### 2. Base64图片处理接口

**POST** `/api/image/process`

```bash
curl -X POST \\
  http://localhost:8080/api/image/process \\
  -H "Content-Type: application/json" \\
  -d '{
    "prompt": "请描述这张图片",
    "imageBase64": "base64-encoded-image-data"
  }'
```

### 3. 健康检查

**GET** `/api/status/health`

```bash
curl http://localhost:8080/api/status/health
```

### 4. 服务信息

**GET** `/api/status/info`

```bash
curl http://localhost:8080/api/status/info
```

## 响应格式

所有API都返回统一的响应格式：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "taskId": "uuid",
    "originalPrompt": "用户输入的提示词",
    "processedText": "处理后的文本",
    "summary": "内容摘要",
    "metadata": {
      "fileName": "image.jpg",
      "mimeType": "image/jpeg",
      "fileSize": 1234567,
      "model": "qwen-vl-plus",
      "tokensUsed": 150,
      "processingTimeMs": 2500
    },
    "processedAt": "2024-01-01T12:00:00"
  }
}
```

## 配置说明

主要配置项在 `src/main/resources/application.yml`：

```yaml
# 服务端口
server:
  port: 8080

# 文件上传限制
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# 通义千问配置
qwen:
  api:
    url: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
    key: ${QWEN_API_KEY:your-api-key-here}
    model: qwen-turbo
```

## 错误码说明

- `200`: 成功
- `400`: 请求参数错误（如文件为空、格式不支持等）
- `500`: 服务内部错误（如API调用失败等）

## 开发和部署

### 开发环境

```bash
# 启动开发模式
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产部署

```bash
# 打包
mvn clean package

# 运行jar包
java -jar target/flowservice-1.0.0.jar
```

### Docker部署

项目支持Docker部署，可以创建Dockerfile：

```dockerfile
FROM openjdk:17-jre-slim
COPY target/flowservice-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 注意事项

1. **API密钥安全**: 请妥善保管阿里云通义千问的API密钥，不要提交到代码仓库
2. **文件大小限制**: 默认支持最大10MB的图片上传
3. **网络连接**: 确保服务器能够访问阿里云通义千问API
4. **性能考虑**: AI模型调用可能需要几秒时间，建议设置合理的超时时间

## 许可证

MIT License