# Layout XML BOM Build Failure Bugfix Design

## Overview

The Android Gradle debug build fails during resource merging because three layout XML files under `app/src/main/res/layout/` begin with a UTF-8 byte-order mark (the bytes `EF BB BF`) placed before the `<?xml ...?>` prolog. The data-binding `LayoutFileParser` does not tolerate a leading BOM: it reads an empty token at line 1, column 0 (`line 1:0 mismatched input '' expecting {COMMENT, SEA_WS, '<', PI}`) and then throws a misleading `NullPointerException` ("Cannot read field \"elmName\" because \"root\" is null"), aborting `:app:mergeDebugResources` / `:app:packageDebugResources`.

Byte inspection confirmed the BOM is present in `activity_login.xml`, `activity_registro.xml`, and `activity_splash.xml`, and confirmed that other layout files (e.g. `activity_main.xml`, `fragment_inicio.xml`) begin directly with `3C 3F 78` (`<?x`) and have no BOM.

The fix is minimal and targeted: rewrite each of the three affected files as UTF-8 **without** a BOM, leaving the XML content byte-identical apart from the stripped three leading bytes. Files that never had a BOM are left untouched. Success is verified by re-running the Gradle debug build and confirming the merge/package/install tasks complete.

## Glossary

- **Bug_Condition (C)**: A layout XML file under `app/src/main/res/layout/` whose byte stream begins with the UTF-8 BOM `EF BB BF` before the `<?xml ...?>` prolog.
- **Property (P)**: After the fix, such a file begins directly with the `<?xml ...?>` prolog (first bytes `3C 3F 78 6D 6C`), the data-binding parser reads it without an empty-token error, and the resource merge/package tasks succeed.
- **Preservation**: The XML content of every layout file (elements, attributes, structure, resulting views/IDs/bindings) is unchanged, and files that never had a BOM keep their exact bytes.
- **UTF-8 BOM**: The three-byte sequence `EF BB BF` that some editors prepend to UTF-8 files to mark encoding. It is optional in UTF-8 and, when present before an XML prolog, breaks the data-binding `LayoutFileParser`.
- **LayoutFileParser**: The Android Gradle data-binding component that parses layout XML during `mergeDebugResources`; it expects the document to start with a comment, whitespace, `<`, or a processing instruction, and fails on a leading BOM.
- **Affected files**: `activity_login.xml`, `activity_registro.xml`, `activity_splash.xml` (all under `app/src/main/res/layout/`).

## Bug Details

### Bug Condition

The bug manifests when a layout XML file's raw byte stream starts with the UTF-8 BOM (`EF BB BF`) before the `<?xml ...?>` prolog. The data-binding `LayoutFileParser` is either treating the BOM as an unexpected empty token at line 1:0, failing to build the parse tree root, or dereferencing that null root — producing the mismatched-input error followed by the `NullPointerException`.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type LayoutFile (path + raw bytes)
  OUTPUT: boolean

  RETURN input.path IS UNDER "app/src/main/res/layout/"
         AND input.bytes[0..2] == [0xEF, 0xBB, 0xBF]   // leading UTF-8 BOM
         AND input.bytesAfterBOM begins a well-formed "<?xml ...?>" document
END FUNCTION
```

### Examples

- `activity_login.xml` — first three bytes are `EF BB BF` (confirmed). Expected: parser reads the prolog; actual: `line 1:0 mismatched input ''` then `NullPointerException`, `:app:mergeDebugResources` fails.
- `activity_registro.xml` — first three bytes are `EF BB BF` (confirmed). Same failure; build aborts.
- `activity_splash.xml` — first three bytes are `EF BB BF` (confirmed). The build aborted on the first two files before reaching this one, so it would fail on a subsequent run once the earlier files are fixed.
- `activity_main.xml` — first three bytes are `3C 3F 78` (`<?x`, no BOM, confirmed). Edge case: parses correctly today and must remain unchanged.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- The XML content of each affected file (elements, attributes, ordering, structure) remains exactly as authored; only the three leading BOM bytes are removed.
- Layout files that never contained a BOM (e.g. `activity_main.xml`, `fragment_inicio.xml`, and all other files under `layout/`) keep their exact bytes.
- The views, IDs, and data-binding bindings produced from each fixed file are identical to what the well-formed XML already defined.

**Scope:**
All inputs that do NOT involve a leading UTF-8 BOM should be completely unaffected by this fix. This includes:
- Layout files that already start directly with `<?xml ...?>` (no BOM).
- Any non-layout resources and source files in the project.
- The XML body of the affected files below the stripped BOM (unchanged byte-for-byte).

**Note:** The expected correct behavior for the buggy inputs is defined in the Correctness Properties section (Property 1).

## Hypothesized Root Cause

Based on the bug description and byte inspection, the cause is a leading UTF-8 BOM introduced by an editor when the three files were saved:

1. **Leading BOM before the XML prolog**: The files were saved as "UTF-8 with BOM", prepending `EF BB BF` ahead of `<?xml ...?>`. Confirmed via byte inspection of all three files.
   - The `LayoutFileParser` grammar expects the stream to start with COMMENT, SEA_WS, `<`, or a PI; the BOM does not match, so it reports an empty token at line 1:0.

2. **Null parse-tree root leading to NPE**: Because the first token is rejected, the parser fails to construct the document root, and downstream code dereferences the null `root` (`Cannot read field "elmName" because "root" is null`), surfacing as a `NullPointerException` during resource merging.

3. **Build abort ordering**: The merge fails fast on the first offending file, which is why only two of the three errors were reported; `activity_splash.xml` shares the same defect and would fail once the earlier files are fixed.

The fix does not require changing any build configuration or parser behavior — only removing the BOM from the source files.

## Correctness Properties

Property 1: Bug Condition - BOM-Prefixed Layout Files Parse and Build

_For any_ layout file under `app/src/main/res/layout/` where the bug condition holds (isBugCondition returns true — the file begins with the UTF-8 BOM `EF BB BF`), the fixed file SHALL begin directly with the `<?xml ...?>` prolog (first bytes `3C 3F 78 6D 6C`) with no leading BOM, so the data-binding `LayoutFileParser` parses it without emitting the `line 1:0 mismatched input ''` error and `:app:mergeDebugResources` / `:app:packageDebugResources` complete without a `NullPointerException`.

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - Non-BOM Content and Files Unchanged

_For any_ input where the bug condition does NOT hold (isBugCondition returns false — a layout file with no leading BOM, or the XML body below a stripped BOM), the fixed project SHALL produce exactly the same bytes and the same parsed result (views, IDs, bindings) as the original, preserving all existing XML content and leaving BOM-free files byte-identical.

**Validates: Requirements 3.1, 3.2, 3.3**

## Fix Implementation

### Changes Required

Assuming the root cause analysis is correct (confirmed by byte inspection):

**Files**:
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/layout/activity_registro.xml`
- `app/src/main/res/layout/activity_splash.xml`

**Operation**: For each affected file, remove the three leading BOM bytes (`EF BB BF`) and rewrite the file as UTF-8 without a BOM, leaving every subsequent byte unchanged.

**Specific Changes**:
1. **Detect the BOM per file**: Read the raw bytes and confirm the file starts with `EF BB BF` before acting. Only files that actually carry the BOM are modified (guards against re-running the fix and against touching clean files).
   - Confirmed present: `activity_login.xml`, `activity_registro.xml`, `activity_splash.xml`.
   - Confirmed absent (do not touch): `activity_main.xml`, `fragment_inicio.xml`, and the remaining layout files.

2. **Strip only the BOM**: Write back the byte slice starting at offset 3 (`bytes[3..]`) so the content begins with `<?xml ...?>`. Do not reformat, re-indent, or re-serialize the XML; the body must remain byte-identical.

3. **Preserve line endings and encoding of the body**: Save as UTF-8 with no BOM. Do not convert CRLF/LF or normalize whitespace inside the document.

4. **Idempotence**: Because the fix is guarded by the BOM check, applying it to an already-fixed or never-affected file is a no-op, satisfying preservation requirement 3.2.

5. **No configuration changes**: No changes to `build.gradle`, the data-binding setup, or the Gradle version are required or made.

## Testing Strategy

### Validation Approach

The strategy is two-phase: first surface counterexamples that demonstrate the bug on the unfixed files (byte-level BOM detection and a failing build), then verify the fix parses/builds correctly and that all other files and XML content are preserved. Because the artifacts here are source files consumed by the Gradle build, "tests" are a mix of byte-level assertions and Gradle task outcomes.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix, confirming the root cause (leading BOM). If the byte inspection had shown no BOM, the root cause would be refuted and we would re-hypothesize.

**Test Plan**: Inspect the first three bytes of each layout file and run the Gradle debug build on the unfixed sources to observe the failure and its message.

**Test Cases**:
1. **Login BOM detection**: Read first 3 bytes of `activity_login.xml`; expect `EF BB BF` (confirmed) — demonstrates the bug on unfixed code.
2. **Registro BOM detection**: Read first 3 bytes of `activity_registro.xml`; expect `EF BB BF` (confirmed).
3. **Splash BOM detection**: Read first 3 bytes of `activity_splash.xml`; expect `EF BB BF` (confirmed).
4. **Build failure reproduction**: Run `gradlew.bat mergeDebugResources`; expect `line 1:0 mismatched input ''` and a `NullPointerException`, with the task failing (will fail on unfixed code).
5. **Clean-file control (edge case)**: Read first 3 bytes of `activity_main.xml`; expect `3C 3F 78` (no BOM, confirmed) — shows the defect is specific to the three files.

**Expected Counterexamples**:
- The three affected files begin with `EF BB BF`.
- `:app:mergeDebugResources` fails with the mismatched-input error followed by the null-root `NullPointerException`.
- Cause: leading UTF-8 BOM before the XML prolog.

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed file no longer starts with a BOM and the build parses/merges it successfully.

**Pseudocode:**
```
FOR ALL file WHERE isBugCondition(file) DO
  stripLeadingBOM(file)                       // write bytes[3..] as UTF-8 no BOM
  ASSERT file.bytes[0..4] == [0x3C,0x3F,0x78,0x6D,0x6C]   // "<?xml"
  ASSERT file.bytes[0..2] != [0xEF,0xBB,0xBF]
END FOR

result := run "gradlew.bat mergeDebugResources packageDebugResources"
ASSERT result.succeeded
ASSERT result.output DOES NOT CONTAIN "mismatched input ''"
ASSERT result.output DOES NOT CONTAIN "NullPointerException"
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed project produces the same bytes and the same parsed result as the original. Also verify that for the fixed files, everything below the stripped BOM is byte-identical to the original body.

**Pseudocode:**
```
FOR ALL file WHERE NOT isBugCondition(file) DO
  ASSERT bytesAfter(file) == bytesBefore(file)          // never-BOM files untouched
END FOR

FOR ALL file WHERE isBugCondition(file) DO
  ASSERT bytesAfter(file) == bytesBefore(file)[3..]     // only 3 BOM bytes removed
END FOR
```

**Testing Approach**: Byte-level equality is the strongest preservation guarantee here and is inexpensive to check exhaustively across the layout directory, so it is preferred over sampling. A property-style check (compare original-minus-BOM against the fixed bytes for every layout file) covers both the affected and unaffected files uniformly.

**Test Plan**: Snapshot the original bytes of every layout file before the fix. After the fix, compare: unaffected files must be identical; affected files must equal their original bytes with exactly the leading three bytes removed.

**Test Cases**:
1. **Affected-body preservation**: For each of the three files, assert fixed bytes equal original bytes minus the first three, and that the resulting first bytes spell `<?xml`.
2. **Clean-file preservation**: Assert `activity_main.xml`, `fragment_inicio.xml`, and all other non-BOM layout files are byte-identical before and after.
3. **Binding-output preservation**: Build succeeds and the generated views/IDs/bindings for the fixed layouts match what the well-formed XML defines (verified indirectly via a successful `assembleDebug` / `installDebug` and the app inflating these layouts).

### Unit Tests

- Assert the first three bytes of each affected file are `EF BB BF` before the fix and `3C 3F 78` after the fix.
- Assert each never-BOM layout file's first bytes are `3C 3F 78` and unchanged after the fix.
- Assert the byte length of each fixed file equals the original length minus 3.

### Property-Based Tests

- For every file under `app/src/main/res/layout/`, assert the post-fix bytes equal `stripLeadingBOM(originalBytes)` — a single property that simultaneously enforces the fix (BOM files lose exactly the BOM) and preservation (non-BOM files are unchanged).
- Assert idempotence: applying `stripLeadingBOM` twice yields the same bytes as applying it once.

### Integration Tests

- Run `gradlew.bat mergeDebugResources packageDebugResources` and confirm both tasks complete without the mismatched-input error or `NullPointerException`.
- Run `gradlew.bat installDebug` (or `assembleDebug`) and confirm the debug build completes successfully end to end.
- Launch/inflate the affected screens (login, registro, splash) to confirm the layouts render with the same views and bindings as intended.
