# Single Debug Check Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make single-debug check rendering and submit validation follow the same rules as debug-plan task execution.

**Architecture:** Keep the existing activities and APIs intact. Extend the existing shared rule objects with the small aggregate needed by both footers, then replace SingleDebugActivity's duplicated type and validation decisions with those shared rules.

**Tech Stack:** Kotlin, Android ViewBinding, JUnit 4, Gradle

---

### Task 1: Shared reportable failure count

**Files:**
- Modify: `app/src/test/java/com/jj/zds/feature/debugplan/ui/DebugTaskSubmitRulesTest.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskSubmitRules.kt`

- [ ] Add a test asserting that one failed text row plus one failed switch row produces a reportable failure count of one.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugTaskSubmitRulesTest"` and confirm compilation fails because `reportableFailureCount` does not exist.
- [ ] Add `reportableFailureCount(rows)` and implement `hasReportableFailure(rows)` in terms of it.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Synchronize SingleDebugActivity

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/singledebug/ui/SingleDebugActivity.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskExecuteActivity.kt`

- [ ] Use `DebugTaskSubmitRules.reportableFailureCount` for both footer NG counts.
- [ ] Use `DebugTaskSubmitRules.hasReportableFailure` before loading the NG dictionary in single debug.
- [ ] Use `DebugTaskInputRules.resolveType` for single-debug row creation; keep text initial input blank unless a backend result exists.
- [ ] Apply the shared 50-character input limit to the single-debug text dialog.
- [ ] Locate the first pending non-skippable row when submit validation fails.
- [ ] Remove SingleDebugActivity's duplicated collect-type helper functions.

### Task 3: Verification

**Files:**
- Verify only

- [ ] Run `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin --console=plain`.
- [ ] Run `git diff --check` and inspect the scoped diff for unrelated changes.
