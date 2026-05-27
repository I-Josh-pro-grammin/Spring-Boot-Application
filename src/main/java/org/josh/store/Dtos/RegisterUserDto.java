package org.josh.store.Dtos;

import java.util.Date;

public class RegisterUserDto {
    private String password;
    private String username;
    private String email;
    private Date createdAt;

    public RegisterUserDto() {};

    public RegisterUserDto(String password, String username, String email, Date createdAt) {
        this.password = password;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
