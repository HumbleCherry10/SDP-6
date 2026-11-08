package kz.aitu.shp.filesystem.model;

import kz.aitu.shp.filesystem.exception.InvalidFileSizeException;
import kz.aitu.shp.filesystem.visitor.FileSystemVisitor;

import java.util.Objects;

/**
 * Represents a concrete leaf file holding a name and a non-negative byte size.
 */
public class FileNode implements FileSystemElement {

    private final String name;
    private final long size;
    private DirectoryNode parent;

    /**
     * Constructs a new FileNode.
     *
     * @param name non-blank file name
     * @param size non-negative size in bytes
     * @throws IllegalArgumentException if the name is null or blank
     * @throws InvalidFileSizeException if the size is negative
     */
    public FileNode(String name, long size) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or blank");
        }
        if (size < 0) {
            throw new InvalidFileSizeException("File size cannot be negative. Provided: " + size);
        }
        this.name = name.trim();
        this.size = size;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * @return non-negative file size in bytes
     */
    public long getSize() {
        return size;
    }

    @Override
    public DirectoryNode getParent() {
        return parent;
    }

    @Override
    public void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    @Override
    public void accept(FileSystemVisitor visitor) {
        Objects.requireNonNull(visitor, "Visitor cannot be null");
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "FileNode[name='" + name + "', size=" + size + " bytes]";
    }
}
