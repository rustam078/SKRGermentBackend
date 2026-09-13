package com.skr.erp.service;

import com.skr.erp.dto.request.LoginRequest;
import com.skr.erp.dto.response.LoginResponse;
import com.skr.erp.entity.AppUser;
import com.skr.erp.exception.InvalidCredentialsException;
import com.skr.erp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;

    @Override
    public LoginResponse login(LoginRequest request) {

        AppUser user = appUserRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid username or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("User is inactive");
        }

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}