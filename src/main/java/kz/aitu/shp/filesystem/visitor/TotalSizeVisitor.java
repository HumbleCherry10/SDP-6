package kz.aitu.shp.filesystem.visitor;

import kz.aitu.shp.filesystem.model.DirectoryNode;
import kz.aitu.shp.filesystem.model.FileNode;
import kz.aitu.shp.filesystem.model.FileSystemElement;

import java.util.Objects;

/**
 * Visitor computing the cumulative byte size of all files across an entire subtree.
 * Directories contribute zero bytes directly.
 * Traversal is driven by the visitor itself across all reachable children.
 */
public class TotalSizeVisitor implements FileSystemVisitor {

    private long totalSize = 0L;

    /**
     * Visits a leaf file and accumulates its byte size.
     *
     * @param file the file node
     */
    @Override
    public void visit(FileNode file) {
        Objects.requireNonNull(file, "File node cannot be null");
        this.totalSize += file.getSize();
    }

    /**
     * Visits a directory node.
     * Directories do not contribute their own size; instead, the visitor traverses
     * all reachable child elements recursively.
     *
     * @param directory the directory node
     */
    @Override
    public void visit(DirectoryNode directory) {
        Objects.requireNonNull(directory, "Directory node cannot be null");
        for (FileSystemElement child : directory.getChildren()) {
            child.accept(this);
        }
    }

    /**
     * @return the cumulative size in bytes accumulated so far
     */
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * Resets internal accumulated state to allow safe reuse of the visitor instance.
     */
    public void reset() {
        this.totalSize = 0L;
    }
}
