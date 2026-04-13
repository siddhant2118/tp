package linuxlingo.shell.utility;

public class Preconditions {

    private Preconditions() {} // prevent instantiation as this class is a utility class only with static methods

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not  be null");
        }
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }
}
