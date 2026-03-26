package com.codewithmosh.store.config;


import com.codewithmosh.store.entities.Role;
import com.codewithmosh.store.filters.JwtAuthenticationFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();

        //DaoAuthenticationProvider裡面有兩個欄位
        // 1. passwordEncoder
        // 2. userDetailsService
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();

    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.sessionManagement(
                 //告訴伺服器我們不需要維護state，no sessions
                c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
                // Cross-site request forgery 跨站請求偽造
                // 駭客盜取你的Session Cookie
                // 因為我們使用了JWT方式，沒有使用Session，所以關閉CSRF，節少開發上的麻煩
                .csrf(AbstractHttpConfigurer::disable)

                // 設定無須授權即可瀏覽的頁面
                .authorizeHttpRequests(c->c
                        //carts跟child pages都不需要授權即可瀏覽
                        .requestMatchers("/carts/**").permitAll()

                        //只有符合特定權限身分的人才能讀取 （Role是我們自己寫的enum class）
                        .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",    // OpenAPI 3 的定義檔路徑
                                "/swagger-resources/**",
                                "/webjars/**").permitAll()
                        //這個users endpoint只允許POST method
                        .requestMatchers(HttpMethod.POST,"/users").permitAll()
                        .requestMatchers(HttpMethod.POST,"/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST,"/auth/refresh").permitAll()

                        //其他頁面都需要授權才能瀏覽
                        .anyRequest().authenticated())
                // 在檢查帳號密碼之前，先看看有沒有 JWT
                // 把jwtAuthenticationFilter置於在UsernamePasswordAuthenticationFilter之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // exceptionHandling會捕獲其他filter回傳的錯誤類型，再根據這些錯誤類型，從我們寫的程式碼挑選對應的錯誤回傳
                // 如果是沒有寫到的對應exception，就會回傳500 internal server error
                .exceptionHandling(c-> {
                    // 401 未通過驗證
                    c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

                    // 403 通過驗證，但不具此頁面權限
                    c.accessDeniedHandler((req, res, ex) -> res.setStatus(HttpStatus.FORBIDDEN.value()));


                });



        return http.build();
}

}
