package aws;

public class Exception extends java.lang.Exception {
    public static class IdTokenExpiredException extends RuntimeException {}
    public static class IdTokenExpiredSoonException extends RuntimeException {}
}