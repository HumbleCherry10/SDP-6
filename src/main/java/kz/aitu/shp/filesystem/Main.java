package kz.aitu.shp.filesystem;

import kz.aitu.shp.filesystem.exception.FileSystemException;
import kz.aitu.shp.filesystem.model.DirectoryNode;
import kz.aitu.shp.filesystem.model.FileNode;
import kz.aitu.shp.filesystem.visitor.SearchVisitor;
import kz.aitu.shp.filesystem.visitor.StatisticsVisitor;
import kz.aitu.shp.filesystem.visitor.TotalSizeVisitor;
import kz.aitu.shp.filesystem.visitor.TreeStatistics;

import java.util.List;

/**
 * Deterministic executable demonstration of the Visitor Pattern applied to an in-memory file system.
 * Constructs an enterprise project tree, runs all three visitors, and displays formatted results.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  ASTANA IT UNIVERSITY | ShP-2216 Software Design Patterns");
        System.out.println("  Assignment 06: Visitor Pattern - Advanced In-Memory File System");
        System.out.println("================================================================================\n");

        // 1. Construct realistic hierarchical file system tree
        DirectoryNode root = createSampleProjectTree();
        System.out.println("[1] In-Memory File System Tree successfully constructed.");
        System.out.println("    Root directory: '" + root.getName() + "' with arbitrary nested directories.\n");

        // 2. Operation 1: TotalSizeVisitor
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("  OPERATION 1: TotalSizeVisitor (R3)");
        System.out.println("--------------------------------------------------------------------------------");
        TotalSizeVisitor sizeVisitor = new TotalSizeVisitor();
        root.accept(sizeVisitor);
        long totalBytes = sizeVisitor.getTotalSize();
        System.out.printf("Total Subtree Byte Size: %d bytes (%.2f KB)%n%n", totalBytes, totalBytes / 1024.0);

        // 3. Operation 2: SearchVisitor
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("  OPERATION 2: SearchVisitor (R4)");
        System.out.println("--------------------------------------------------------------------------------");

        // Query A: Java source files with size >= 3000 bytes
        System.out.println("Query A: Filename contains '.java' AND size >= 3000 bytes");
        SearchVisitor javaSearch = new SearchVisitor(".java", 3000L);
        root.accept(javaSearch);
        printSearchResults(javaSearch.getMatchingPaths());

        // Query B: Large files across tree with size >= 5000 bytes
        System.out.println("Query B: All files with size >= 5000 bytes");
        SearchVisitor largeFilesSearch = new SearchVisitor("", 5000L);
        root.accept(largeFilesSearch);
        printSearchResults(largeFilesSearch.getMatchingPaths());

        // Query C: Configuration / documentation files (.json or .properties)
        System.out.println("Query C: Filename predicate [ends with .json OR .properties] AND size >= 0 bytes");
        SearchVisitor configSearch = new SearchVisitor(
                name -> name.endsWith(".json") || name.endsWith(".properties"),
                0L
        );
        root.accept(configSearch);
        printSearchResults(configSearch.getMatchingPaths());

        // 4. Operation 3: StatisticsVisitor
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("  OPERATION 3: StatisticsVisitor (R5 & R8)");
        System.out.println("--------------------------------------------------------------------------------");
        StatisticsVisitor statsVisitor = new StatisticsVisitor();
        root.accept(statsVisitor);
        TreeStatistics stats = statsVisitor.getStatistics();
        System.out.println("Hierarchy Statistics Summary:");
        System.out.println("  * Total Files Visited:      " + stats.fileCount());
        System.out.println("  * Total Directories Visited:" + stats.directoryCount() + " (including root)");
        System.out.println("  * Maximum Tree Depth:       " + stats.maxDepth() + " (root depth = 0)\n");

        // 5. Tree Integrity Demonstration (R6)
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("  TREE INTEGRITY ENFORCEMENT (R6)");
        System.out.println("--------------------------------------------------------------------------------");
        demonstrateIntegrityGuards(root);

        System.out.println("================================================================================");
        System.out.println("  DEMONSTRATION COMPLETED SUCCESSFULLY");
        System.out.println("================================================================================");
    }

    private static DirectoryNode createSampleProjectTree() {
        DirectoryNode repo = new DirectoryNode("e-commerce-core");

        // Top-level files
        repo.addChild(new FileNode("pom.xml", 2800L));
        repo.addChild(new FileNode("README.md", 1450L));
        repo.addChild(new FileNode(".gitignore", 160L));

        // src directory
        DirectoryNode src = new DirectoryNode("src");
        DirectoryNode main = new DirectoryNode("main");
        DirectoryNode test = new DirectoryNode("test");

        DirectoryNode javaPkg = new DirectoryNode("java");
        javaPkg.addChild(new FileNode("Application.java", 3400L));
        javaPkg.addChild(new FileNode("OrderService.java", 4800L));
        javaPkg.addChild(new FileNode("PaymentProcessor.java", 2900L));

        DirectoryNode resources = new DirectoryNode("resources");
        resources.addChild(new FileNode("application.properties", 620L));
        resources.addChild(new FileNode("logback.xml", 1100L));

        main.addChild(javaPkg);
        main.addChild(resources);

        DirectoryNode testJava = new DirectoryNode("java");
        testJava.addChild(new FileNode("OrderServiceTest.java", 3900L));
        test.addChild(testJava);

        src.addChild(main);
        src.addChild(test);
        repo.addChild(src);

        // docs directory
        DirectoryNode docs = new DirectoryNode("docs");
        docs.addChild(new FileNode("architecture-spec.json", 6400L));
        docs.addChild(new FileNode("system-design.pdf", 24500L));
        docs.addChild(new FileNode("release-notes.txt", 950L));
        repo.addChild(docs);

        // target build directory
        DirectoryNode target = new DirectoryNode("target");
        target.addChild(new FileNode("ecommerce-core-1.0.jar", 85000L));
        target.addChild(new FileNode("build.log", 3200L));
        repo.addChild(target);

        // empty directory
        repo.addChild(new DirectoryNode("empty-temp"));

        return repo;
    }

    private static void printSearchResults(List<String> paths) {
        if (paths.isEmpty()) {
            System.out.println("  (No matching files found)");
        } else {
            for (int i = 0; i < paths.size(); i++) {
                System.out.printf("  [%d] %s%n", i + 1, paths.get(i));
            }
        }
        System.out.println();
    }

    private static void demonstrateIntegrityGuards(DirectoryNode root) {
        // Attempt duplicate sibling name
        try {
            System.out.print("1. Testing duplicate sibling prevention: ");
            root.addChild(new FileNode("pom.xml", 500L));
            System.out.println("FAILED (allowed duplicate)");
        } catch (FileSystemException e) {
            System.out.println("PASSED -> Caught: " + e.getMessage());
        }

        // Attempt negative file size
        try {
            System.out.print("2. Testing negative size prevention:    ");
            new FileNode("invalid.bin", -50L);
            System.out.println("FAILED (allowed negative size)");
        } catch (FileSystemException e) {
            System.out.println("PASSED -> Caught: " + e.getMessage());
        }

        // Attempt cyclical hierarchy (adding root into one of its subdirectories)
        try {
            System.out.print("3. Testing cycle prevention:            ");
            DirectoryNode docs = (DirectoryNode) root.getChildren().stream()
                    .filter(c -> c.getName().equals("docs"))
                    .findFirst()
                    .orElseThrow();
            docs.addChild(root);
            System.out.println("FAILED (allowed cycle)");
        } catch (FileSystemException e) {
            System.out.println("PASSED -> Caught: " + e.getMessage());
        }
        System.out.println();
    }
}
