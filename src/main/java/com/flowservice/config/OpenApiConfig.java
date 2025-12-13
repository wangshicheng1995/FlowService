package com.flowservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置类
 * 配置 API 文档的基本信息
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI flowServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlowService API")
                        .description("Flow App 后端服务 API 文档\n\n" +
                                "## 功能模块\n" +
                                "- **图片上传**: 上传食物图片，AI 分析营养成分\n" +
                                "- **热量统计**: 查询用户在指定时间范围内的食物总热量\n" +
                                "- **健康评分**: 获取用户的健康压力分数\n\n" +
                                "## 测试说明\n" +
                                "1. 点击接口展开详情\n" +
                                "2. 点击 'Try it out' 按钮\n" +
                                "3. 填写参数并点击 'Execute' 执行")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Flow Team")
                                .email("support@flowapp.com"))
                        .license(new License()
                                .name("私有协议")
                                .url("https://flowapp.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("本地开发环境"),
                        new Server()
                                .url("http://139.196.221.226:8080" + contextPath)
                                .description("线上测试环境")));
    }
}
