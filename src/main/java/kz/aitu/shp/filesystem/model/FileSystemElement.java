package kz.aitu.shp.filesystem.model;

import kz.aitu.shp.filesystem.visitor.FileSystemVisitor;

/**
 * Common abstraction for all in-memory file system elements.
 * Defines the contract for naming, parent-link hierarchy, and double dispatch acceptance.
 */
public interface FileSystemElement {

    /**
     * @return the name of the file system element.
     */
    String getName();

    /**
     * @return the parent directory of this element, or null if it is an unattached root.
     */
    DirectoryNode getParent();

    /**
     * Sets or clears the parent directory reference for this element.
     * Intended for internal hierarchy coordination.
     *
     * @param parent the enclosing directory node
     */
    void setParent(DirectoryNode parent);

    /**
     * Double dispatch hook accepting a visitor.
     *
     * @param visitor the visitor executing an operation across the hierarchy
     */
    void accept(FileSystemVisitor visitor);
}
