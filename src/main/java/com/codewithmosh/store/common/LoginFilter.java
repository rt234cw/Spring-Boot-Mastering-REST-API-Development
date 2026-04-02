package com.codewithmosh.store.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;



@Component
public class LoginFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("request: " + request.getRequestURI());
        //filterChain前面的程式碼會先執行
        // 接著filterChain這行會把請求傳給下一個filter，最後傳給Controller
        // Controller處理完後，才會繼續執行後面的程式碼
        // 所以印出來的順序會是 filter->controller->filter
        filterChain.doFilter(request, response);
        System.out.println("response = " + response.getStatus() + ", filterChain = " + filterChain);
    }
}
