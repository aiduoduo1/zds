# Production SN Bind Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Submit raw key-part scans directly to the bind endpoint and support backend-driven bind, replacement, and unbind operations.

**Architecture:** Introduce a small tested request factory for the three operation payloads. Replace the activity's decode-and-optimistic-update flow with a response-driven state machine and refresh the server-backed progress and bind-info models after every successful mutation.

**Tech Stack:** Kotlin, Android ViewBinding, LiveData, JUnit 4, Gradle

---

### Task 1: Operation request contract

**Files:**
- Modify: `app/src/main/java/com/jj/zds/data/dto/productionplan/ProductionPlanDtos.kt`
- Create: `app/src/main/java/com/jj/zds/feature/production/ui/ProductionKeyPartBindRequests.kt`
- Create: `app/src/test/java/com/jj/zds/feature/production/ui/ProductionKeyPartBindRequestsTest.kt`

- [ ] Add failing tests for bind `(1, raw SN, null)`, unbind `(2, old SN, null)`, and replace `(3, new SN, old SN)` payloads.
- [ ] Run the focused test and confirm it fails because the request factory and `operateType` are missing.
- [ ] Make `changeMatSn` nullable, add `operateType`, and implement the three request factories.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Response-driven activity flow

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/production/ui/ProductionSnBindActivity.kt`
- Delete: `app/src/main/java/com/jj/zds/feature/production/ui/ProductionKeyPartMatcher.kt`
- Delete: `app/src/test/java/com/jj/zds/feature/production/ui/ProductionKeyPartMatcherTest.kt`

- [ ] Remove QR decode observation and calls from key-part scanning.
- [ ] Submit every scan first with `operateType=1` and the raw scan as `matSn`.
- [ ] If `boundMatSns` is non-empty, show `JSingleChoiceDialog`; submit the selected old SN with `operateType=3`.
- [ ] Make delete submit the displayed bound SN immediately with `operateType=2`.
- [ ] Remove local optimistic row mutation and reload bind progress plus SN bind info after successful mutations.
- [ ] Keep the current product active after all slots are full so another scan can trigger replacement.

### Task 3: Verification

**Files:**
- Verify only

- [ ] Run `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin --console=plain`.
- [ ] Run `git diff --check` and confirm `ProductionSnBindActivity` no longer references QR decoding.
