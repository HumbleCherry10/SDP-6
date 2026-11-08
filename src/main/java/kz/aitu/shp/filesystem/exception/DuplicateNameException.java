package kz.aitu.shp.filesystem.exception;

/**
 * Thrown when attempting to add an element whose name already exists in the target directory.
 */
public class DuplicateNameException extends FileSystemException {
    private static final long serialVersionUID = 1L;

    public DuplicateNameException(String message) {
        super(message);
    }
}
