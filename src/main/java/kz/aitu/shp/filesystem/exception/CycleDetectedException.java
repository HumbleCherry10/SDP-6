package kz.aitu.shp.filesystem.exception;

/**
 * Thrown when an addition would create a cyclical parent-child directory hierarchy.
 */
public class CycleDetectedException extends FileSystemException {
    private static final long serialVersionUID = 1L;

    public CycleDetectedException(String message) {
        super(message);
    }
}
