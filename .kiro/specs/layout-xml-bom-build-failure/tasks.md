# Implementation Plan

- [ ] 1. Write bug condition exploration test (byte-level BOM detection)
  - **Property 1: Bug Condition** - BOM-Prefixed Layout Files Parse and Build
  - **CRITICAL**: This check MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the files or the build when it fails**
  - **NOTE**: This check encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists (leading UTF-8 BOM `EF BB BF`)
  - **Scoped PBT Approach**: This is a deterministic bug over a fixed, finite set of files, so scope the property to the concrete failing cases: the three affected files under `app/src/main/res/layout/`
  - Read the first 3 bytes of each affected file and assert they equal `EF BB BF` (bug condition: `isBugCondition(input)` where `input.bytes[0..2] == [0xEF,0xBB,0xBF]`):
    - `app/src/main/res/layout/activity_login.xml`
    - `app/src/main/res/layout/activity_registro.xml`
    - `app/src/main/res/layout/activity_splash.xml`
  - PowerShell example per file: `[System.IO.File]::ReadAllBytes("app/src/main/res/layout/activity_login.xml")[0..2]` → expect `239 187 191` (0xEF 0xBB 0xBF)
  - Run the Gradle build on UNFIXED sources to reproduce the parser failure: `.\gradlew.bat mergeDebugResources`
  - **EXPECTED OUTCOME**: BOM bytes are present AND the build FAILS with `line 1:0 mismatched input ''` followed by a `NullPointerException` (this is correct - it proves the bug exists)
  - Document counterexamples found (e.g., "activity_login.xml begins with EF BB BF; :app:mergeDebugResources fails with mismatched-input then NPE 'root is null'")
  - Mark task complete when the byte check confirms the BOM, the build failure is reproduced, and the failure is documented
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-BOM Content and Files Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Snapshot the exact bytes of every file under `app/src/main/res/layout/` on the UNFIXED sources (e.g. store a copy or record per-file byte arrays / lengths / hashes)
  - Observe on UNFIXED code that clean files begin with `3C 3F 78` (`<?x`, no BOM): confirm `activity_main.xml`, `fragment_inicio.xml`, and the remaining layout files (cases where `isBugCondition` returns false)
  - Write a property-based assertion over the whole layout directory: for every file, `postFixBytes == stripLeadingBOM(originalBytes)` — non-BOM files are byte-identical; BOM files lose exactly the 3 leading bytes and the body is unchanged
  - Assert idempotence of `stripLeadingBOM`: applying it twice yields the same bytes as applying it once
  - Run these checks on UNFIXED code to establish the baseline snapshot
  - **EXPECTED OUTCOME**: Clean files begin with `3C 3F 78` and the baseline snapshot is captured (this confirms behavior to preserve)
  - Mark task complete when the baseline is snapshotted and the clean-file / idempotence expectations are recorded on unfixed code
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 3. Fix for leading UTF-8 BOM in layout XML files

  - [ ] 3.1 Strip the leading BOM from the three affected files
    - For each of the three files, read the raw bytes and confirm they start with `EF BB BF` before acting (guard clause; skip files without the BOM to guarantee idempotence and to leave clean files untouched)
    - Rewrite each guarded file as UTF-8 **without** a BOM using the byte slice starting at offset 3 (`bytes[3..]`), leaving every subsequent byte identical (no re-indent, no re-serialize, no CRLF/LF conversion)
    - PowerShell example per file:
      `$b = [System.IO.File]::ReadAllBytes($p); if ($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF) { [System.IO.File]::WriteAllBytes($p, $b[3..($b.Length-1)]) }`
    - Apply to:
      - `app/src/main/res/layout/activity_login.xml`
      - `app/src/main/res/layout/activity_registro.xml`
      - `app/src/main/res/layout/activity_splash.xml`
    - Make no changes to `build.gradle`, the data-binding setup, or the Gradle version
    - _Bug_Condition: isBugCondition(input) where input.path is under app/src/main/res/layout/ AND input.bytes[0..2] == [0xEF,0xBB,0xBF]_
    - _Expected_Behavior: expectedBehavior(result) — file begins with 3C 3F 78 6D 6C ("<?xml"), no leading BOM, parser reads it and merge/package succeed_
    - _Preservation: Preservation Requirements from design — body byte-identical below stripped BOM; non-BOM files untouched; guarded edit is idempotent_
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3_

  - [ ] 3.2 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - BOM-Prefixed Layout Files Parse and Build
    - **IMPORTANT**: Re-run the SAME checks from task 1 - do NOT write new ones
    - Re-read the first bytes of each fixed file and assert they now equal `3C 3F 78 6D 6C` (`<?xml`) and that `bytes[0..2] != [0xEF,0xBB,0xBF]`
    - Assert each fixed file's byte length equals its original length minus exactly 3
    - Re-run `.\gradlew.bat mergeDebugResources packageDebugResources`
    - **EXPECTED OUTCOME**: Byte checks pass AND both tasks complete without `mismatched input ''` or `NullPointerException` (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 3.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-BOM Content and Files Unchanged
    - **IMPORTANT**: Re-run the SAME checks from task 2 - do NOT write new ones
    - Compare against the baseline snapshot: every non-BOM layout file is byte-identical; each fixed file equals its original bytes with exactly the leading three bytes removed (`postFixBytes == originalBytes[3..]`)
    - Confirm idempotence: re-applying the guarded strip is a no-op on the now-fixed and never-BOM files
    - **EXPECTED OUTCOME**: All preservation checks PASS (confirms no regressions and no unintended edits)
    - _Requirements: 3.1, 3.2, 3.3_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Run the full debug build end to end: `.\gradlew.bat assembleDebug` (or `.\gradlew.bat installDebug` if a device/emulator is connected)
  - Confirm `:app:mergeDebugResources`, `:app:packageDebugResources`, and the build complete successfully with no `mismatched input ''` error and no `NullPointerException`
  - Confirm the affected screens (login, registro, splash) inflate with the same views, IDs, and bindings the well-formed XML already defined
  - Ensure all byte-level and Gradle checks pass; ask the user if questions arise.
