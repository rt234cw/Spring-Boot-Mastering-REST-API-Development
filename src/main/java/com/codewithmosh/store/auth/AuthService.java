package com.codewithmosh.store.auth;

import com.codewithmosh.store.users.User;
import com.codewithmosh.store.users.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    public User getCurrentUser() {
       var authentication =  SecurityContextHolder.getContext().getAuthentication();
       // Principal（當事人）就是代表目前正在操作系統的那個人，要去讀取他的相關身分資訊
       // 返回的物件類型，就是UsernamePasswordAuthenticationToken第一個參數所放入的類型
       var userId = (Long) authentication.getPrincipal();
       return userRepository.findById(userId).orElse(null);


   }

   public LoginResponse login(LoginRequest loginRequest) {
       authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
               loginRequest.getEmail(), loginRequest.getPassword()
       ));
       var user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow();
       var accessToken = jwtService.generateAccessToken(user);
       var refreshToken = jwtService.generateRefreshToken(user);
       return new LoginResponse(accessToken, refreshToken);
   }

   public Jwt refreshToken(String refreshToken) {
       var jwt = jwtService.parseToken(refreshToken);
       if (jwt == null || jwt.isExpired() ) {
           throw new BadCredentialsException("Invalid refresh token");
       }

       var userId = jwt.getUserId();
       var user = userRepository.findById(userId).orElseThrow();
       return jwtService.generateAccessToken(user);
   }
}
