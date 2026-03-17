package linuxlingo.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extracts bundled question bank resources from the JAR on first run.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Bundled resources</h3>
 * The following files are expected in the JAR under {@code /questions/}:
 * <ul>
 *   <li>file-management.txt</li>
 *   <li>text-processing.txt</li>
 *   <li>permissions.txt</li>
 *   <li>navigation.txt</li>
 *   <li>piping-redirection.txt</li>
 * </ul>
 *
 * <h3>Extraction logic</h3>
 * If {@code data/questions/} does not exist, create it and copy all bundled
 * files. If it already exists, do nothing (user may have customized files).
 * Also ensures {@code data/environments/} exists for VFS snapshots.
 *
 * <h3>Dependencies</h3>
 * Uses {@link Storage#ensureDirectory(Path)} for directory creation (infrastructure).
 * Uses {@link Class#getResourceAsStream(String)} to read from the JAR.
 */
public class ResourceExtractor {
    private static final Logger LOGGER = Logger.getLogger(ResourceExtractor.class.getName());

    private static final String[] BUNDLED_QUESTIONS = {
        "file-management.txt",
        "text-processing.txt",
        "permissions.txt",
        "navigation.txt",
        "piping-redirection.txt"
    };

    /**
     * Extract bundled resources if they have not already been extracted.
     *
     * @param dataDir the application data directory (e.g. {@code data/})
     * @throws StorageException if directory creation or file copy fails
     */
    public static void extractIfNeeded(Path dataDir) throws StorageException {
        Path validatedDataDir = Objects.requireNonNull(dataDir, "dataDir must not be null");
        Path questionsDir = validatedDataDir.resolve("questions");
        Path environmentsDir = validatedDataDir.resolve("environments");

        if (!Files.exists(questionsDir)) {
            Storage.ensureDirectory(questionsDir);
            for (String fileName : BUNDLED_QUESTIONS) {
                try {
                    extractResource("/questions/" + fileName, questionsDir.resolve(fileName));
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to extract resource: " + fileName, e);
                    throw new StorageException("Failed to extract resource: " + fileName, e);
                }
            }
        }

        if (!Files.exists(environmentsDir)) {
            Storage.ensureDirectory(environmentsDir);
        }

        assert Files.exists(environmentsDir) : "environments directory should exist after extraction";
    }

    /**
     * Copy a single resource from the JAR to a target path on disk.
     *
     * @param resourcePath JAR resource path (e.g. "/questions/navigation.txt")
     * @param targetPath   target file path on disk
     * @throws IOException if the resource cannot be read or the file cannot be written
     */
    private static void extractResource(String resourcePath, Path targetPath) throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        Objects.requireNonNull(targetPath, "targetPath must not be null");
        try (InputStream is = ResourceExtractor.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Bundled resource not found: " + resourcePath);
            }
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
