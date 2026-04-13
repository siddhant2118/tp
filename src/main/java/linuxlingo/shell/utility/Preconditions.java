package linuxlingo.shell.utility;

/**
 * Utility class providing common precondition checks for method arguments.
 *
 * <p>These methods are intended to enforce basic validation rules such as
 * non-null and non-blank constraints. If a precondition is violated,
 * an {@link IllegalArgumentException} is thrown with a descriptive message.</p>
 *
 * <p>This class cannot be instantiated.</p>
 */
public class Preconditions {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Preconditions() {}

    /**
     * Ensures that the given value is not {@code null}.
     *
     * @param value the object to check
     * @param fieldName the name of the field or parameter being validated
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not  be null");
        }
    }

    /**
     * Ensures that the given string is neither {@code null} nor blank.
     *
     * <p>A string is considered blank if it is empty or contains only
     * whitespace characters.</p>
     *
     * @param value the string to check
     * @param fieldName the name of the field or parameter being validated
     * @throws IllegalArgumentException if {@code value} is {@code null} or blank
     */
    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }
}
