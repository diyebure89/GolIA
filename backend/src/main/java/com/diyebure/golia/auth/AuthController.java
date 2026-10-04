package com.diyebure.golia.auth;

import com.diyebure.golia.auth.dto.AuthDtos.AuthResponse;
import com.diyebure.golia.auth.dto.AuthDtos.LoginRequest;
import com.diyebure.golia.auth.dto.AuthDtos.RegisterRequest;
import com.diyebure.golia.auth.dto.AuthDtos.TokenRefreshRequest;
import com.diyebure.golia.auth.dto.AuthDtos.UserDto;
import com.diyebure.golia.common.ApiException;
import com.diyebure.golia.user.UserEntity;
import com.diyebure.golia.user.UserMapper;
import com.diyebure.golia.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticacion que la app Android ya espera consumir a traves de
 * {@code AuthApiService}:
 * <ul>
 *   <li>POST /auth/login</li>
 *   <li>POST /auth/register</li>
 *   <li>POST /auth/refresh</li>
 *   <li>POST /auth/logout</li>
 *   <li>GET  /user/profile</li>
 * </ul>
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request.refreshToken);
    }

    /**
     * Logout. Con JWT sin estado el servidor no guarda sesion, asi que basta con
     * responder 200 y que la app borre sus tokens locales. Se acepta el header
     * de autorizacion por compatibilidad con el contrato de la app.
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    /** Perfil del usuario autenticado (resuelto desde el access token). */
    @GetMapping("/user/profile")
    public UserDto profile(Authentication authentication) {
        String userId = requireUserId(authentication);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("No autenticado."));
        return UserMapper.toDto(user);
    }

    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw ApiException.unauthorized("No autenticado.");
        }
        return authentication.getPrincipal().toString();
    }
}
