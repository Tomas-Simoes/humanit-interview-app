package humanit.auth.jwt;

public class JwtSecretConfigurationException extends RuntimeException {
  public JwtSecretConfigurationException(String message) {
    super(message);
  }

  public JwtSecretConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
