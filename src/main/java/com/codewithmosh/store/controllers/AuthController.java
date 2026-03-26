package com.codewithmosh.store.controllers;


import com.codewithmosh.store.config.JwtConfig;
import com.codewithmosh.store.dtos.JwtResponse;
import com.codewithmosh.store.dtos.LoginRequest;
import com.codewithmosh.store.dtos.UserDto;
import com.codewithmosh.store.mappers.UserMapper;
import com.codewithmosh.store.repositories.UserRepository;
import com.codewithmosh.store.services.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("auth")
@AllArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtConfig jwtConfig;


    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
          @Valid @RequestBody LoginRequest loginRequest,
          HttpServletResponse response
    ) {

//        var user = userRepository.findByEmail((loginRequest.getEmail()));
//        System.out.println(user);
//        if (user.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//        var requestPassword = loginRequest.getPassword();
//        var currentPassword = user.get().getPassword();
//        var passwordMatched = passwordEncoder.matches(requestPassword, currentPassword);
//        if (passwordMatched) {
//            return ResponseEntity.noContent().build();
//        }

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(), loginRequest.getPassword()
        ));

        var user =userRepository.findByEmail(loginRequest.getEmail()).orElseThrow();
        var accessToken = jwtService.generateAccessToken(user);

        var refreshToken = jwtService.generateRefreshToken(user);


        // refresh token回傳給user的方式跟access token不一樣
        // refresh token要存在Http-only的地方

        // 建立cookie 物件
        var cookie = new Cookie("refreshToken",refreshToken.toString());

        // 設定cookie 各項屬性
        cookie.setHttpOnly(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(jwtConfig.getRefreshTokenExpiration());
        //限定只能透過HTTPS傳遞
        cookie.setSecure(true);

        // 回傳response時，同時夾帶cookie
        response.addCookie(cookie);
        return ResponseEntity.ok(new JwtResponse(accessToken.toString()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @CookieValue(value = "refreshToken") String refreshToken

    ){
        var jwt = jwtService.parseToken(refreshToken);
        if (jwt == null || jwt.isExpired() ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userId = jwt.getUserId();
        var user = userRepository.findById(userId).orElseThrow();
        var accessToken = jwtService.generateAccessToken(user);

        return  ResponseEntity.ok(new JwtResponse(accessToken.toString()));


    }


    @GetMapping("/me")
    public ResponseEntity<UserDto> me(){
       var authentication =  SecurityContextHolder.getContext().getAuthentication();
       // Principal（當事人）就是代表目前正在操作系統的那個人，要去讀取他的相關身分資訊
        // 返回的物件類型，就是UsernamePasswordAuthenticationToken第一個參數所放入的類型
       var userId = (Long) authentication.getPrincipal();
       var user = userRepository.findById(userId);
       if(user.isEmpty()){
           return ResponseEntity.notFound().build();
       }

       var userDto = userMapper.toDto(user.get());
       return ResponseEntity.ok(userDto);


    }


    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String,String>> handleValidationErrors(
            MethodArgumentNotValidException exception
    ){

        var errors = new HashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach((error) -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<Void> handleBadCredentialsException(){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
