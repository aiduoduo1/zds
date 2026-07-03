# Repair Submit Recheck Handoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route formal repair submission by the exact result label so only “已修好，允许复调” automatically enters recheck, while other final states require confirmation before submission.

**Architecture:** Resolve the selected backend value to its exact dictionary label in a pure rule helper. Keep request construction in `RepairActivity`; gate non-recheck results before calling the existing submit method, and reuse the existing QR-code launch contract for automatic recheck after successful repair submission.

**Tech Stack:** Kotlin, Android ViewBinding, JiangCore dialogs, JUnit 4

---

### Task 1: Exact result-label rules

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairSubmitSuccessRules.kt`
- Modify: `app/src/test/java/com/jj/zds/feature/repair/ui/RepairSubmitSuccessRulesTest.kt`

- [ ] Add failing tests proving only an exact dictionary label `已修好，允许复调` permits automatic recheck; value matches and fuzzy labels must not pass.
- [ ] Run the focused test and confirm the old action-only rule fails the new assertions.
- [ ] Implement `isRecheckAllowed(selectedValue, items)` with exact trimmed value lookup and exact trimmed label comparison; keep QR trimming.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Pre-submit final-state confirmation

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairActivity.kt`

- [ ] Keep the existing draft success toast and page clear.
- [ ] Extract the validated request dispatch so formal submission can be called only after branching.
- [ ] If the selected label is exactly `已修好，允许复调`, submit immediately without a confirmation dialog.
- [ ] For every other label or unresolved dictionary value, show “该状态为最终状态，确认后无法继续使用，是否确认？” before the request; confirm dispatches once, cancel leaves all form state intact.
- [ ] Remove the old post-success “继续维修 / 去复检” dialog.

### Task 3: Post-success destination

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairActivity.kt`
- Verify: `app/src/main/java/com/jj/zds/feature/recheck/ui/RecheckActivity.kt`

- [ ] Capture whether the in-flight submission permits recheck and preserve the original scan QR code until success.
- [ ] On successful allowed submission, clear the repair page and call the existing `RecheckActivity.start(context, qrCode)` directly.
- [ ] On successful non-recheck submission, show success, clear the page, and remain in repair.
- [ ] Keep the existing recheck input prefill and automatic validation behavior unchanged.

### Task 4: Verification

**Files:**
- Verify scoped repair and recheck files.

- [ ] Run focused rule tests and all repair/recheck unit tests.
- [ ] Run `:app:assembleDebug` and `git diff --check`.
- [ ] Install the debug APK on the connected 480×800 PDA.
