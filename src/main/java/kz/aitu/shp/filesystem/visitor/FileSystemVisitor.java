package kz.aitu.shp.filesystem.visitor;

import kz.aitu.shp.filesystem.model.DirectoryNode;
import kz.aitu.shp.filesystem.model.FileNode;

/**
 * Visitor interface declaring overloaded visit methods for all concrete element types
 * in the file system hierarchy.
 */
public interface FileSystemVisitor {

    /**
     * Visit a concrete file element.
     *
     * @param file the file node being visited
     */
    void visit(FileNode file);

    /**
     * Visit a concrete directory element.
     *
     * @param directory the directory node being visited
     */
    void visit(DirectoryNode directory);
}
