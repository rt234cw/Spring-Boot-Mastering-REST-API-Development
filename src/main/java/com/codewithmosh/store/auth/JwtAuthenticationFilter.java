package com.codewithmosh.store.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;



    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;

        }

        var token = authHeader.replace("Bearer ", "");
        var jwt = jwtService.parseToken(token);

        if (jwt == null || jwt.isExpired()) {
            filterChain.doFilter(request, response);
            return;
        }



        /// 這行是在建立一個 Authentication 物件。在 Spring Security 中，這就是使用者的「數位身分證」。
        /// UsernamePasswordAuthenticationToken: 這是身分證的「格式」。雖然我們是用 JWT 登入，但我們借用這個標準格式來儲存使用者資訊。
        /// 第一個參數 jwtService.getUsernameFromToken(token) (Principal):
        /// 關鍵詞：Principal (當事人)。
        /// 這代表「你是誰」。通常放用戶名或 User ID。
        /// 第二個參數 null (Credentials):
        /// 關鍵詞：Credentials (憑據)。
        /// 原本是用來放密碼的。但因為 JWT 已經驗證過了，我們不再需要密碼，所以傳 null。
        /// 第三個參數 null (Authorities):
        /// 關鍵詞：Authorities (權限/角色)。
        /// 這代表「你能做什麼」（例如 ROLE_USER 或 ROLE_ADMIN）。你現在傳 null 代表這個人沒有任何權限，通常實務上會從資料庫查出他的角色並傳入。

        // UsernamePasswordAuthenticationToken，有兩個constructor
        // 第一個constructor有principal, credentials兩個參數，用於「登入」
        // 第二個constructor多了Authorities，用於已通過認證的用戶，並定義其「權限等級」
        var authentication = new UsernamePasswordAuthenticationToken(
                jwt.getUserId(),
                null,
                //role前面要Prefix "ROLE_"，這是規定格式
                List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getRole())));


        /// 這行是在身分證的背面寫上一些「額外資訊」。
        /// WebAuthenticationDetailsSource: 這是一個工具類，專門用來從 HTTP 請求（HttpServletRequest）中提取資訊。
        /// buildDetails(request): 它會自動抓取發出請求的人的 IP 地址和** Session ID**。
        /// 用途：萬一以後發生安全問題，你可以從這個身分證物件裡查出這筆請求是從哪個 IP 來的。
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));




        /// 第三行：把身分證存進錢包 (SecurityContext)
        /// 這是最關鍵的一步：正式認可你的身分。
        /// SecurityContextHolder: 想像成一個巨大的置物櫃。它是全域的，在同一個請求的任何地方都能存取。
        /// getContext(): 這是你在這個置物櫃裡專屬的抽屜（Context）。
        /// setAuthentication(authentication): 把剛剛做好的身分證放進抽屜。
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);

    }
}
