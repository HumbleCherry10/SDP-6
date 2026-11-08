package kz.aitu.shp.filesystem.visitor;

import kz.aitu.shp.filesystem.model.DirectoryNode;
import kz.aitu.shp.filesystem.model.FileNode;
import kz.aitu.shp.filesystem.model.FileSystemElement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Visitor that searches the hierarchy for files satisfying both:
 * 1. A configurable filename criterion (substring or custom predicate).
 * 2. An inclusive minimum-size threshold (size >= minSizeThreshold).
 *
 * Matching paths are returned relative to the chosen traversal root,
 * separated by '/', and deterministically sorted in natural lexicographical order.
 */
public class SearchVisitor implements FileSystemVisitor {

    private final Predicate<String> namePredicate;
    private final long minSizeThreshold;

    private final Deque<String> pathStack = new ArrayDeque<>();
    private final List<String> matches = new ArrayList<>();
    private boolean isRootEncountered = false;

    /**
     * Constructs a SearchVisitor with a filename substring criterion.
     *
     * @param nameSubstring substring that must be contained within matching file names
     * @param minSizeThreshold inclusive minimum byte size (size >= minSizeThreshold)
     */
    public SearchVisitor(String nameSubstring, long minSizeThreshold) {
        this(
                name -> name != null && name.contains(Objects.requireNonNull(nameSubstring, "Search criterion cannot be null")),
                minSizeThreshold
        );
    }

    /**
     * Constructs a SearchVisitor with a custom filename predicate.
     *
     * @param namePredicate predicate to test file names
     * @param minSizeThreshold inclusive minimum byte size (size >= minSizeThreshold)
     */
    public SearchVisitor(Predicate<String> namePredicate, long minSizeThreshold) {
        if (minSizeThreshold < 0) {
            throw new IllegalArgumentException("Minimum size threshold cannot be negative: " + minSizeThreshold);
        }
        this.namePredicate = Objects.requireNonNull(namePredicate, "Filename predicate cannot be null");
        this.minSizeThreshold = minSizeThreshold;
    }

    @Override
    public void visit(FileNode file) {
        Objects.requireNonNull(file, "File node cannot be null");

        boolean matchesName = namePredicate.test(file.getName());
        boolean matchesSize = file.getSize() >= minSizeThreshold;

        if (matchesName && matchesSize) {
            String relativePath = buildRootRelativePath(file.getName());
            matches.add(relativePath);
        }
    }

    @Override
    public void visit(DirectoryNode directory) {
        Objects.requireNonNull(directory, "Directory node cannot be null");

        if (!isRootEncountered) {
            // First directory visited is treated as the root of this search traversal.
            // Its own name is omitted from relative paths.
            isRootEncountered = true;
            for (FileSystemElement child : directory.getChildren()) {
                child.accept(this);
            }
        } else {
            // Nested directory: push name to path stack, traverse children, then pop.
            pathStack.addLast(directory.getName());
            for (FileSystemElement child : directory.getChildren()) {
                child.accept(this);
            }
            pathStack.removeLast();
        }
    }

    /**
     * Constructs the '/' separated root-relative path for a file.
     */
    private String buildRootRelativePath(String fileName) {
        if (pathStack.isEmpty()) {
            return fileName;
        }
        return String.join("/", pathStack) + "/" + fileName;
    }

    /**
     * Returns an unmodifiable list of matching root-relative file paths,
     * sorted lexicographically for deterministic output.
     *
     * @return sorted matching file paths
     */
    public List<String> getMatchingPaths() {
        List<String> sorted = new ArrayList<>(matches);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Clears internal state and resets the root flag to allow clean reuse.
     */
    public void reset() {
        this.pathStack.clear();
        this.matches.clear();
        this.isRootEncountered = false;
    }
}
