package th.ac.kku.freelance_hub.exception.handling;

/** Entry point controls authentication meaning and preserves request-specific codes. */
public record ErrorContext(Source source, String path) {
    public enum Source { MVC, AUTHENTICATION, AUTHORIZATION, FILTER_DEPENDENCY }
    public static ErrorContext mvc(String path) { return new ErrorContext(Source.MVC, path); }
    public boolean isAuthOrClient() {
        return path != null && (path.startsWith("/api/auth") || path.startsWith("/api/users") || path.startsWith("/api/clients"));
    }
    public boolean isClient() { return path != null && path.startsWith("/api/clients"); }
}
