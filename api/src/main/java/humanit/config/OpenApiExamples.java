package humanit.config;

public final class OpenApiExamples {
    public static final String VALIDATION_PROBLEM = """
            {
              "type": "/problems/validation-failed",
              "title": "Validation failed",
              "status": 400,
              "detail": "Request validation failed.",
              "errorCode": "VALIDATION_FAILED",
              "errors": [
                {
                  "field": "email",
                  "message": "must be a well-formed email address"
                }
              ]
            }
            """;

    public static final String CLIENT_NOT_FOUND_PROBLEM = """
            {
              "type": "/problems/client-not-found",
              "title": "Client not found",
              "status": 404,
              "detail": "Client 1 was not found.",
              "errorCode": "CLIENT_NOT_FOUND"
            }
            """;

    public static final String DOCUMENT_NOT_FOUND_PROBLEM = """
            {
              "type": "/problems/document-not-found",
              "title": "Document not found",
              "status": 404,
              "detail": "Document 10 was not found for client 1.",
              "errorCode": "DOCUMENT_NOT_FOUND"
            }
            """;

    public static final String CLIENT_CONFLICT_PROBLEM = """
            {
              "type": "/problems/client-email-already-exists",
              "title": "Client email already exists",
              "status": 409,
              "detail": "A client with this email already exists.",
              "errorCode": "CLIENT_EMAIL_EXISTS"
            }
            """;

    public static final String DOCUMENT_CONFLICT_PROBLEM = """
            {
              "type": "/problems/document-number-already-exists",
              "title": "Document number already exists",
              "status": 409,
              "detail": "A document with this number already exists for client 1.",
              "errorCode": "DOCUMENT_NUMBER_EXISTS"
            }
            """;

    public static final String INVALID_CREDENTIALS_PROBLEM = """
            {
              "type": "/problems/invalid-credentials",
              "title": "Invalid credentials",
              "status": 401,
              "detail": "Invalid username or password",
              "errorCode": "INVALID_CREDENTIALS"
            }
            """;

    public static final String USER_EMAIL_CONFLICT_PROBLEM = """
            {
              "type": "/problems/user-email-already-exists",
              "title": "User email already exists",
              "status": 409,
              "detail": "A user with this email already exists.",
              "errorCode": "USER_EMAIL_EXISTS"
            }
            """;

    private OpenApiExamples() {
    }
}
