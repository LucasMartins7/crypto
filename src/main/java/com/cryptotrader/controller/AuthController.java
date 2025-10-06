package com.cryptotrader.controller;

import com.cryptotrader.dto.ApiResponse;
import com.cryptotrader.dto.JwtResponse;
import com.cryptotrader.dto.LoginRequest;
import com.cryptotrader.dto.RegisterRequest;
import com.cryptotrader.entity.User;
import com.cryptotrader.repository.UserRepository;
import com.cryptotrader.security.JwtUtils;
import com.cryptotrader.service.RateLimitingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder encoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                            HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        
        // Rate limiting for login attempts
        if (!rateLimitingService.tryConsumeLoginAttempt(clientIp)) {
            logger.warn("Login rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Too many login attempts. Please try again later."));
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            User user = (User) authentication.getPrincipal();
            
            // Reset failed login attempts on successful login
            user.resetFailedLoginAttempts();
            userRepository.save(user);
            
            JwtResponse jwtResponse = new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail());
            
            logger.info("User {} logged in successfully from IP: {}", user.getUsername(), clientIp);
            
            return ResponseEntity.ok(ApiResponse.success("Login successful", jwtResponse));
            
        } catch (Exception e) {
            logger.warn("Failed login attempt for {} from IP: {}", loginRequest.getUsernameOrEmail(), clientIp);
            
            // Increment failed login attempts
            userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail())
                    .ifPresent(user -> {
                        user.incrementFailedLoginAttempts();
                        userRepository.save(user);
                    });
            
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid username/email or password"));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                                        HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        
        // Rate limiting for registration
        if (!rateLimitingService.tryConsumeApiRequest(clientIp)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Too many registration attempts. Please try again later."));
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username is already taken!"));
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email is already in use!"));
        }
        
        try {
            // Create new user
            User user = new User(registerRequest.getUsername(),
                               registerRequest.getEmail(),
                               encoder.encode(registerRequest.getPassword()));
            
            userRepository.save(user);
            
            logger.info("New user registered: {} from IP: {}", user.getUsername(), clientIp);
            
            return ResponseEntity.ok(ApiResponse.success("User registered successfully!"));
            
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration failed. Please try again."));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String jwt = parseJwt(request);
        
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            return ResponseEntity.ok(ApiResponse.success("Token is valid", username));
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid or expired token"));
    }
    
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        
        return null;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0];
        }
    }
}
