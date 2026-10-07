package humanit.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import humanit.auth.dto.LoginRequest;
import humanit.auth.dto.LoginResponse;
import humanit.auth.dto.RegisterRequest;
import humanit.auth.dto.RegisterResponse;
import humanit.auth.jwt.JwtTokenService;
import humanit.auth.user.AppUser;
import humanit.auth.user.AppUserRepository;
import humanit.auth.user.UserRole;
import humanit.error.ApplicationException;
import humanit.error.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authManager;
    private final JwtTokenService jwtTokenService;

    private final AppUserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authManager,
            JwtTokenService jwtTokenService,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApplicationException(
                    ErrorCode.USER_EMAIL_EXISTS,
                    "A user with this email already exists.");
        }

        AppUser user = new AppUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                UserRole.USER);
        AppUser savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name());
    }

    public LoginResponse login(LoginRequest request) {
        try {
            var authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()));

            return jwtTokenService.createAccessToken(authentication);
        } catch (AuthenticationException e) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
        }
    }

}
