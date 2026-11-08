package kz.aitu.shp.filesystem;

import kz.aitu.shp.filesystem.exception.CycleDetectedException;
import kz.aitu.shp.filesystem.exception.DuplicateNameException;
import kz.aitu.shp.filesystem.exception.InvalidFileSizeException;
import kz.aitu.shp.filesystem.exception.MultipleParentException;
import kz.aitu.shp.filesystem.model.DirectoryNode;
import kz.aitu.shp.filesystem.model.FileNode;
import kz.aitu.shp.filesystem.model.FileSystemElement;
import kz.aitu.shp.filesystem.visitor.FileSystemVisitor;
import kz.aitu.shp.filesystem.visitor.SearchVisitor;
import kz.aitu.shp.filesystem.visitor.StatisticsVisitor;
import kz.aitu.shp.filesystem.visitor.TotalSizeVisitor;
import kz.aitu.shp.filesystem.visitor.TreeStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automated verification suite for the Visitor Pattern In-Memory File System.
 * Covers requirements T1 through T8 comprehensively using JUnit 5.
 */
public class FileSystemVisitorTest {

    // =========================================================================
    // T1: Both element types reach the correct overloaded visit methods
    // =========================================================================
    @Test
    @DisplayName("T1: Double dispatch correctly routes each concrete element type without instanceof")
    void testT1_DoubleDispatchReachesCorrectOverloadedVisitMethods() {
        class DispatchSpyVisitor implements FileSystemVisitor {
            private final List<String> callLog = new ArrayList<>();

            @Override
            public void visit(FileNode file) {
                callLog.add("visit(FileNode):" + file.getName());
            }

            @Override
            public void visit(DirectoryNode directory) {
                callLog.add("visit(DirectoryNode):" + directory.getName());
            }
        }

        DispatchSpyVisitor spy = new DispatchSpyVisitor();
        FileSystemElement file = new FileNode("report.pdf", 1024);
        FileSystemElement directory = new DirectoryNode("documents");

        file.accept(spy);
        assertEquals(List.of("visit(FileNode):report.pdf"), spy.callLog);

        directory.accept(spy);
        assertEquals(
                List.of("visit(FileNode):report.pdf", "visit(DirectoryNode):documents"),
                spy.callLog
        );
    }

    // =========================================================================
    // T2: Size accumulation across multiple nesting levels is correct
    // =========================================================================
    @Test
    @DisplayName("T2: TotalSizeVisitor sums all files in multi-level hierarchy; directories contribute 0")
    void testT2_SizeAccumulationAcrossMultipleNestingLevels() {
        DirectoryNode root = new DirectoryNode("project");
        FileNode rootFile = new FileNode("pom.xml", 2000L);
        root.addChild(rootFile);

        DirectoryNode src = new DirectoryNode("src");
        DirectoryNode main = new DirectoryNode("main");
        FileNode code = new FileNode("Main.java", 4500L);
        main.addChild(code);
        src.addChild(main);
        root.addChild(src);

        DirectoryNode docs = new DirectoryNode("docs");
        FileNode readme = new FileNode("README.md", 1500L);
        DirectoryNode emptyImages = new DirectoryNode("images");
        docs.addChild(readme);
        docs.addChild(emptyImages);
        root.addChild(docs);

        TotalSizeVisitor sizeVisitor = new TotalSizeVisitor();
        root.accept(sizeVisitor);

        // 2000 + 4500 + 1500 = 8000
        assertEquals(8000L, sizeVisitor.getTotalSize());

        // Test running on single file directly
        TotalSizeVisitor singleFileVisitor = new TotalSizeVisitor();
        code.accept(singleFileVisitor);
        assertEquals(4500L, singleFileVisitor.getTotalSize());
    }

    // =========================================================================
    // T3: Search correctly combines both configurable filter conditions
    // =========================================================================
    @Test
    @DisplayName("T3: SearchVisitor requires both name criterion AND inclusive minSize threshold")
    void testT3_SearchCombinesNameCriterionAndMinSizeThreshold() {
        DirectoryNode root = new DirectoryNode("logs_archive");
        root.addChild(new FileNode("server-error.log", 500L));
        root.addChild(new FileNode("server-debug.log", 150L));
        root.addChild(new FileNode("access.log", 800L));
        root.addChild(new FileNode("config.xml", 900L));

        // Name must contain "server" AND size >= 200
        SearchVisitor searchVisitor = new SearchVisitor("server", 200L);
        root.accept(searchVisitor);

        List<String> results = searchVisitor.getMatchingPaths();
        assertEquals(1, results.size());
        assertEquals("server-error.log", results.get(0));

        // Predicate-based search: ends with ".log" AND size >= 500
        SearchVisitor extVisitor = new SearchVisitor(name -> name.endsWith(".log"), 500L);
        root.accept(extVisitor);
        assertEquals(List.of("access.log", "server-error.log"), extVisitor.getMatchingPaths());
    }

    // =========================================================================
    // T4: Search produces correct, deterministic, root-relative paths
    // =========================================================================
    @Test
    @DisplayName("T4: Search produces deterministic, root-relative paths sorted lexicographically with '/'")
    void testT4_SearchProducesDeterministicRootRelativePaths() {
        DirectoryNode root = new DirectoryNode("workspace");

        DirectoryNode zFolder = new DirectoryNode("z_folder");
        zFolder.addChild(new FileNode("script.sh", 100L));

        DirectoryNode aFolder = new DirectoryNode("a_folder");
        aFolder.addChild(new FileNode("delta.txt", 100L));
        DirectoryNode subFolder = new DirectoryNode("sub");
        subFolder.addChild(new FileNode("alpha.txt", 100L));
        aFolder.addChild(subFolder);

        root.addChild(zFolder);
        root.addChild(aFolder);
        root.addChild(new FileNode("root_file.txt", 100L));

        SearchVisitor visitor = new SearchVisitor("", 50L);
        root.accept(visitor);

        List<String> paths = visitor.getMatchingPaths();

        List<String> expected = List.of(
                "a_folder/delta.txt",
                "a_folder/sub/alpha.txt",
                "root_file.txt",
                "z_folder/script.sh"
        );

        assertEquals(expected, paths);
        for (String path : paths) {
            assertFalse(path.startsWith("/"), "Path must not start with leading slash: " + path);
            assertFalse(path.contains("\\"), "Path must use '/' separator, not '\\': " + path);
        }
    }

    // =========================================================================
    // T5: Statistics are correct for nesting, counting and maximum depth
    // =========================================================================
    @Test
    @DisplayName("T5: StatisticsVisitor calculates file count, directory count and max depth (root depth = 0)")
    void testT5_StatisticsCorrectForNestingCountingAndDepth() {
        // Tree:
        // root (depth 0)
        // ├── dir1 (depth 1)
        // │   └── dir2 (depth 2)
        // │       └── deep_file.txt (depth 3)
        // ├── empty_dir (depth 1)
        // └── top_file.txt (depth 1)
        DirectoryNode root = new DirectoryNode("root");

        DirectoryNode dir1 = new DirectoryNode("dir1");
        DirectoryNode dir2 = new DirectoryNode("dir2");
        dir2.addChild(new FileNode("deep_file.txt", 300L));
        dir1.addChild(dir2);

        DirectoryNode emptyDir = new DirectoryNode("empty_dir");

        root.addChild(dir1);
        root.addChild(emptyDir);
        root.addChild(new FileNode("top_file.txt", 150L));

        StatisticsVisitor visitor = new StatisticsVisitor();
        root.accept(visitor);

        // Directories: root, dir1, dir2, empty_dir = 4
        // Files: deep_file.txt, top_file.txt = 2
        // Max depth: root(0) -> dir1(1) -> dir2(2) -> deep_file(3) => 3
        assertEquals(2, visitor.getFileCount());
        assertEquals(4, visitor.getDirectoryCount());
        assertEquals(3, visitor.getMaxDepth());

        TreeStatistics stats = visitor.getStatistics();
        assertEquals(2, stats.fileCount());
        assertEquals(4, stats.directoryCount());
        assertEquals(3, stats.maxDepth());
    }

    // =========================================================================
    // T6: Empty structures and no-match search cases are handled
    // =========================================================================
    @Test
    @DisplayName("T6: Empty structures and search misses are gracefully handled without crashing")
    void testT6_EmptyStructuresAndNoMatchCasesHandled() {
        DirectoryNode emptyRoot = new DirectoryNode("empty");

        // 1. Size of empty tree
        TotalSizeVisitor sizeVisitor = new TotalSizeVisitor();
        emptyRoot.accept(sizeVisitor);
        assertEquals(0L, sizeVisitor.getTotalSize());

        // 2. Statistics of empty tree
        StatisticsVisitor statsVisitor = new StatisticsVisitor();
        emptyRoot.accept(statsVisitor);
        assertEquals(0, statsVisitor.getFileCount());
        assertEquals(1, statsVisitor.getDirectoryCount()); // Root is visited directory
        assertEquals(0, statsVisitor.getMaxDepth());

        // 3. Search on empty tree
        SearchVisitor searchEmpty = new SearchVisitor("data", 0L);
        emptyRoot.accept(searchEmpty);
        assertNotNull(searchEmpty.getMatchingPaths());
        assertTrue(searchEmpty.getMatchingPaths().isEmpty());

        // 4. Search with no matches in populated tree
        DirectoryNode populated = new DirectoryNode("populated");
        populated.addChild(new FileNode("image.png", 500L));

        SearchVisitor searchMiss = new SearchVisitor("video", 100L);
        populated.accept(searchMiss);
        assertTrue(searchMiss.getMatchingPaths().isEmpty());

        SearchVisitor sizeMiss = new SearchVisitor("image", 10000L);
        populated.accept(sizeMiss);
        assertTrue(sizeMiss.getMatchingPaths().isEmpty());
    }

    // =========================================================================
    // T7: Repeat executions do not leak or multiply accumulated state
    // =========================================================================
    @Test
    @DisplayName("T7: Multiple runs with reset do not leak or multiply state")
    void testT7_RepeatExecutionsDoNotLeakOrMultiplyAccumulatedState() {
        DirectoryNode root = new DirectoryNode("repeat_test");
        root.addChild(new FileNode("item1.txt", 100L));
        root.addChild(new FileNode("item2.txt", 200L));

        // TotalSizeVisitor reset test
        TotalSizeVisitor sizeVisitor = new TotalSizeVisitor();
        root.accept(sizeVisitor);
        assertEquals(300L, sizeVisitor.getTotalSize());

        sizeVisitor.reset();
        root.accept(sizeVisitor);
        assertEquals(300L, sizeVisitor.getTotalSize(), "Second run after reset must not accumulate 600");

        // SearchVisitor reset test
        SearchVisitor searchVisitor = new SearchVisitor("item", 50L);
        root.accept(searchVisitor);
        assertEquals(2, searchVisitor.getMatchingPaths().size());

        searchVisitor.reset();
        root.accept(searchVisitor);
        assertEquals(2, searchVisitor.getMatchingPaths().size(), "Second search after reset must not duplicate results");

        // StatisticsVisitor reset test
        StatisticsVisitor statsVisitor = new StatisticsVisitor();
        root.accept(statsVisitor);
        assertEquals(2, statsVisitor.getFileCount());
        assertEquals(1, statsVisitor.getDirectoryCount());
        assertEquals(1, statsVisitor.getMaxDepth());

        statsVisitor.reset();
        root.accept(statsVisitor);
        assertEquals(2, statsVisitor.getFileCount());
        assertEquals(1, statsVisitor.getDirectoryCount());
        assertEquals(1, statsVisitor.getMaxDepth());
    }

    // =========================================================================
    // T8: Invalid tree mutations are rejected while valid nesting remains supported
    // =========================================================================
    @Test
    @DisplayName("T8: Tree integrity enforces non-negative sizes, unique sibling names, single parent, and no cycles")
    void testT8_InvalidTreeMutationsRejectedWhileValidNestingSupported() {
        // 1. Negative file size rejection
        assertThrows(InvalidFileSizeException.class, () -> new FileNode("bad.dat", -1L));
        assertThrows(IllegalArgumentException.class, () -> new FileNode("", 100L));
        assertThrows(IllegalArgumentException.class, () -> new DirectoryNode("   "));

        // 2. Duplicate sibling names rejection
        DirectoryNode parent = new DirectoryNode("parent");
        parent.addChild(new FileNode("config.json", 100L));
        assertThrows(DuplicateNameException.class, () -> parent.addChild(new FileNode("config.json", 200L)));
        assertThrows(DuplicateNameException.class, () -> parent.addChild(new DirectoryNode("config.json")));

        // 3. Multiple parent ownership rejection
        DirectoryNode otherParent = new DirectoryNode("otherParent");
        FileNode sharedFile = new FileNode("unique.doc", 50L);
        parent.addChild(sharedFile);
        assertThrows(MultipleParentException.class, () -> otherParent.addChild(sharedFile));

        // 4. Cycle prevention: Self-containment
        DirectoryNode cycleDirA = new DirectoryNode("dirA");
        assertThrows(CycleDetectedException.class, () -> cycleDirA.addChild(cycleDirA));

        // 5. Cycle prevention: Indirect ancestor containment
        DirectoryNode grandParent = new DirectoryNode("grandParent");
        DirectoryNode middle = new DirectoryNode("middle");
        DirectoryNode child = new DirectoryNode("child");

        grandParent.addChild(middle);
        middle.addChild(child);

        // Attempting to add grandParent into child would create an ancestor loop
        assertThrows(CycleDetectedException.class, () -> child.addChild(grandParent));

        // 6. Valid nesting works seamlessly
        assertDoesNotThrow(() -> {
            DirectoryNode validA = new DirectoryNode("validA");
            DirectoryNode validB = new DirectoryNode("validB");
            validA.addChild(validB);
            validB.addChild(new FileNode("valid.txt", 123L));
        });
    }
}
