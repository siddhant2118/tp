package linuxlingo.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides generic file read/write utilities for persistent storage on the
 * real (host) file system.
 *
 * <p>This is an infrastructure class — fully static, used by
 * {@code VfsSerializer}, the exam module, and any other component that
 * needs disk I/O. Errors are wrapped in {@link StorageException}.</p>
 */
public class Storage {

    /** Default data directory, relative to the application working directory. */
    private static final Path DATA_DIR = Paths.get("data");

    // ─── Read ────────────────────────────────────────────────────

    /**
     * Reads the entire contents of a file as a single string.
     *
     * @param path the file to read.
     * @return the file contents as a string.
     * @throws StorageException if the file cannot be read.
     */
    public static String readFile(Path path) throws StorageException {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new StorageException("Cannot read file: " + path, e);
        }
    }

    /**
     * Reads all lines from a file.
     *
     * @param path the file to read.
     * @return a list of lines without line terminators.
     * @throws StorageException if the file cannot be read.
     */
    public static List<String> readLines(Path path) throws StorageException {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new StorageException("Cannot read file: " + path, e);
        }
    }

    // ─── Write ───────────────────────────────────────────────────

    /**
     * Writes content to a file, creating parent directories if necessary.
     * Overwrites any existing content.
     *
     * @param path    the file to write.
     * @param content the content to write.
     * @throws StorageException if the file cannot be written.
     */
    public static void writeFile(Path path, String content) throws StorageException {
        try {
            ensureDirectory(path.getParent());
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new StorageException("Cannot write file: " + path, e);
        }
    }

    /**
     * Appends content to a file, creating it if it does not exist.
     *
     * @param path    the file to append to.
     * @param content the content to append.
     * @throws StorageException if the file cannot be written.
     */
    public static void appendFile(Path path, String content) throws StorageException {
        try {
            ensureDirectory(path.getParent());
            if (Files.exists(path)) {
                String existing = Files.readString(path);
                Files.writeString(path, existing + content);
            } else {
                Files.writeString(path, content);
            }
        } catch (IOException e) {
            throw new StorageException("Cannot append to file: " + path, e);
        }
    }

    // ─── Query / Delete ──────────────────────────────────────────

    /**
     * Checks whether a path exists on disk.
     *
     * @param path the path to check.
     * @return {@code true} if the path exists.
     */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * Deletes a file if it exists.
     *
     * @param path the file to delete.
     * @return {@code true} if the file was deleted, {@code false} if it did not exist.
     */
    public static boolean delete(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Lists files in a directory that end with a given extension.
     *
     * @param dir       the directory to list.
     * @param extension file extension filter (e.g. {@code ".env"}).
     * @return a list of matching file paths, or an empty list if none found or directory is missing.
     */
    public static List<Path> listFiles(Path dir, String extension) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().endsWith(extension))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    // ─── Directory ───────────────────────────────────────────────

    /**
     * Creates the directory (and any missing parents) if it does not already exist.
     *
     * @param dir the directory to create.
     * @throws StorageException if the directory cannot be created.
     */
    public static void ensureDirectory(Path dir) throws StorageException {
        if (dir == null) {
            return;
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new StorageException("Cannot create directory: " + dir, e);
        }
    }

    // ─── Paths ───────────────────────────────────────────────────

    /**
     * Returns the application-level data directory ({@code data/}).
     */
    public static Path getDataDir() {
        return DATA_DIR;
    }

    /**
     * Returns a subdirectory path under the data directory.
     * For example: {@code getDataSubDir("environments")} → {@code data/environments/}.
     *
     * @param sub the subdirectory name.
     * @return the path to the subdirectory.
     */
    public static Path getDataSubDir(String sub) {
        return DATA_DIR.resolve(sub);
    }
}
