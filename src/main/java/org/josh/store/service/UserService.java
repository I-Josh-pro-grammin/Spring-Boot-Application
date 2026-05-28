package org.josh.store.service;

import jakarta.transaction.Transactional;
import org.josh.store.Dtos.LoginUserDto;
import org.josh.store.Dtos.RegisterUserDto;
import org.josh.store.Dtos.UserDto;
import org.josh.store.config.SecurityConfig;
import org.josh.store.model.User;
import org.josh.store.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public User registerUser(RegisterUserDto userDto) {
        if(userDto.getEmail().isEmpty() || userDto.getPassword().isEmpty() || userDto.getUsername().isEmpty()) {
            System.out.println("Email or password or username is empty");
        }

        User foundUser = userRepository.findByEmail(userDto.getEmail());

        if(foundUser != null) {
            System.out.println("User already exists");
            return null;
        }

        String encodedPassword = passwordEncoder.encode(userDto.getPassword());

        User user = new User(userDto.getEmail(), userDto.getUsername(), encodedPassword, new Date());

        User newUser = userRepository.save(user);

        return newUser;
    }

    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getCreatedAt()
        );
    }

    public UserDto login(LoginUserDto user) {
        if(user.getEmail().isEmpty() || user.getPassword().isEmpty()) {
            System.out.println("Email or password or username is empty");
            return null;
        }

        User foundUser = userRepository.findByEmail(user.getEmail());

        if(foundUser == null) {
            System.out.println("User not found");
            return  null;
        }

        if(passwordEncoder.matches(foundUser.getPassword(), user.getPassword()))  {
            System.out.println("password is incorrect");
            throw new RuntimeException("Password is incorrect");
        }

        return new UserDto(
                foundUser.getId(),
                foundUser.getEmail(),
                foundUser.getUsername(),
                foundUser.getCreatedAt()
        );
    }


}
