package com.diyebure.golia.data.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;

import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.remote.api.AuthApiService;
import com.diyebure.golia.data.remote.dto.AuthDto.AuthResponse;
import com.diyebure.golia.data.remote.dto.AuthDto.UserDto;
import com.diyebure.golia.domain.model.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Unit tests for AuthRepositoryImpl.
 * Tests the authentication data layer logic.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthRepositoryImplTest {

    @Mock
    private Context mockContext;

    @Mock
    private AuthApiService mockAuthApiService;

    @Mock
    private PreferencesManager mockPreferencesManager;

    @Mock
    private Call<AuthResponse> mockAuthCall;

    @Mock
    private Call<UserDto> mockUserCall;

    private AuthRepositoryImpl authRepository;

    @Before
    public void setUp() {
        // Create test instance
        authRepository = new AuthRepositoryImpl(
                mockContext,
                mockAuthApiService,
                mockPreferencesManager
        );
    }

    @Test
    public void testLogin_Success() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        UserDto userDto = new UserDto(
                "user123",
                "testuser",
                email,
                "ES",
                "https://example.com/avatar.jpg",
                100,
                50,
                25,
                "2024-01-01T00:00:00Z"
        );
        
        AuthResponse authResponse = new AuthResponse(
                "access_token",
                "refresh_token",
                userDto,
                3600
        );
        
        Response<AuthResponse> response = Response.success(authResponse);
        when(mockAuthApiService.login(any())).thenReturn(mockAuthCall);
        when(mockAuthCall.execute()).thenReturn(response);

        // Act
        Result<User> result = authRepository.login(email, password);

        // Assert
        assertTrue(result instanceof Result.Success);
        User user = ((Result.Success<User>) result).getData();
        assertEquals("user123", user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals("ES", user.getCountry());
        
        // Verify token storage was called
        verify(mockPreferencesManager).saveToken("access_token");
        verify(mockPreferencesManager).saveRefreshToken("refresh_token");
        verify(mockPreferencesManager).saveTokenExpiry(anyLong());
        verify(mockPreferencesManager).setLoggedIn(true);
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        String email = "wrong@example.com";
        String password = "wrongpass";
        
        Response<AuthResponse> response = Response.error(401, okhttp3.ResponseBody.create(
                "application/json",
                "{\"error\":\"invalid_credentials\",\"message\":\"Invalid email or password\",\"status_code\":401}"
        ));
        
        when(mockAuthApiService.login(any())).thenReturn(mockAuthCall);
        when(mockAuthCall.execute()).thenReturn(response);

        // Act
        Result<User> result = authRepository.login(email, password);

        // Assert
        assertTrue(result instanceof Result.Error);
        assertEquals("Authentication failed. Please check your credentials.", 
                ((Result.Error) result).getError().getMessage());
    }

    @Test
    public void testIsLoggedIn_WhenTokenValid() {
        // Arrange
        when(mockPreferencesManager.isLoggedIn()).thenReturn(true);
        when(mockPreferencesManager.isTokenExpired()).thenReturn(false);

        // Act
        boolean isLoggedIn = authRepository.isLoggedIn();

        // Assert
        assertTrue(isLoggedIn);
    }

    @Test
    public void testIsLoggedIn_WhenTokenExpired() {
        // Arrange
        when(mockPreferencesManager.isLoggedIn()).thenReturn(true);
        when(mockPreferencesManager.isTokenExpired()).thenReturn(true);

        // Act
        boolean isLoggedIn = authRepository.isLoggedIn();

        // Assert
        assertFalse(isLoggedIn);
    }

    @Test
    public void testClearSession() {
        // Act
        authRepository.clearSession();

        // Assert
        verify(mockPreferencesManager).clearTokens();
    }
}