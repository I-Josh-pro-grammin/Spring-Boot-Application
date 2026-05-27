package org.josh.store.controller;

import org.josh.store.model.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
public class authController {
    @PostMapping("register")
    public User register(@RequestBody User user) {
        return register(user);
    }

    @PostMapping("login")
    public User login(@RequestBody User user) {
        return login(user);
    }
}
