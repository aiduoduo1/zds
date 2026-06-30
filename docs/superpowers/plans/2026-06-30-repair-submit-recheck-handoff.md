# Repair Submit Recheck Handoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a next-step dialog after formal repair submission and pass the original repair QR code into recheck for automatic validation.

**Architecture:** Keep submission branching in `RepairActivity`; use a small pure rule helper to distinguish draft and submit success. Add one explicit QR-code launch contract to `RecheckActivity`, then reuse its existing input and `submitScanSn` path.

**Tech Stack:** Kotlin, Android ViewBinding, JiangCore dialogs, JUnit 4

---

### Task 1: Post-submit rules

**Files:**
- Create: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairSubmitSuccessRules.kt`
- Create: `app/src/test/java/com/jj/zds/feature/repair/ui/RepairSubmitSuccessRulesTest.kt`

- [ ] Add failing tests proving only `RepairAction.SUBMIT` shows the next-step prompt and QR handoff trims but preserves the original SN/PRO scan payload.
- [ ] Run the focused test and confirm it fails because `RepairSubmitSuccessRules` is missing.
- [ ] Implement `shouldPrompt(action)` and `handoffQrCode(scanInput)`.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Repair success dialog

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/repair/ui/RepairActivity.kt`

- [ ] Keep the existing draft success toast and page clear.
- [ ] On formal submit success, show a non-cancelable `JConfirmDialog` with “继续维修” and “去复检”.
- [ ] Make “继续维修” call the existing `clearRepairPage()`.
- [ ] Make “去复检” capture `binding.inputSn.text()`, clear the repair page, and call `RecheckActivity.start(this, qrCode)`.

### Task 3: Recheck launch contract

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/recheck/ui/RecheckActivity.kt`

- [ ] Add `start(context, qrCode)` using an explicit Intent extra.
- [ ] Read the extra before `bindScanInput()` so the existing input renders the original QR code.
- [ ] After adapters and listeners are ready, automatically call the existing `submitScanSn` when the launch QR code is nonblank.
- [ ] Keep ordinary no-extra entry, manual input, scanner input, debounce, and error handling unchanged.

### Task 4: Verification

**Files:**
- Verify scoped repair and recheck files.

- [ ] Run focused rule tests and all repair/recheck unit tests.
- [ ] Run `:app:assembleDebug` and `git diff --check`.
- [ ] Install the debug APK on the connected 480×800 PDA.
