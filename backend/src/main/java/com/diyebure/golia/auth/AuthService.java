package com.diyebure.golia.auth;

import com.diyebure.golia.auth.dto.AuthDtos.AuthResponse;
import com.diyebure.golia.auth.dto.AuthDtos.LoginRequest;
import com.diyebure.golia.auth.dto.AuthDtos.RegisterRequest;
import com.diyebure.golia.auth.dto.AuthDtos.UserDto;
import com.diyebure.golia.common.ApiException;
import com.diyebure.golia.security.JwtService;
import com.diyebure.golia.user.UserEntity;
import com.diyebure.golia.user.UserMapper;
import com.diyebure.golia.user.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Logica de autenticacion: registro, login y refresco de tokens.
 *
 * <p>La contrasena se guarda hasheada con BCrypt (nunca en claro). Los tokens
 * se emiten con {@link JwtService}. Estas operaciones son la razon por la que
 * los datos se comparten entre dispositivos: la cuenta vive en MySQL, no en el
 * telefono, asi que el mismo usuario puede entrar desde cualquier dispositivo.</p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email)) {
            throw ApiException.conflict("Ya existe una cuenta con ese correo.");
        }
        if (request.username != null && !request.username.isBlank()
                && userRepository.existsByUsername(request.username)) {
            throw ApiException.conflict("Ese nombre de usuario ya esta en uso.");
        }

        UserEntity user = new UserEntity(
                UUID.randomUUID().toString(),
                null,
                normalize(request.username),
                request.email,
                passwordEncoder.encode(request.password),
                request.country,
                null,
                0, 0, 0,
                System.currentTimeMillis());

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // La app envia username O email en el campo "email".
        UserEntity user = userRepository
                .findByUsernameOrEmail(request.email, request.email)
                .orElseThrow(() -> ApiException.unauthorized("Credenciales invalidas."));

        if (!passwordEncoder.matches(request.password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Credenciales invalidas.");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw ApiException.unauthorized("Token de refresco invalido.");
            }
            String userId = jwtService.extractUserId(refreshToken);
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> ApiException.unauthorized("Sesion expirada. Inicia sesion de nuevo."));
            return buildAuthResponse(user);
        } catch (JwtException e) {
            throw ApiException.unauthorized("Sesion expirada. Inicia sesion de nuevo.");
        }
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String access = jwtService.generateAccessToken(user.getId());
        String refresh = jwtService.generateRefreshToken(user.getId());
        UserDto dto = UserMapper.toDto(user);
        return new AuthResponse(access, refresh, dto, jwtService.getAccessExpirationSeconds());
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
