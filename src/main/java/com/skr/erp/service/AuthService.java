package com.skr.erp.service;

import com.skr.erp.dto.request.LoginRequest;
import com.skr.erp.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}