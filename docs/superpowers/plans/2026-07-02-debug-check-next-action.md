# Debug Check Next Action Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make check-item IME Next and hardware Enter silently jump to the first pending required check from the top, or submit when no required checks remain.

**Architecture:** Put first-pending selection in a pure shared rule and expose one next-action callback from the shared value binder. Each of the four pages keeps ownership of its existing checks, scrolling, text dialog, and submit method; it calls one local navigation function so scan-input Enter remains untouched.

**Tech Stack:** Kotlin, Android EditorInfo/KeyEvent, RecyclerView, JInputDialog, JUnit 4.

---

### Task 1: Add the shared first-pending rule

**Files:**
- Create: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskNextActionRules.kt`
- Create: `app/src/test/java/com/jj/zds/feature/debugplan/ui/DebugTaskNextActionRulesTest.kt`

- [ ] Write tests proving the rule returns the topmost required `PENDING` row, ignores skippable pending rows, and returns `-1` when all required rows are handled.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugTaskNextActionRulesTest"` and verify RED because the rule does not exist.
- [ ] Implement `firstPendingIndex(rows)` with `indexOfFirst { it.result == PENDING && !it.dto.isSkipable }`.
- [ ] Rerun the focused test and verify GREEN.

### Task 2: Emit next action from numeric inputs

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/debugplan/adapter/DebugTaskCheckBinders.kt`

- [ ] Add `onNext: () -> Unit` to `DebugTaskValueBinder`.
- [ ] Set `editValue.imeOptions = EditorInfo.IME_ACTION_NEXT`.
- [ ] Handle `IME_ACTION_NEXT` and hardware `KEYCODE_ENTER` on key-up, invoke `onNext`, and consume the event.
- [ ] Do not add listeners to the SN scan input.

### Task 3: Wire the four page owners

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskExecuteActivity.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/singledebug/ui/SingleDebugActivity.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/recheck/ui/RecheckActivity.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/production/ui/ProductionOverallTestActivity.kt`

- [ ] Pass a local `handleCheckNextAction` callback to `DebugTaskValueBinder`.
- [ ] In each page, find the first pending required index from the top; if found, smooth-scroll to its adapter position without Toast; otherwise call that page's existing submit method.
- [ ] In each text input dialog, make IME Next/hardware Enter apply the current text using the same code as the dialog confirm button, dismiss the dialog, then call `handleCheckNextAction`.
- [ ] Keep all scan-input and NG-dialog Enter behavior unchanged.

### Task 4: Verify

**Files:**
- Verify all files above.

- [ ] Run `./gradlew.bat :app:testDebugUnitTest` and expect zero failures.
- [ ] Run `./gradlew.bat :app:compileDebugKotlin` and expect success.
- [ ] Run `git diff --check` on scoped files and expect no whitespace errors.
- [ ] Search the four page constructors and confirm every `DebugTaskValueBinder` supplies the next callback.
