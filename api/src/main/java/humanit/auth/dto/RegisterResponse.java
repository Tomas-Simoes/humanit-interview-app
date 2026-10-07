package humanit.auth.dto;

public record RegisterResponse(
        Long id,
        String email,
        String role) {
}
