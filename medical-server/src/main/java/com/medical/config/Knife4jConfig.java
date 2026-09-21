package com.medical.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧医院预约挂号与电子病历管理系统")
                        .description("医疗后端 API 接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Medical Team")
                                .email("medical@example.com")));
    }
}
