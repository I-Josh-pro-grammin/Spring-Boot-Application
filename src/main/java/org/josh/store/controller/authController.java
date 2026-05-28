package org.josh.store.controller;

import org.josh.store.Dtos.LoginAndRegisterResponseDto;
import org.josh.store.Dtos.LoginUserDto;
import org.josh.store.Dtos.RegisterUserDto;
import org.josh.store.Dtos.UserDto;
import org.josh.store.config.JwtService;
import org.josh.store.model.User;
import org.josh.store.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
public class authController {
    @Autowired
    UserService userService;
    JwtService jwtService;

    @PostMapping("register")
    public ResponseEntity register(@RequestBody RegisterUserDto user) {
        String token = jwtService.generateToken(user.getEmail());

        return new ResponseEntity(new LoginAndRegisterResponseDto(token), HttpStatusCode.valueOf(201));
    }

    @PostMapping("login")
    public ResponseEntity login(@RequestBody LoginUserDto user) {
        String token = jwtService.generateToken(user.getEmail());

        return ResponseEntity.ok(new LoginAndRegisterResponseDto(token));
    }
}
