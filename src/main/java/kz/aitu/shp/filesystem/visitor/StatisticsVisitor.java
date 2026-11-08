package kz.aitu.shp.filesystem.visitor;

import kz.aitu.shp.filesystem.model.DirectoryNode;
import kz.aitu.shp.filesystem.model.FileNode;
import kz.aitu.shp.filesystem.model.FileSystemElement;

import java.util.Objects;

/**
 * Visitor collecting hierarchy metrics across the tree:
 * 1. File count.
 * 2. Directory count (including the visited root when it is a directory).
 * 3. Maximum tree depth (with the visited root at depth = 0).
 *
 * Implements the extension boundary (R8) without modifying FileNode,
 * DirectoryNode, or their accept() methods.
 */
public class StatisticsVisitor implements FileSystemVisitor {

    private int fileCount = 0;
    private int directoryCount = 0;
    private int maxDepth = 0;
    private int currentDepth = 0;

    @Override
    public void visit(FileNode file) {
        Objects.requireNonNull(file, "File node cannot be null");
        this.fileCount++;
        this.maxDepth = Math.max(this.maxDepth, this.currentDepth);
    }

    @Override
    public void visit(DirectoryNode directory) {
        Objects.requireNonNull(directory, "Directory node cannot be null");
        this.directoryCount++;
        this.maxDepth = Math.max(this.maxDepth, this.currentDepth);

        for (FileSystemElement child : directory.getChildren()) {
            this.currentDepth++;
            child.accept(this);
            this.currentDepth--;
        }
    }

    /**
     * @return the number of visited files
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * @return the number of visited directories (including root)
     */
    public int getDirectoryCount() {
        return directoryCount;
    }

    /**
     * @return the maximum depth reached (root depth = 0)
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * @return an immutable snapshot of current statistics
     */
    public TreeStatistics getStatistics() {
        return new TreeStatistics(fileCount, directoryCount, maxDepth);
    }

    /**
     * Clears counters and depths to allow clean reuse of this visitor instance.
     */
    public void reset() {
        this.fileCount = 0;
        this.directoryCount = 0;
        this.maxDepth = 0;
        this.currentDepth = 0;
    }
}
