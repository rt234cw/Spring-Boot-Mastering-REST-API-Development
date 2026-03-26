package com.codewithmosh.store.services;

import com.codewithmosh.store.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;


@AllArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
      var user =   userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));

      //這裡的User是Package裡面的User，繼承了UserDetails，不是我們自己寫在App裡面的User entity
      return new User(
              user.getEmail(),
              user.getPassword(),
              Collections.emptyList()
      );

    }
}
