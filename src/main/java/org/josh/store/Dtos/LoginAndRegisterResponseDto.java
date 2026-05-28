package org.josh.store.Dtos;

public class LoginAndRegisterResponseDto {
    private String token;

    public LoginAndRegisterResponseDto() {};
    public LoginAndRegisterResponseDto(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
