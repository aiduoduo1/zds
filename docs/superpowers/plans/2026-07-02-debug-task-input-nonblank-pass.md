# Debug Text Nonblank Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make shared `collectType=input` validation pass for any nonblank operator input without comparing it to the reference text.

**Architecture:** Keep the behavior inside the existing pure `DebugTaskInputRules` helper so batch debug, single debug, recheck, and overall test pages continue to use one result rule. Change only its test and predicate; no page, layout, DTO, or submit-flow changes are needed.

**Tech Stack:** Kotlin, JUnit 4, existing Android/JiangCore project.

---

### Task 1: Change input matching to nonblank validation

**Files:**
- Modify: `app/src/test/java/com/jj/zds/feature/debugplan/ui/DebugTaskInputRulesTest.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskInputRules.kt`

- [ ] **Step 1: Write the failing behavior test**

Replace the old exact-match assertions with:

```kotlin
@Test
fun `input passes for any nonblank content without matching reference`() {
    assertEquals(50, DebugTaskInputRules.MAX_INPUT_LENGTH)
    assertTrue(DebugTaskInputRules.isInputMatched("任意调试说明", "标准参考值"))
    assertFalse(DebugTaskInputRules.isInputMatched("", "标准参考值"))
    assertFalse(DebugTaskInputRules.isInputMatched("   ", "标准参考值"))
}
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugTaskInputRulesTest"`

Expected: the nonmatching nonblank assertion fails because the old implementation compares input and reference.

- [ ] **Step 3: Implement the minimal rule**

```kotlin
fun isInputMatched(value: String, reference: String): Boolean {
    return value.isNotBlank()
}
```

Keep the `reference` parameter for the existing call sites; it remains available for display but is no longer part of validation.

- [ ] **Step 4: Verify GREEN and regressions**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugTaskInputRulesTest"`

Expected: the focused tests pass.

Run: `./gradlew.bat :app:testDebugUnitTest`

Expected: the full debug unit suite passes.

Run: `./gradlew.bat :app:compileDebugKotlin`

Expected: Kotlin compilation succeeds without changing page or submit code.
