# FlowService 快速开始指南

## 本地开发测试

### 1. 前置要求
- Java 17+
- Maven 3.6+
- 阿里云通义千问 API Key

### 2. 配置 API Key
```bash
export DASHSCOPE_API_KEY="sk-your-api-key-here"
```

### 3. 启动服务
```bash
./start.sh
```

### 4. 测试接口
打开浏览器访问：http://localhost:8080/api/test.html

或使用 curl 测试：
```bash
curl http://localhost:8080/api/status/health
```

---

## 服务器部署（生产环境）

### 方式1：Docker 部署（推荐）

#### 1. 准备环境
```bash
# 安装 Docker 和 Docker Compose
sudo apt update
sudo apt install docker.io docker-compose
```

#### 2. 克隆代码
```bash
git clone <your-repo-url>
cd FlowService
```

#### 3. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 文件，填入真实的 API Key
nano .env
```

#### 4. 运行部署脚本
```bash
export DASHSCOPE_API_KEY="sk-your-api-key-here"
./deploy.sh
# 选择 1 (Docker 部署)
```

#### 5. 验证服务
```bash
./test-api.sh http://your-server-ip:8080
```

---

### 方式2：直接 JAR 部署

#### 1. 编译打包
```bash
mvn clean package -DskipTests
```

#### 2. 上传到服务器
```bash
scp target/flowservice-1.0.0.jar user@server:/opt/flowservice/
```

#### 3. 配置 systemd 服务
```bash
# 复制服务配置文件
sudo cp flowservice.service /etc/systemd/system/

# 编辑服务文件，填入真实的 API Key
sudo nano /etc/systemd/system/flowservice.service

# 创建用户和目录
sudo useradd -r -s /bin/false flowservice
sudo mkdir -p /opt/flowservice
sudo mkdir -p /var/log/flowservice
sudo chown -R flowservice:flowservice /opt/flowservice /var/log/flowservice

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable flowservice
sudo systemctl start flowservice

# 查看状态
sudo systemctl status flowservice
```

#### 4. 查看日志
```bash
sudo journalctl -u flowservice -f
```

---

## App 端接入

### API 基础信息
- 基础 URL: `http://your-server:8080/api`
- Content-Type: `multipart/form-data`
- 超时时间: 建议 30-60 秒

### 图片上传接口

**端点**: `POST /api/image/upload`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 图片文件（JPG/PNG/GIF/HEIC，最大10MB） |
| prompt | String | 否 | 提示词（默认："请描述这张图片"） |
| temperature | Double | 否 | 温度参数（默认：0.7） |
| maxTokens | Integer | 否 | 最大 Token 数（默认：1000） |

**响应示例**:
```json
{
  "code": 200,
  "message": "图片处理成功",
  "data": {
    "taskId": "uuid",
    "originalPrompt": "请描述这张图片",
    "processedText": "这是一张...",
    "summary": "图片显示...",
    "metadata": {
      "fileName": "image.jpg",
      "fileSize": 1234567,
      "processingTimeMs": 2500
    }
  }
}
```

### iOS 示例代码
```swift
func uploadImage(image: UIImage, prompt: String) {
    let url = URL(string: "http://your-server:8080/api/image/upload")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"

    let boundary = UUID().uuidString
    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

    var body = Data()

    // 添加图片
    if let imageData = image.jpegData(compressionQuality: 0.8) {
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n".data(using: .utf8)!)
    }

    // 添加提示词
    body.append("--\(boundary)\r\n".data(using: .utf8)!)
    body.append("Content-Disposition: form-data; name=\"prompt\"\r\n\r\n".data(using: .utf8)!)
    body.append(prompt.data(using: .utf8)!)
    body.append("\r\n".data(using: .utf8)!)

    body.append("--\(boundary)--\r\n".data(using: .utf8)!)
    request.httpBody = body

    URLSession.shared.dataTask(with: request) { data, response, error in
        // 处理响应
    }.resume()
}
```

### Android 示例代码
```kotlin
fun uploadImage(imageFile: File, prompt: String) {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            imageFile.name,
            imageFile.asRequestBody("image/*".toMediaType())
        )
        .addFormDataPart("prompt", prompt)
        .build()

    val request = Request.Builder()
        .url("http://your-server:8080/api/image/upload")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            // 处理响应
        }

        override fun onFailure(call: Call, e: IOException) {
            // 处理错误
        }
    })
}
```

---

## 常见问题

### Q1: 服务启动失败
**检查清单**:
1. Java 版本是否 >= 17
2. API Key 是否正确配置
3. 端口 8080 是否被占用
4. 查看日志：`sudo journalctl -u flowservice -f`

### Q2: 图片上传失败
**可能原因**:
1. 图片大小超过 10MB
2. 图片格式不支持
3. 网络超时（AI 处理需要时间）

### Q3: 无法访问接口
**检查清单**:
1. 防火墙是否开放 8080 端口
2. 服务是否正常运行：`./test-api.sh`
3. CORS 配置是否正确

### Q4: AI 返回错误
**可能原因**:
1. API Key 无效或过期
2. 服务器无法访问阿里云 API
3. 图片内容违规

---

## 监控和维护

### 查看服务状态
```bash
sudo systemctl status flowservice
```

### 查看日志
```bash
# systemd 日志
sudo journalctl -u flowservice -f

# 应用日志
tail -f /var/log/flowservice/application.log
```

### 重启服务
```bash
sudo systemctl restart flowservice
```

### 停止服务
```bash
sudo systemctl stop flowservice
```

---

## 更多信息

- 完整文档：[README.md](README.md)
- 部署清单：[DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)
- 更新日志：[CHANGELOG.md](CHANGELOG.md)