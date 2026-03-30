package linuxlingo.shell;

public class Preconditions {

    private Preconditions() {} // prevent instantiation as this class is a utility class only with static methods

    static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not  be null");
        }
    }

    static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }
}
