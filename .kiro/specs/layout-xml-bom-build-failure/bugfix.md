# Bugfix Requirements Document

## Introduction

The Android Gradle debug build fails during resource merging because three layout XML files under `app/src/main/res/layout/` begin with a UTF-8 byte-order mark (the bytes `EF BB BF`) placed before the `<?xml ...?>` prolog. The Android data-binding `LayoutFileParser` does not tolerate a leading BOM, so it reads an empty token at line 1, column 0 and throws a misleading `NullPointerException` ("Cannot read field \"elmName\" because \"root\" is null").

Byte inspection confirmed the BOM is present in `activity_login.xml`, `activity_registro.xml`, and `activity_splash.xml`. The build only surfaced the first two errors because it aborted before reaching the third; `activity_splash.xml` would fail on a subsequent run. The XML content of each file is otherwise well-formed.

Fixing the bug means removing the UTF-8 BOM from these three files (rewriting them as UTF-8 without BOM) while leaving their XML content unchanged, so that `mergeDebugResources`, `packageDebugResources`, and `installDebug` complete successfully.

## Bug Analysis

### Current Behavior (Defect)

When a layout XML file begins with a UTF-8 BOM before the XML prolog, the data-binding resource merger fails to parse it and the build aborts.

1.1 WHEN a file under `app/src/main/res/layout/` begins with the UTF-8 BOM bytes `EF BB BF` before the `<?xml ...?>` prolog THEN the data-binding `LayoutFileParser` reads an empty token at line 1, column 0 and reports `line 1:0 mismatched input '' expecting {COMMENT, SEA_WS, '<', PI}`
1.2 WHEN `activity_login.xml` or `activity_registro.xml` contains the leading BOM THEN the resource and asset merger throws `NullPointerException` ("Cannot read field \"elmName\" because \"root\" is null") and the `:app:mergeDebugResources` / `:app:packageDebugResources` tasks fail
1.3 WHEN the debug build (`gradlew.bat installDebug`) is executed while any affected layout file contains the leading BOM THEN the build aborts before completing

### Expected Behavior (Correct)

The affected layout files should be encoded as UTF-8 without a BOM so the merger parses them and the build completes.

2.1 WHEN a file under `app/src/main/res/layout/` begins directly with the `<?xml ...?>` prolog with no leading BOM bytes THEN the data-binding `LayoutFileParser` SHALL parse the document without emitting an empty-token error
2.2 WHEN `activity_login.xml` and `activity_registro.xml` are encoded as UTF-8 without a BOM THEN the `:app:mergeDebugResources` and `:app:packageDebugResources` tasks SHALL complete without throwing a `NullPointerException`
2.3 WHEN the debug build (`gradlew.bat installDebug`) is executed after the BOM is removed from all affected layout files THEN the build SHALL complete successfully

### Unchanged Behavior (Regression Prevention)

Removing the BOM must not alter the XML content or affect files that never had a BOM.

3.1 WHEN a layout file's BOM is removed THEN the system SHALL CONTINUE TO preserve the file's existing XML content (elements, attributes, and structure) unchanged
3.2 WHEN a layout file did not contain a leading BOM THEN the system SHALL CONTINUE TO leave that file's bytes unchanged
3.3 WHEN the fixed layout files are parsed by the data-binding merger THEN the system SHALL CONTINUE TO produce the same views, IDs, and bindings the well-formed XML already defined
