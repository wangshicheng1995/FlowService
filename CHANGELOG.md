# 更新日志

## 2025-11-10 - v1.0.1

### 修复
- 修复通义千问API调用问题，从HTTP请求方式改为使用官方SDK
- 移除 `InputRequiredException` 的不必要捕获
- 清理未使用的导入和模型类（QwenRequest, QwenResponse）

### 变更
- 添加阿里云DashScope SDK依赖（版本 2.16.7）
- 使用 `MultiModalConversation` 调用通义千问VL模型
- 更新配置项，移除不需要的 `url` 配置
- 环境变量改为 `DASHSCOPE_API_KEY`

### 技术细节
- QwenApiService 现在使用官方SDK的 `MultiModalConversation.call()` 方法
- 支持图片Base64编码 + 文本提示词的多模态输入
- 改进异常处理，区分 NoApiKeyException 和 ApiException

## 2025-11-10 - v1.0.0

### 新增
- 初始版本发布
- 实现图片上传和处理功能
- 集成阿里通义千问VL模型
- 提供RESTful API接口
- 创建测试HTML页面