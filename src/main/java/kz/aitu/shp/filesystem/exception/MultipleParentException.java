package kz.aitu.shp.filesystem.exception;

/**
 * Thrown when attempting to attach an element that already belongs to another parent directory.
 */
public class MultipleParentException extends FileSystemException {
    private static final long serialVersionUID = 1L;

    public MultipleParentException(String message) {
        super(message);
    }
}
