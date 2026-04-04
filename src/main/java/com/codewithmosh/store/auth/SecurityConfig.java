package com.codewithmosh.store.auth;


import com.codewithmosh.store.common.SecurityRules;
import com.codewithmosh.store.users.Role;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;

    //程式啟動時，只有這些subclass有implement這個SecurityRules 介面，且subclass有@Component這個註記，就會自動生成
    private final List<SecurityRules> featureSecurityRules;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter, HandlerExceptionResolver handlerExceptionResolver) throws Exception {
        http.sessionManagement(
                 //告訴伺服器我們不需要維護state，no sessions
                c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
                // Cross-site request forgery 跨站請求偽造
                // 駭客盜取你的Session Cookie
                // 因為我們使用了JWT方式，沒有使用Session，所以關閉CSRF，節少開發上的麻煩
                .csrf(AbstractHttpConfigurer::disable)
                // 設定無須授權即可瀏覽的頁面
                .authorizeHttpRequests(c-> {

                    featureSecurityRules.forEach(r->r.configure(c));
                                                //其他頁面都需要授權才能瀏覽
                            c.anyRequest().authenticated();
                })
                // 在檢查帳號密碼之前，先看看有沒有 JWT
                // 把jwtAuthenticationFilter置於在UsernamePasswordAuthenticationFilter之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // exceptionHandling會捕獲其他filter回傳的錯誤類型，再根據這些錯誤類型，從我們寫的程式碼挑選對應的錯誤回傳
                // 如果是沒有寫到的對應exception，就會回傳500 internal server error
                .exceptionHandling(c-> {
                    // 401 未通過驗證
                    c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

                    // 403 通過驗證，但不具此頁面權限
                    c.accessDeniedHandler((req, res, ex) -> handlerExceptionResolver.resolveException(req,res,null,ex));


                });



        return http.build();
}

}
