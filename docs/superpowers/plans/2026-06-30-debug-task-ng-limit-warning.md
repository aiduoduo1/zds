# Batch Debug NG Limit Warning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After a formal batch-debug SN submission, warn once per page session when the latest plan NG count is strictly above its rejection limit, and save the operator's selected action as a non-blocking audit log.

**Architecture:** Keep threshold and log-text rules in a small pure Kotlin unit so they can be developed test-first. Reuse the existing batch-info refresh to discover the plan ID, then load the authoritative plan detail and evaluate the threshold only for a completed formal submission. Add the OpenAPI log request through the existing API/repository stack, with a dedicated ViewModel result so log failures do not interfere with scan and submission state.

**Tech Stack:** Kotlin 2.0.21, Android LiveData/ViewModel, Retrofit, JiangResult/JiangApiCaller, JUnit 4, existing `JConfirmDialog`.

---

### Task 1: Add pure threshold and audit-log rules

**Files:**
- Create: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskNgLimitRules.kt`
- Create: `app/src/test/java/com/jj/zds/feature/debugplan/ui/DebugTaskNgLimitRulesTest.kt`

- [ ] **Step 1: Write failing threshold and log mapping tests**

Create tests asserting that equality does not warn, strict overflow warns, an acknowledged session does not warn, and the two actions produce the approved `opName`/`opDesc` values using the plan count and limit.

```kotlin
package com.jj.zds.feature.debugplan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugTaskNgLimitRulesTest {
    @Test fun `equal count and limit does not warn`() {
        assertFalse(DebugTaskNgLimitRules.shouldWarn(2, 2, sessionAcknowledged = false))
    }

    @Test fun `count above limit warns before session acknowledgement`() {
        assertTrue(DebugTaskNgLimitRules.shouldWarn(3, 2, sessionAcknowledged = false))
    }

    @Test fun `acknowledged session does not warn again`() {
        assertFalse(DebugTaskNgLimitRules.shouldWarn(3, 2, sessionAcknowledged = true))
    }

    @Test fun `contact supervisor log describes selection without claiming contact completed`() {
        val log = DebugTaskNgLimitRules.buildLog(DebugTaskNgLimitAction.CONTACT_SUPERVISOR, 3, 2)
        assertEquals("批次不良超限-联系主管", log.opName)
        assertEquals("当前批次不良数为 3，已超过计划上限 2；操作员已知晓并选择联系主管，继续批次调试。", log.opDesc)
    }

    @Test fun `continue work log records explicit continuation`() {
        val log = DebugTaskNgLimitRules.buildLog(DebugTaskNgLimitAction.CONTINUE_WORK, 3, 2)
        assertEquals("批次不良超限-继续工作", log.opName)
        assertEquals("当前批次不良数为 3，已超过计划上限 2；操作员已知晓并选择继续工作。", log.opDesc)
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugTaskNgLimitRulesTest"`

Expected: compilation failure because `DebugTaskNgLimitRules` and related types do not exist.

- [ ] **Step 3: Implement the minimal pure rules**

```kotlin
package com.jj.zds.feature.debugplan.ui

enum class DebugTaskNgLimitAction { CONTACT_SUPERVISOR, CONTINUE_WORK }

data class DebugTaskNgLimitLog(val opName: String, val opDesc: String)

object DebugTaskNgLimitRules {
    fun shouldWarn(ngCount: Int, ngLimit: Int, sessionAcknowledged: Boolean): Boolean {
        return !sessionAcknowledged && ngCount > ngLimit
    }

    fun buildLog(action: DebugTaskNgLimitAction, ngCount: Int, ngLimit: Int): DebugTaskNgLimitLog {
        return when (action) {
            DebugTaskNgLimitAction.CONTACT_SUPERVISOR -> DebugTaskNgLimitLog(
                opName = "批次不良超限-联系主管",
                opDesc = "当前批次不良数为 $ngCount，已超过计划上限 $ngLimit；操作员已知晓并选择联系主管，继续批次调试。",
            )
            DebugTaskNgLimitAction.CONTINUE_WORK -> DebugTaskNgLimitLog(
                opName = "批次不良超限-继续工作",
                opDesc = "当前批次不良数为 $ngCount，已超过计划上限 $ngLimit；操作员已知晓并选择继续工作。",
            )
        }
    }
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugTaskNgLimitRulesTest"`

Expected: all five tests pass.

### Task 2: Add the exact log request contract and network path

**Files:**
- Modify: `app/src/main/java/com/jj/zds/data/dto/debugplan/DebugPlanOperationDtos.kt`
- Create: `app/src/test/java/com/jj/zds/data/dto/debugplan/DebugTaskLogRequestTest.kt`
- Modify: `app/src/main/java/com/jj/zds/data/api/ZdsApi.kt`
- Modify: `app/src/main/java/com/jj/zds/data/repository/ZdsRepository.kt`

- [ ] **Step 1: Write a failing request serialization test**

```kotlin
package com.jj.zds.data.dto.debugplan

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugTaskLogRequestTest {
    @Test fun `serializes exact log field names`() {
        val json = Gson().toJson(
            DebugTaskLogRequest(
                taskId = "plan-1",
                taskItemId = "batch-1",
                opName = "批次不良超限-继续工作",
                opDesc = "desc",
            ),
        )
        val map = Gson().fromJson(json, Map::class.java)
        assertEquals(setOf("taskId", "taskItemId", "opName", "opDesc"), map.keys)
    }
}
```

- [ ] **Step 2: Run the serialization test and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.data.dto.debugplan.DebugTaskLogRequestTest"`

Expected: compilation failure because `DebugTaskLogRequest` does not exist.

- [ ] **Step 3: Add the request DTO**

```kotlin
data class DebugTaskLogRequest(
    val taskId: String,
    val taskItemId: String,
    val opName: String,
    val opDesc: String,
)
```

- [ ] **Step 4: Run the serialization test and verify GREEN**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.data.dto.debugplan.DebugTaskLogRequestTest"`

Expected: the serialization test passes.

- [ ] **Step 5: Add API and repository methods**

Add to `ZdsApi`:

```kotlin
@POST("/m/iqcTaskOpLog/addLog")
suspend fun saveDebugTaskLog(
    @Body request: DebugTaskLogRequest,
): ZdsBaseResult<Any>
```

Add to `ZdsRepository`:

```kotlin
suspend fun saveDebugTaskLog(request: DebugTaskLogRequest): JiangResult<Unit> {
    return when (val result = JiangApiCaller.safeApiCall { api.saveDebugTaskLog(request) }) {
        is JiangResult.Success -> result.data.toUnitResult()
        is JiangResult.Error -> result
        JiangResult.Loading -> JiangResult.Loading
        JiangResult.Empty -> JiangResult.Empty
    }
}
```

- [ ] **Step 6: Compile the app network path**

Run: `./gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL` with the exact DTO import used by API and repository.

### Task 3: Expose a dedicated non-blocking log result

**Files:**
- Modify: `app/src/main/java/com/jj/zds/viewmodel/ZdsViewModel.kt`

- [ ] **Step 1: Add a dedicated result model and LiveData**

Add a result carrying success and error text so the activity never has to infer a log failure from shared UI state:

```kotlin
data class DebugTaskLogSaveResult(
    val request: DebugTaskLogRequest,
    val success: Boolean,
    val message: String = "",
)

private val _debugTaskLogSaveResult = MutableLiveData<DebugTaskLogSaveResult>()
val debugTaskLogSaveResult: LiveData<DebugTaskLogSaveResult> = _debugTaskLogSaveResult
```

- [ ] **Step 2: Add a ViewModel method that maps every JiangResult branch to the dedicated result**

```kotlin
fun saveDebugTaskLog(request: DebugTaskLogRequest) {
    viewModelScope.launch {
        when (val result = repository.saveDebugTaskLog(request)) {
            is JiangResult.Success -> _debugTaskLogSaveResult.value = DebugTaskLogSaveResult(request, true)
            is JiangResult.Error -> _debugTaskLogSaveResult.value = DebugTaskLogSaveResult(
                request = request,
                success = false,
                message = result.exception.message.orEmpty(),
            )
            JiangResult.Empty -> _debugTaskLogSaveResult.value = DebugTaskLogSaveResult(
                request = request,
                success = false,
                message = "日志保存结果为空",
            )
            JiangResult.Loading -> Unit
        }
    }
}
```

- [ ] **Step 3: Compile the ViewModel path**

Run: `./gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`; logging does not publish to `debugOperationDone` or shared `uiState`.

### Task 4: Connect post-submit checking, one-session prompt, and one retry

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskExecuteActivity.kt`

- [ ] **Step 1: Add page-session state**

```kotlin
private var pendingNgLimitCheck = false
private var ngLimitPromptHandled = false
private var currentPlanId = ""
private var pendingLogRequest: DebugTaskLogRequest? = null
private var logRetryAttempted = false
```

- [ ] **Step 2: Observe authoritative plan detail and dedicated log results**

When `debugTaskItemInfo` arrives and `pendingNgLimitCheck` is true, capture its `taskId` and load plan detail. Observe `debugPlanDetail` only to evaluate a pending post-submit check. Observe `debugTaskLogSaveResult` to clear a successful request or retain the first failure for one later retry.

```kotlin
viewModel.debugPlanDetail.observe(this) { detail ->
    if (!pendingNgLimitCheck) return@observe
    pendingNgLimitCheck = false
    if (DebugTaskNgLimitRules.shouldWarn(detail.ngCount, detail.rejectionQty, ngLimitPromptHandled)) {
        showNgLimitDialog(detail.ngCount, detail.rejectionQty)
    }
}

viewModel.debugTaskLogSaveResult.observe(this) { result ->
    if (result.request != pendingLogRequest) return@observe
    if (result.success) {
        pendingLogRequest = null
    } else {
        JToast.show(result.message.ifBlank { "调试日志保存失败，稍后自动重试" })
    }
}
```

- [ ] **Step 3: Mark only formal submit completion for a threshold check**

Update `handleSnSubmitDone()` so it sets `pendingNgLimitCheck = true`, retries a retained log at most once, then follows the existing reset/refresh/focus flow. Do not change `handleDraftSaved()`.

```kotlin
private fun handleSnSubmitDone() {
    pendingNgLimitCheck = true
    retryPendingLogOnce()
    resetSnForm(clearInput = true)
    refreshTaskInfo()
    focusScanInput(selectAll = false)
    JToast.show("SN调试已提交")
}
```

- [ ] **Step 4: Add the two-action dialog and exact log request creation**

Use `JConfirmDialog` with cancel text `联系主管` and confirm text `继续工作`. Both callbacks call one handler that first marks the session handled, builds the approved log, creates `DebugTaskLogRequest(currentPlanId, taskItemId, ...)`, stores it as pending, and sends it.

- [ ] **Step 5: Add one background retry without reopening the dialog**

```kotlin
private fun retryPendingLogOnce() {
    val request = pendingLogRequest ?: return
    if (logRetryAttempted) return
    logRetryAttempted = true
    viewModel.saveDebugTaskLog(request)
}
```

Reset `logRetryAttempted` to false when a new user choice creates a new pending request. Never reset `ngLimitPromptHandled` inside `resetSnForm`; it resets only when the activity instance ends.

- [ ] **Step 6: Run focused tests and compile**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.jj.zds.feature.debugplan.ui.DebugTaskNgLimitRulesTest" --tests "com.jj.zds.data.dto.debugplan.DebugTaskLogRequestTest"`

Expected: all new tests pass.

Run: `./gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

### Task 5: Final regression verification

**Files:**
- Verify all modified files above.

- [ ] **Step 1: Run the feature-area unit suite**

Run: `./gradlew.bat :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL` and zero failing tests.

- [ ] **Step 2: Inspect the scoped diff**

Run: `git diff --check`

Expected: no whitespace errors.

Run: `git diff -- app/src/main/java/com/jj/zds/data/dto/debugplan/DebugPlanOperationDtos.kt app/src/main/java/com/jj/zds/data/api/ZdsApi.kt app/src/main/java/com/jj/zds/data/repository/ZdsRepository.kt app/src/main/java/com/jj/zds/viewmodel/ZdsViewModel.kt app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskNgLimitRules.kt app/src/main/java/com/jj/zds/feature/debugplan/ui/DebugTaskExecuteActivity.kt app/src/test/java/com/jj/zds/data/dto/debugplan/DebugTaskLogRequestTest.kt app/src/test/java/com/jj/zds/feature/debugplan/ui/DebugTaskNgLimitRulesTest.kt`

Expected: only the approved threshold warning, audit logging, retry, and tests are present; existing unrelated working-tree changes remain untouched.

