# Assignment 06: Visitor Pattern — Advanced In-Memory File System

**Course:** ShP-2216 Software Design Patterns  
**Institution:** Astana IT University  
**Format:** Individual | Java 17 | Apache Maven | JUnit 5  

---

## 1. Project Overview

This project implements an extensible, in-memory hierarchical file system modeling files and directories using the **Visitor Behavioral Design Pattern** and **Composite Pattern**.

The implementation features **double dispatch** to completely eliminate runtime type discrimination (`instanceof`, `getClass()`, pattern-matching switches, and reflection). Three independent operations operate on the file system tree without modifying the element classes:

1. **`TotalSizeVisitor` (R3)**: Recursively computes cumulative subtree byte sizes where directories contribute 0 bytes directly.
2. **`SearchVisitor` (R4)**: Filters files by configurable filename criteria and an inclusive minimum-size threshold (`size >= threshold`), returning root-relative, `/`-separated paths sorted lexicographically.
3. **`StatisticsVisitor` (R5, R8)**: Collects file count, directory count (including root when visited), and maximum tree depth (`root depth = 0`), implemented without modifying element contracts.

---

## 2. Prerequisites & Build Environment

* **Java Development Kit (JDK):** Version 17 or higher.
* **Apache Maven:** Version 3.8+ (A local wrapper script `mvn.cmd` is bundled in the project root preconfigured with JDK 17).

---

## 3. How to Build, Test, and Run

### Run Automated JUnit 5 Verification Suite (T1 – T8)
From the project root directory, execute:

```bash
mvn clean test
```

*(Or on Windows PowerShell / Command Prompt with the bundled wrapper)*:
```powershell
.\mvn.cmd clean test
```

### Run Executable Demonstration Application
To run the interactive showcase demonstrating all three visitor operations, deterministic path resolution, and tree integrity validation:

```bash
mvn compile exec:java
```

*(Or via the bundled wrapper)*:
```powershell
.\mvn.cmd compile exec:java
```

---

## 4. Architecture & Key Design Decisions

### A. Double Dispatch Architecture
Behavioral selection is achieved strictly through two polymorphic method dispatches:
1. **Dispatch 1 (`Element.accept`)**: The client calls `element.accept(visitor)`. Dynamic dispatch invokes either `FileNode.accept()` or `DirectoryNode.accept()`.
2. **Dispatch 2 (`Visitor.visit`)**: Inside the concrete element, it invokes the overloaded visit method with `this` as the argument (`visitor.visit(this)`), routing execution to either `visit(FileNode)` or `visit(DirectoryNode)`.

**No `instanceof`, reflection, or class checks are used.**

### B. Traversal Ownership (R7)
**Design Choice: The Visitor owns and directs the traversal.**  
* **Rationale:** Elements (`FileNode` and `DirectoryNode`) adhere strictly to the Single Responsibility Principle by managing the composite structure, child collections, and tree integrity invariants. If composite elements controlled the traversal, context-dependent state (such as the path stack in `SearchVisitor` or depth level tracking in `StatisticsVisitor`) would either need to be awkwardly passed back and forth or pollute domain nodes. Putting traversal inside each visitor allows each operation to determine its own traversal strategy, push/pop context stacks, or prune branches if needed.

### C. Tree Invariants & Integrity Enforcement (R6)
All directory mutations are validated synchronously in `DirectoryNode.addChild()`:
* **Negative File Size:** Rejected at construction in `FileNode` via `InvalidFileSizeException`.
* **Duplicate Sibling Names:** Rejected in `DirectoryNode.addChild()` via `DuplicateNameException`.
* **Multiple Parent Ownership:** Re-parenting without explicit detaching is rejected via `MultipleParentException`.
* **Cycle Prevention:** Ancestor chain validation is performed in `DirectoryNode.addChild()`. Walking up the parent pointers verifies that neither `this == child` nor is the child an ancestor of `this`, throwing `CycleDetectedException`.

### D. Controlled State & Reusability
Each visitor provides a `reset()` method to clear internal accumulators, path stacks, and counters. This guarantees that visitors can either be reused safely or discarded without stale state leaking across multiple traversals.

---

## 5. Automated Verification Summary (T1 – T8)

| Check | Target | Test Method in `FileSystemVisitorTest` | Status |
| :--- | :--- | :--- | :---: |
| **T1** | Both element types reach correct overloaded visit methods | `testT1_DoubleDispatchReachesCorrectOverloadedVisitMethods()` | **PASSED** |
| **T2** | Size accumulation across multiple nesting levels is correct | `testT2_SizeAccumulationAcrossMultipleNestingLevels()` | **PASSED** |
| **T3** | Search correctly combines both configurable filter conditions | `testT3_SearchCombinesNameCriterionAndMinSizeThreshold()` | **PASSED** |
| **T4** | Search produces correct, deterministic, root-relative paths | `testT4_SearchProducesDeterministicRootRelativePaths()` | **PASSED** |
| **T5** | Statistics are correct for nesting, counting, and max depth | `testT5_StatisticsCorrectForNestingCountingAndDepth()` | **PASSED** |
| **T6** | Empty structures and no-match search cases are handled | `testT6_EmptyStructuresAndNoMatchCasesHandled()` | **PASSED** |
| **T7** | Repeat executions do not leak or multiply accumulated state | `testT7_RepeatExecutionsDoNotLeakOrMultiplyAccumulatedState()` | **PASSED** |
| **T8** | Invalid tree mutations rejected while valid nesting supported | `testT8_InvalidTreeMutationsRejectedWhileValidNestingSupported()` | **PASSED** |

---

## 6. Git Commit History

The repository maintains an incremental commit history demonstrating the development process and the addition of the third visitor (`StatisticsVisitor`) without touching the element classes:

1. `feat: implement file system object model and tree integrity (R1, R6)`
2. `feat: implement visitor interface, double dispatch, size and search visitors (R2, R3, R4, R7)`
3. `feat: add statistics visitor without modifying element hierarchy (R5, R8)`
4. `test: add comprehensive JUnit 5 test suite (T1-T8), demo runner, and project documentation`
