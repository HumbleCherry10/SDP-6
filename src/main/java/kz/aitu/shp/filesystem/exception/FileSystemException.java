package kz.aitu.shp.filesystem.exception;

/**
 * Base unchecked exception for all file system integrity and operation errors.
 */
public class FileSystemException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FileSystemException(String message) {
        super(message);
    }

    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
