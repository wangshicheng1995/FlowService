# Flow Backend - Java Spring Boot 项目开发指南

## 项目概述

Flow 后端服务采用 Java Spring Boot 框架,为移动端提供 RESTful API。核心功能包括:
- 食物图片接收与通义千问多模态 AI 识别
- 营养数据分析与健康预测算法
- 用户数据管理与健康档案存储
- 健康货币系统后端逻辑
- 社交功能数据服务

## 技术栈
- **框架**: Spring Boot 3.x
- **语言**: Java 17+
- **数据库**: MySQL 8.0 / PostgreSQL
- **AI 服务**: 阿里云通义千问 (qwen-vl-plus)
- **构建工具**: Maven
- **API 文档**: SpringDoc OpenAPI

## 代码开发规范

### 代码风格
- 遵循阿里巴巴 Java 开发规范
- 使用 Google Java Style Guide 格式化
- 类名使用大驼峰(PascalCase),方法和变量使用小驼峰(camelCase)
- 常量使用全大写下划线分隔(CONSTANT_NAME)
- 包名全小写,遵循域名反写规则
- 关键业务逻辑添加中文注释,便于团队理解

## AI 代码验收流程

### 第一步:语法与编译检查
**目标**: 确保代码可编译,无语法错误

```bash
# Maven 编译检查(不运行测试)
mvn clean compile -DskipTests

# 检查依赖冲突
mvn dependency:tree
```

**验收清单**:
- [ ] 代码编译成功,无 compilation error
- [ ] 无 Maven 依赖冲突警告
- [ ] 无未使用的 import
- [ ] 所有 `@Autowired` 注入的 Bean 已定义
- [ ] 所有 `@RequestMapping` 路径无重复
- [ ] `application.yml` 配置格式正确
- [ ] 数据库连接配置正确(密码等敏感信息使用环境变量)

**静态代码检查**:
```bash
# 使用 Checkstyle
mvn checkstyle:check

# 使用 SpotBugs 查找潜在 bug
mvn spotbugs:check

# 使用 PMD 代码检查
mvn pmd:check
```

### 第二步:上下文影响分析
**目标**: 确保修改不破坏现有功能

**检查要点**:
1. **接口变更影响**
   - 如修改 Controller 的 API 路径或参数,检查前端调用
   - 如修改 Service 方法签名,检查所有调用方
   - 如修改数据库表结构,检查所有相关 Entity 和 SQL

2. **依赖注入影响**
   - 新增 Service 是否已添加 `@Service` 注解
   - 构造器注入是否正确(推荐使用构造器注入而非字段注入)
   - Bean 循环依赖检查

3. **事务管理影响**
   - 新增数据库操作是否添加 `@Transactional`
   - 事务传播行为是否正确
   - 避免大事务和长事务

**验收操作**:
```bash
# 查找方法调用
grep -rn "methodName" src/main/java/

# 查找 Entity 使用
grep -rn "EntityName" src/

# 检查 API 端点
grep -rn "@RequestMapping\|@GetMapping\|@PostMapping" src/main/java/com/flow/controller/
```

### 第三步:单元测试验证
**目标**: 确保新代码通过单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=YourTestClass

# 运行测试并生成覆盖率报告
mvn test jacoco:report
```

**验收清单**:
- [ ] 新增 Service 方法已编写单元测试
- [ ] 测试覆盖率 > 70%(核心业务逻辑 > 90%)
- [ ] 所有已有测试仍然通过
- [ ] Mock 外部依赖(数据库、AI API)
- [ ] 测试包含正常情况和异常情况

**单元测试示例**:
```java
@SpringBootTest
class FoodRecognitionServiceTest {
    
    @MockBean
    private TongyiService tongyiService;
    
    @Autowired
    private FoodRecognitionService foodRecognitionService;
    
    @Test
    void testRecognizeFood_success() {
        // Given
        MultipartFile mockFile = createMockImage();
        when(tongyiService.analyzeImage(any()))
            .thenReturn(new FoodInfo("苹果", 52));
        
        // When
        FoodInfo result = foodRecognitionService.recognizeFood(mockFile);
        
        // Then
        assertNotNull(result);
        assertEquals("苹果", result.getName());
        assertEquals(52, result.getCalories());
    }
    
    @Test
    void testRecognizeFood_aiServiceFailure() {
        // Given
        when(tongyiService.analyzeImage(any()))
            .thenThrow(new AiServiceException("API 调用失败"));
        
        // When & Then
        assertThrows(BusinessException.class, 
            () -> foodRecognitionService.recognizeFood(mockFile));
    }
}
```

### 第四步:集成测试(可选)
**目标**: 测试完整业务流程

```bash
# 启动测试数据库(如 H2 或 Testcontainers)
# 运行集成测试
mvn verify
```

**验收清单**:
- [ ] 数据库操作正确(CRUD)
- [ ] 事务回滚正常
- [ ] API 端到端流程正确

### 第五步:应用启动验证
**目标**: 确保应用可正常启动和运行

```bash
# 本地启动应用
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/flow-backend-0.0.1-SNAPSHOT.jar
```

**验收清单**:
- [ ] 应用启动无 ERROR 日志
- [ ] 数据库连接成功
- [ ] 所有 Bean 正确加载
- [ ] Tomcat 正常监听端口(默认 8080)
- [ ] 健康检查接口可访问 `http://localhost:8080/actuator/health`

**如果无法完整启动,使用降级测试**:
1. **测试 Configuration**
   ```java
   @SpringBootTest
   class ConfigurationTest {
       @Autowired
       private ApplicationContext context;
       
       @Test
       void contextLoads() {
           assertNotNull(context);
       }
   }
   ```

2. **测试单个 Controller**
   ```bash
   # 使用 MockMvc 测试
   mvn test -Dtest=FoodControllerTest
   ```

3. **测试数据库连接**
   ```java
   @DataJpaTest
   class RepositoryTest {
       @Autowired
       private FoodRepository foodRepository;
       
       @Test
       void testConnection() {
           assertNotNull(foodRepository.findAll());
       }
   }
   ```

### 第六步:API 功能验证
**目标**: 确保 API 正确响应

**使用 Postman/Curl 测试**:
```bash
# 测试食物识别 API
curl -X POST http://localhost:8080/api/food/recognize \
  -H "Content-Type: multipart/form-data" \
  -F "image=@test.jpg"

# 测试健康分析 API
curl -X POST http://localhost:8080/api/health/analyze \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "foodId": 100, "amount": 200}'
```

**验收清单**:
- [ ] API 返回正确的 HTTP 状态码(200/201/400/500)
- [ ] 响应 JSON 格式正确
- [ ] 必填参数校验生效(@Valid)
- [ ] 错误响应包含清晰的错误信息
- [ ] 响应时间可接受(< 3 秒)

**使用 Swagger 文档测试**:
- 访问 `http://localhost:8080/swagger-ui.html`
- 验证所有 API 文档自动生成
- 直接在 Swagger UI 中测试接口

### 第七步:性能与安全检查
**目标**: 确保代码性能和安全性

**性能检查**:
- [ ] 数据库查询已优化(避免 N+1 查询)
- [ ] 使用分页查询大数据集
- [ ] 缓存策略合理(如 Redis)
- [ ] 异步处理耗时操作(如 AI 调用)
- [ ] 连接池配置合理

**安全检查**:
- [ ] 敏感信息不打印到日志(密码、token)
- [ ] SQL 使用参数化查询(防止注入)
- [ ] 文件上传限制大小和类型
- [ ] API 有权限控制(如 Spring Security)
- [ ] CORS 配置正确

**性能测试**:
```bash
# 使用 JMeter 或 ab 工具
ab -n 1000 -c 10 http://localhost:8080/api/food/list
```

### 第八步:代码质量检查
**目标**: 保持代码可维护性

**人工审查清单**:
- [ ] 业务逻辑清晰,有中文注释
- [ ] 魔法数字提取为常量
- [ ] 重复代码提取为公共方法
- [ ] 异常处理完善(不吞异常)
- [ ] 日志记录合理(INFO/WARN/ERROR 级别正确)
- [ ] 代码符合单一职责原则
- [ ] 方法长度合理(< 50 行)

**代码质量工具**:
```bash
# SonarQube 代码扫描
mvn sonar:sonar

# 查看圈复杂度
mvn pmd:cpd
```

## 常用命令

### 构建与运行
```bash
# 清理并编译
mvn clean compile

# 打包(跳过测试)
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run

# 指定配置文件运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 测试相关
```bash
# 运行所有测试
mvn test

# 运行并生成覆盖率报告
mvn clean test jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

### 代码检查
```bash
# 代码风格检查
mvn checkstyle:check

# 查找潜在 bug
mvn spotbugs:check

# 依赖安全漏洞检查
mvn dependency-check:check
```

### 数据库相关
```bash
# Flyway 数据库迁移
mvn flyway:migrate

# 查看迁移历史
mvn flyway:info

# 生成数据库文档
mvn jooq-codegen:generate
```

## 特定场景验收

### 数据库操作代码
- [ ] Entity 类正确映射表结构(@Entity, @Table)
- [ ] 主键策略正确(@Id, @GeneratedValue)
- [ ] 关联关系正确(@OneToMany, @ManyToOne)
- [ ] 索引已添加(@Index)
- [ ] 字段约束正确(nullable, length, unique)
- [ ] 使用 JPA 方法命名规范或 @Query
- [ ] 批量操作使用 batch 模式

### AI 接口集成代码
- [ ] API Key 从配置读取,不硬编码
- [ ] 请求超时时间设置(建议 30 秒)
- [ ] 重试机制(如 3 次重试)
- [ ] 限流控制(避免超出配额)
- [ ] 错误码映射为业务异常
- [ ] 降级策略(AI 失败时使用备用方案)

### 文件上传代码
- [ ] 文件大小限制(application.yml 配置)
- [ ] 文件类型白名单校验
- [ ] 文件名防止路径穿越攻击
- [ ] 上传文件存储路径配置化
- [ ] 返回文件访问 URL
- [ ] 临时文件及时清理

### 定时任务代码
- [ ] 使用 @Scheduled 正确配置
- [ ] Cron 表达式正确
- [ ] 任务执行时间合理(避免高峰期)
- [ ] 任务幂等性(重复执行不出错)
- [ ] 任务执行日志记录

## 开发建议

### 优先级原则
1. **功能正确性** > 代码优雅
2. **系统稳定性** > 功能丰富度
3. **核心链路性能** > 辅助功能性能

### 渐进式开发
- 先实现 Happy Path(正常流程)
- 再处理 Exception Path(异常流程)
- 最后优化性能和体验

### API 设计原则
- RESTful 风格,资源名词复数
- 使用标准 HTTP 方法(GET/POST/PUT/DELETE)
- 统一返回格式 `Result<T>`
- 版本控制 `/api/v1/`
- 文档自动生成(Swagger)

### 配置管理策略
- 使用 `application-{profile}.yml` 多环境配置
- 敏感信息使用环境变量或配置中心
- 配置项添加注释说明
- 提供配置默认值

## 常见问题排查

### 编译失败
1. 检查 Maven 依赖版本冲突: `mvn dependency:tree`
2. 清理 Maven 缓存: `rm -rf ~/.m2/repository`
3. 重新导入项目(IDEA: Maven > Reload)

### 启动失败
1. 检查端口占用: `lsof -i :8080`
2. 查看完整启动日志
3. 检查数据库连接配置
4. 验证 Bean 循环依赖

### 性能问题
1. 开启 SQL 日志查看慢查询
2. 使用 Arthas 诊断 JVM
3. 检查数据库索引
4. 分析 GC 日志

### 通义千问 API 调用失败
1. 验证 API Key 有效性
2. 检查网络连通性(curl 测试)
3. 确认请求格式符合文档
4. 查看阿里云配额是否用尽

## 提交前最终检查

- [ ] 代码已通过所有验收步骤
- [ ] 单元测试覆盖率达标
- [ ] 应用可正常启动
- [ ] API 已通过 Postman 测试
- [ ] 代码已格式化(IDEA: Ctrl+Alt+L)
- [ ] 无 TODO 或 FIXME 遗留
- [ ] 敏感信息已移除(密码、token)
- [ ] 提交信息清晰描述改动

### 免费额度验证
- [ ] 确认已开通阿里云百炼服务
- [ ] 验证 API Key 可用
- [ ] 测试请求计入配额
- [ ] 监控配额使用情况

---

**记住**: 自动化验收是为了提高代码质量和开发效率,但不能替代人工审查。对关键业务逻辑,仍需仔细人工复核。
