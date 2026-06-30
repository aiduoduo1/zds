# Repair PDA Information Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the repair first screen into a compact scan confirmation and repair-entry page while preserving the existing scan, upload, draft, and submit behavior.

**Architecture:** Keep both confirmation and repair form states inside `RepairActivity`, so the existing mutable form state and ViewModel calls stay in place. Add a read-only detail Activity, redesign the existing history Activity as a timeline, and use a small pure parser to separate an optional status suffix from repair log text.

**Tech Stack:** Kotlin, Android ViewBinding, XML layouts, JiangCore UI components, JUnit 4

---

### Task 1: Timeline text parser

**Files:**
- Create: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairTimelineDisplay.kt`
- Create: `app/src/test/java/com/jj/zds/feature/repair/ui/RepairTimelineDisplayTest.kt`

- [ ] Write tests proving `调试NG上报不良：电流值异常 状态：待维修` becomes content `调试NG上报不良：电流值异常` and status `待维修`, while text without a status suffix remains unchanged.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests "com.jj.zds.feature.repair.ui.RepairTimelineDisplayTest" --console=plain` and confirm the test fails because the parser is missing.
- [ ] Implement `RepairTimelineDisplay.parse(opDesc)` using the last `状态：` marker and blank-safe fallbacks.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Compact confirmation and in-Activity repair form

**Files:**
- Modify: `app/src/main/res/layout/activity_repair.xml`
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairActivity.kt`

- [ ] Replace the old report-detail card with a compact confirmation card containing `tvSn`, `tagStatus`, `tvNgPhenomena`, `tvNgRemark`, `btnToggleRemark`, and `btnStartRepair`.
- [ ] Move the existing repair fields and buttons into `layoutRepairForm`, hidden until `btnStartRepair` is clicked.
- [ ] Add `layoutSummaryPage` and render helpers that switch visibility without clearing `currentState`, field selections, text inputs, or `repairPhotos`.
- [ ] Make the remark render at three lines by default and toggle between three lines and unlimited lines.
- [ ] Override toolbar and system back handling so form mode returns to confirmation mode before the Activity finishes.
- [ ] Keep existing calls to `scanValidRepairSn`, `uploadRepairPhoto`, and `submitRepair` unchanged.

### Task 3: Toolbar actions and photo gallery

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairActivity.kt`

- [ ] Keep the framework toolbar menu as the single visible `刷新` action.
- [ ] Add a second local toolbar action displaying `⋮`, anchored to an Android `PopupMenu` with detail, history, photos, and clear actions.
- [ ] Gate detail, history, and photo actions on a successful scan and show `请先扫描 SN` when unavailable.
- [ ] Reuse the existing photo loading and full-screen preview code to show a compact photo gallery; keep photo upload and deletion inside repair form mode.
- [ ] Make clear/reset return to confirmation mode and focus the scan input.

### Task 4: Read-only detail page

**Files:**
- Create: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairDetailActivity.kt`
- Create: `app/src/main/res/layout/activity_repair_detail.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Add grouped white cards for identification, material, defect, and report metadata.
- [ ] Pass the current `RepairSnScanValidDto` fields through explicit Intent extras; do not add a network request or change the DTO.
- [ ] Map `categoryName` and `productName` into the existing material/test-material display slots without renaming backend fields.
- [ ] Add three-line expand/collapse behavior for the formatted defect remark.
- [ ] Register the Activity as non-exported.

### Task 5: Timeline history page

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairHistoryActivity.kt`
- Modify: `app/src/main/res/layout/activity_repair_history.xml`
- Create: `app/src/main/res/layout/item_repair_timeline.xml`

- [ ] Replace the report and repair text blobs with a compact SN/current-status header and a vertical timeline container.
- [ ] Inflate one small card per `RepairLogDto` showing operation name, time, operator, parsed content, and an optional status Tag.
- [ ] Use warning styling for `待维修`, success styling for completed/OK states, info styling for in-progress states, and neutral styling otherwise.
- [ ] Render a light empty state when no logs are returned.
- [ ] Remove the photo section from history because photos now have their own menu entry.

### Task 6: Verification

**Files:**
- Verify all scoped repair files only.

- [ ] Run the focused parser and existing repair UI unit tests.
- [ ] Run `./gradlew :app:compileDebugKotlin --console=plain`.
- [ ] Run `git diff --check` and inspect only repair, manifest, and plan diffs.
- [ ] Confirm no ViewModel, Repository, API, DTO field, Gradle, dependency, or package changes were introduced.
