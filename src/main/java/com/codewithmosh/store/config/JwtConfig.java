package com.codewithmosh.store.config;


import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;


/**
 * JWT 核心配置類別
 * Configuration:
 *   告訴 Spring 這是一個「配置類別」。Spring 啟動時會掃描這個類別，
 *   並將其註冊為一個 Bean 放在 IoC 容器中，讓其他類別（如 JwtService）可以注入使用。
 * <p>
 * ConfigurationProperties(prefix = "spring.jwt"):
 *   將設定檔（application.yml/properties）中以 "spring.jwt" 為開頭的屬性，
 *   自動綁定到這個類別的成員變數上。
 *   例如：yml 中的 spring.jwt.secret-key 會自動設定給本類別的 secretKey 變數。
 * <p>
 * Data (來自 Lombok):
 *   自動生成 Getter, Setter, toString, equals, hashCode 等方法。
 *   在 ConfigurationProperties 中，Getter/Setter 是「必須」的，
 *   因為 Spring 需要透過 Setter 把設定檔的值注入進來。
 */
@Configuration
@ConfigurationProperties(prefix = "spring.jwt")
@Data
public class JwtConfig {
    // 將 Getter 設為 AccessLevel.NONE，Lombok 就不會產生這個 getter
    @Getter(AccessLevel.NONE)
    private String secret;
    private int accessTokenExpiration;
    private int refreshTokenExpiration;

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
