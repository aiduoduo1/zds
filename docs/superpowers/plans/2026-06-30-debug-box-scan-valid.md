# Debug Box Scan Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Switch `DebugPlanCreateBatchActivity` box scanning from the QR analysis endpoint to `/m/iqcToBoxLabel/scanValid` while preserving duplicate, material, display, and bind behavior.

**Architecture:** Reuse the existing `ZdsApi -> ZdsRepository -> ZdsViewModel.scanValidDebugBox()` path. The Activity observes `debugBoxScanValidResult`, stores `DebugBoxScanValidDto`, and continues submitting the original scanned QR strings.

**Tech Stack:** Kotlin, Android ViewBinding, LiveData, Retrofit, JUnit 4

---

### Task 1: Align the scan-validation response contract

**Files:**
- Modify: `app/src/test/java/com/jj/zds/feature/debugplan/ui/DebugPlanBoxMaterialValidatorTest.kt`
- Modify: `app/src/main/java/com/jj/zds/data/dto/debugplan/DebugPlanOperationDtos.kt`

- [x] **Step 1: Write the failing test**

Change the scan-result fixtures to `DebugBoxScanValidDto` and assert that `qrCode` preserves the server-returned raw box QR string.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugPlanBoxMaterialValidatorTest"`

Expected: FAIL because `DebugBoxScanValidDto` does not yet expose `qrCode`.

- [x] **Step 3: Write minimal implementation**

Add `val qrCode: String? = null` to `DebugBoxScanValidDto`.

- [x] **Step 4: Run test to verify it passes**

Run the same targeted unit-test command and expect PASS.

### Task 2: Route the Activity through scanValid

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugPlanCreateBatchActivity.kt`

- [x] **Step 1: Replace the result type and observer**

Use `DebugBoxScanValidDto`, observe `debugBoxScanValidResult`, and pass the result to the existing `addBox` flow.

- [x] **Step 2: Replace the request call**

In `submitScan`, call `viewModel.scanValidDebugBox(taskId, code)` and update the verifying message to describe validation rather than QR parsing.

- [x] **Step 3: Preserve existing behavior**

Keep the raw scanned code in `PendingBox.code`, preserve material matching and duplicate-box blocking, and render `categoryName`, `lot`, `qty`, `startSn`, and `endSn` from the new DTO.

- [x] **Step 4: Verify compilation and focused tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugPlanBoxMaterialValidatorTest" :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.
