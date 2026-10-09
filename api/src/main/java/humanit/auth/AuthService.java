package humanit.auth;

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
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
    String email = normalizeEmail(request.email());

    if (userRepository.existsByEmail(email)) {
      throw new ApplicationException(
          ErrorCode.USER_EMAIL_EXISTS, "A user with this email already exists.");
    }

    AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), UserRole.USER);
    AppUser savedUser = userRepository.save(user);

    log.info("Registered user id={}", savedUser.getId());

    return new RegisterResponse(
        savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
  }

  public LoginResponse login(LoginRequest request) {
    String username = normalizeEmail(request.username());

    try {
      var authentication =
          authManager.authenticate(
              new UsernamePasswordAuthenticationToken(username, request.password()));

      log.info("Login succeeded for username={}", username);
      return jwtTokenService.createAccessToken(authentication);
    } catch (AuthenticationException e) {
      log.warn("Login failed for username={}", username);
      throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
    }
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
