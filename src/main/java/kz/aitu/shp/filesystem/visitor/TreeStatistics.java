package kz.aitu.shp.filesystem.visitor;

/**
 * Immutable snapshot of collected file system structural statistics.
 *
 * @param fileCount total number of visited file nodes
 * @param directoryCount total number of visited directory nodes (including root when applicable)
 * @param maxDepth maximum tree depth reached during traversal (root depth = 0)
 */
public record TreeStatistics(int fileCount, int directoryCount, int maxDepth) {
}
