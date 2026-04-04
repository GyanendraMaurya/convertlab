package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.security_util.CookieUtil;
import com.convertlab.convertlab_backend.service_core.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CookieUtil cookieUtil;

    @DeleteMapping()
    public ResponseEntity<ApiResponse<String>> deleteCurrentUser(@AuthenticationPrincipal String principal, HttpServletResponse response) {
        userService.deletePrincipalUser(principal, response);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
