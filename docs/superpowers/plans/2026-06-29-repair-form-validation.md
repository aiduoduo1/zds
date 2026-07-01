# Repair Form Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update repair form validation, input limits, hint styling, and repair-result label display without changing submitted backend values.

**Architecture:** Add a small pure display mapper for dictionary value-to-label conversion and test it first. Keep submit validation in `RepairActivity` and declarative input constraints in `activity_repair.xml`.

**Tech Stack:** Kotlin, Android ViewBinding, XML resources, JUnit 4, Gradle

---

### Task 1: Repair result display mapping

**Files:**
- Create: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairResultDisplay.kt`
- Create: `app/src/test/java/com/jj/zds/feature/repair/ui/RepairResultDisplayTest.kt`

- [ ] Add failing tests proving a matching dictionary value returns its label and an unmatched value falls back to the original value.
- [ ] Run the focused test and confirm it fails because `RepairResultDisplay` does not exist.
- [ ] Implement `RepairResultDisplay.labelFor(value, items)` with trimmed exact value matching and label/value fallback.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Repair form behavior

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairActivity.kt`
- Modify: `app/src/main/res/layout/activity_repair.xml`

- [ ] Keep `selectedRepairResult` as the backend value and use `RepairResultDisplay` whenever `tvRepairResult` is rendered.
- [ ] Refresh repair-result display when dictionary items arrive after a scanned record.
- [ ] Keep draft validation partial; keep all four business fields required only for submit.
- [ ] Set repair person `maxLength` to 10 and repair description `maxLength` to 1000.
- [ ] Set repair-description hint text color to the Android theme default hint color.

### Task 3: Repair remark line display

**Files:**
- Create: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairRemarkDisplay.kt`
- Create: `app/src/test/java/com/jj/zds/feature/repair/ui/RepairRemarkDisplayTest.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairActivity.kt`

- [ ] Add a failing test proving Chinese and English comma-separated text is trimmed and joined with line breaks.
- [ ] Run the focused test and confirm it fails because `RepairRemarkDisplay` does not exist.
- [ ] Implement `RepairRemarkDisplay.format` and use it when rendering `tvNgRemark`.
- [ ] Run the focused test and confirm it passes.

### Task 4: Verification

**Files:**
- Verify only

- [ ] Run `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin --console=plain`.
- [ ] Run `git diff --check` and inspect the scoped diff.
