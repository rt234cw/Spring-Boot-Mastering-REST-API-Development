package com.codewithmosh.store.users;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


@AllArgsConstructor
@Service
public class UserService  {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    public Iterable<UserDto> getAllUsers(String sortBy) {
        if (!Set.of("name", "email").contains(sortBy)) {
            sortBy = "name";
        }
        return userRepository.findAll(Sort.by(sortBy)).stream().map(userMapper::toDto).toList();
    }

    public UserDto getUser(Long id) {

        var user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return userMapper.toDto(user);

    }

    public UserDto registerUser(RegisterUserRequest registerUserRequest) {

        if (userRepository.existsByEmail(registerUserRequest.getEmail())) {
            throw new DuplicateUserException();
        }
        var user = userMapper.toEntity(registerUserRequest);
        var hashedPassword = passwordEncoder.encode(registerUserRequest.getPassword());
        user.setPassword(hashedPassword);
        user.setRole(Role.USER);
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public UserDto updateUser(Long id, UpdateUserRequest request) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) {throw  new UserNotFoundException();}
        userMapper.update(request, user);
        userRepository.save(user);

        return userMapper.toDto(user);
    }

    public void deleteUser(Long id) {

        var user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        userRepository.delete(user);
    }

    public void changePassword( ChangePasswordRequest  request,Long id) {
        var user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);

        if (!user.getPassword().equals(request.getOldPassword())) {
            throw  new AccessDeniedException("Password does not match");
        }

        user.setPassword(request.getNewPassword());
        userRepository.save(user);

    }
}
