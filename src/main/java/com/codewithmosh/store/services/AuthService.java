package com.codewithmosh.store.services;

import com.codewithmosh.store.entities.User;
import com.codewithmosh.store.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;



    public User getCurrentUser() {
       var authentication =  SecurityContextHolder.getContext().getAuthentication();
       // Principal（當事人）就是代表目前正在操作系統的那個人，要去讀取他的相關身分資訊
       // 返回的物件類型，就是UsernamePasswordAuthenticationToken第一個參數所放入的類型

        System.out.println("DEBUG - Principal Class: " + authentication.getPrincipal().getClass().getName());
        System.out.println("DEBUG - Principal Value: " + authentication.getPrincipal());
       var userId = (Long) authentication.getPrincipal();
       return userRepository.findById(userId).orElse(null);


   }
}
