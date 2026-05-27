package org.josh.store.service;

import jakarta.transaction.Transactional;
import org.josh.store.Dtos.LoginUserDto;
import org.josh.store.Dtos.RegisterUserDto;
import org.josh.store.model.User;
import org.josh.store.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class UserService {
    @Autowired
    UserRepository userRepository;
    public User registerUser(RegisterUserDto userDto) {
        if(userDto.getEmail().isEmpty() || userDto.getPassword().isEmpty() || userDto.getUsername().isEmpty()) {
            System.out.println("Email or password or username is empty");
        }
        User user = new User(userDto.getEmail(), userDto.getUsername(), userDto.getPassword(), new Date());

        User newUser = userRepository.save(user);

        return newUser;
    }

    public User login(LoginUserDto user) {
        if(user.getEmail().isEmpty() || user.getPassword().isEmpty()) {
            System.out.println("Email or password or username is empty");
            return null;
        }

        User newUser = userRepository.findByEmail(user.getEmail());

        if(user.getPassword() != newUser.getPassword())  {
            System.out.println("password is incorrect");
            return null;
        }

        return newUser;
    }


}
