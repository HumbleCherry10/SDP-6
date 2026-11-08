package kz.aitu.shp.filesystem.exception;

/**
 * Thrown when a file is initialized or updated with a negative byte size.
 */
public class InvalidFileSizeException extends FileSystemException {
    private static final long serialVersionUID = 1L;

    public InvalidFileSizeException(String message) {
        super(message);
    }
}
