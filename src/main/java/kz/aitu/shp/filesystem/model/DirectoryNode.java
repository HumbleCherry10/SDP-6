package kz.aitu.shp.filesystem.model;

import kz.aitu.shp.filesystem.exception.CycleDetectedException;
import kz.aitu.shp.filesystem.exception.DuplicateNameException;
import kz.aitu.shp.filesystem.exception.MultipleParentException;
import kz.aitu.shp.filesystem.visitor.FileSystemVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Composite element in the hierarchical file system representing a named directory.
 * Maintains named child elements, enforces tree integrity invariants, and exposes
 * an unmodifiable view of its children.
 */
public class DirectoryNode implements FileSystemElement {

    private final String name;
    private final Map<String, FileSystemElement> children = new LinkedHashMap<>();
    private DirectoryNode parent;

    /**
     * Constructs a new DirectoryNode.
     *
     * @param name non-blank directory name
     * @throws IllegalArgumentException if the name is null or blank
     */
    public DirectoryNode(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Directory name cannot be null or blank");
        }
        this.name = name.trim();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DirectoryNode getParent() {
        return parent;
    }

    @Override
    public void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    /**
     * Adds a child element to this directory while enforcing tree integrity invariants:
     * 1. Rejects null child.
     * 2. Rejects duplicate sibling names.
     * 3. Rejects multiple parent ownership.
     * 4. Rejects cyclical structures (self-containment or ancestor containment).
     *
     * @param element the child element to attach
     * @return this directory instance for fluent composition
     */
    public DirectoryNode addChild(FileSystemElement element) {
        Objects.requireNonNull(element, "Child element cannot be null");

        // 1. Duplicate sibling name check
        if (children.containsKey(element.getName())) {
            throw new DuplicateNameException(
                    "Duplicate sibling name '" + element.getName() + "' in directory '" + this.name + "'"
            );
        }

        // 2. Multiple parent ownership check
        if (element.getParent() != null && element.getParent() != this) {
            throw new MultipleParentException(
                    "Element '" + element.getName() + "' already belongs to parent directory '" +
                            element.getParent().getName() + "'"
            );
        }

        // 3. Cycle prevention (checks whether element is this node or an ancestor of this node)
        DirectoryNode currentAncestor = this;
        while (currentAncestor != null) {
            if (currentAncestor == element) {
                throw new CycleDetectedException(
                        "Cannot add '" + element.getName() + "' into '" + this.name + "': hierarchy cycle detected"
                );
            }
            currentAncestor = currentAncestor.getParent();
        }

        // Invariants satisfied: attach element and bind parent reference
        children.put(element.getName(), element);
        element.setParent(this);
        return this;
    }

    /**
     * Removes a child by name and unbinds its parent reference.
     *
     * @param childName the name of the child to remove
     * @return the removed element, or empty if not found
     */
    public Optional<FileSystemElement> removeChild(String childName) {
        FileSystemElement removed = children.remove(childName);
        if (removed != null) {
            removed.setParent(null);
            return Optional.of(removed);
        }
        return Optional.empty();
    }

    /**
     * Retrieves an unmodifiable snapshot of child elements preserving insertion order.
     *
     * @return read-only list of children
     */
    public List<FileSystemElement> getChildren() {
        return Collections.unmodifiableList(new ArrayList<>(children.values()));
    }

    /**
     * @return number of immediate child elements in this directory
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Double dispatch delegation.
     *
     * @param visitor the file system visitor
     */
    @Override
    public void accept(FileSystemVisitor visitor) {
        Objects.requireNonNull(visitor, "Visitor cannot be null");
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "DirectoryNode[name='" + name + "', childrenCount=" + children.size() + "]";
    }
}
