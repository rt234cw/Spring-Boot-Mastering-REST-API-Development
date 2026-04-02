package com.codewithmosh.store.auth;


import com.codewithmosh.store.users.UserDto;
import com.codewithmosh.store.users.UserMapper;
import com.codewithmosh.store.users.UserRepository;
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
    private final AuthService authService;


    @PostMapping("/login")
    public JwtResponse login(
          @Valid @RequestBody LoginRequest loginRequest,
          HttpServletResponse response
    ) {

        var loginResponse = authService.login(loginRequest);

        var refreshToken = loginResponse.getRefreshToken().toString();

        // refresh token回傳給user的方式跟access token不一樣
        // refresh token要存在Http-only的地方
        // 建立cookie 物件
        var cookie = new Cookie("refreshToken", refreshToken);
        // 設定cookie 各項屬性
        cookie.setHttpOnly(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(jwtConfig.getRefreshTokenExpiration());
        //限定只能透過HTTPS傳遞
        cookie.setSecure(true);

        // 回傳response時，同時夾帶cookie
        response.addCookie(cookie);
        return new JwtResponse(loginResponse.getAccessToken().toString());
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @CookieValue(value = "refreshToken") String refreshToken

    ){
        var accessToken = authService.refreshToken(refreshToken);
        return  ResponseEntity.ok(new JwtResponse(accessToken.toString()));


    }


    @GetMapping("/me")
    public ResponseEntity<UserDto> me(){
        var user = authService.getCurrentUser();
       if(user==null) {
           return ResponseEntity.notFound().build();
       }

       var userDto = userMapper.toDto(user);
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
