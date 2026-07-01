# Production Overall Test Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a standalone production overall-test page opened from the production execution list, backed by the two provided APIs and reusing existing check-item UI components without changing single-debug behavior.

**Architecture:** Add production-specific request/response DTOs, Retrofit methods, repository conversions, and ViewModel LiveData. Build `ProductionOverallTestActivity` as an independent screen with its own layout while reusing existing check-row models, binders, validation rules, and photo upload facility. Keep save and submit as local placeholders until their backend endpoint exists.

**Tech Stack:** Kotlin 2.0.21, Android ViewBinding, Retrofit, LiveData, JiangCore VM/activity/recycler components, JUnit 4.

---

### Task 1: Overall-test API models

**Files:**
- Create: `app/src/main/java/com/jj/zds/data/dto/productionplan/ProductionOverallTestDtos.kt`
- Create: `app/src/test/java/com/jj/zds/data/dto/productionplan/ProductionOverallTestDtosTest.kt`

- [ ] **Step 1: Write the failing DTO test**

Create tests that instantiate `ProductionOverallTestProgressRequest(planId)`, `ProductionOverallTestSnInfoRequest(planId, proSn)`, `ProductionOverallTestProgressDto`, and `ProductionOverallTestSnInfoDto`, asserting all wire field names and the nested `DebugCheckGroupDto` are retained. Add a subtitle assertion against `ProductionOverallTestProgressDto.subtitle()` expecting `测试总进度 2/8/10`.

- [ ] **Step 2: Run the test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests "com.jj.zds.data.dto.productionplan.ProductionOverallTestDtosTest"`

Expected: Kotlin compilation fails because the production overall-test types do not exist.

- [ ] **Step 3: Add minimal DTOs and subtitle formatter**

```kotlin
data class ProductionOverallTestProgressRequest(val planId: String)

data class ProductionOverallTestSnInfoRequest(
    val planId: String,
    val proSn: String,
)

data class ProductionOverallTestProgressDto(
    val testCount: Int? = null,
    val bindCount: Int? = null,
    val planCount: Int? = null,
    val checkModuleGroupId: String? = null,
) {
    fun subtitle(): String =
        "测试总进度 ${testCount ?: 0}/${bindCount ?: 0}/${planCount ?: 0}"
}

data class ProductionOverallTestSnInfoDto(
    val proSn: String? = null,
    val currentProcess: Int? = null,
    val currentProcessLabel: String? = null,
    val checkGroupDto: DebugCheckGroupDto? = null,
)
```

- [ ] **Step 4: Run the focused DTO test and verify GREEN**

Run the command from Step 2. Expected: all `ProductionOverallTestDtosTest` tests pass.

- [ ] **Step 5: Commit the DTO slice**

```powershell
git add app/src/main/java/com/jj/zds/data/dto/productionplan/ProductionOverallTestDtos.kt app/src/test/java/com/jj/zds/data/dto/productionplan/ProductionOverallTestDtosTest.kt
git commit -m "feat: add production overall test models"
```

### Task 2: API, repository, and ViewModel data flow

**Files:**
- Modify: `app/src/main/java/com/jj/zds/data/api/ZdsApi.kt`
- Modify: `app/src/main/java/com/jj/zds/data/repository/ZdsRepository.kt`
- Modify: `app/src/main/java/com/jj/zds/viewmodel/ZdsViewModel.kt`

- [ ] **Step 1: Add the two Retrofit declarations**

```kotlin
@POST("/pro/proPlaDtl/testProgress")
suspend fun getProductionOverallTestProgress(
    @Body request: ProductionOverallTestProgressRequest,
): ZdsBaseResult<ProductionOverallTestProgressDto>

@POST("/pro/proPlaDtl/snTestInfo")
suspend fun getProductionOverallTestSnInfo(
    @Body request: ProductionOverallTestSnInfoRequest,
): ZdsBaseResult<ProductionOverallTestSnInfoDto>
```

- [ ] **Step 2: Add repository wrappers using the established result conversion**

Add `getProductionOverallTestProgress(planId)` and `getProductionOverallTestSnInfo(planId, proSn)`. Each must trim inputs, create its request DTO, call `JiangApiCaller.safeApiCall`, and convert the `ZdsBaseResult` with `toJiangResult`; use explicit empty messages `Overall test progress is empty.` and `Overall test SN information is empty.`.

- [ ] **Step 3: Add independent ViewModel state**

Add:

```kotlin
private val _productionOverallTestProgress = MutableLiveData<ProductionOverallTestProgressDto>()
val productionOverallTestProgress: LiveData<ProductionOverallTestProgressDto> =
    _productionOverallTestProgress

private val _productionOverallTestSnInfo = MutableLiveData<ProductionOverallTestSnInfoDto>()
val productionOverallTestSnInfo: LiveData<ProductionOverallTestSnInfoDto> =
    _productionOverallTestSnInfo
```

Add `loadProductionOverallTestProgress(planId)` with page loading and `loadProductionOverallTestSnInfo(planId, proSn)` with no loading overlay. Reject blank values with the existing toast mechanism before calling the repository.

- [ ] **Step 4: Compile both variants**

Run: `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin`

Expected: both Kotlin compilation tasks succeed.

- [ ] **Step 5: Commit the network slice**

```powershell
git add app/src/main/java/com/jj/zds/data/api/ZdsApi.kt app/src/main/java/com/jj/zds/data/repository/ZdsRepository.kt app/src/main/java/com/jj/zds/viewmodel/ZdsViewModel.kt
git commit -m "feat: connect production overall test APIs"
```

### Task 3: Standalone page shell and production entry

**Files:**
- Create: `app/src/main/res/layout/activity_production_overall_test.xml`
- Create: `app/src/main/java/com/jj/zds/feature/production/ui/ProductionOverallTestActivity.kt`
- Modify: `app/src/main/java/com/jj/zds/feature/production/ui/ProductionExecuteProgressActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create the standalone layout**

Use a vertical `JStyle.Page` root containing `JInputLayout` with ID `inputSn`, scan mode, clear enabled, title `整体测试`, and hint `扫描或输入产品码`; add a weighted `RecyclerView` with ID `recyclerChecks`. Do not reuse `ActivitySingleDebugBinding`.

- [ ] **Step 2: Create the Activity shell**

Create `ProductionOverallTestActivity : JiangToolbarStateVmActivity<ActivityProductionOverallTestBinding, ZdsViewModel>`. Add a required `EXTRA_PLAN_ID`, a `start(context, planId)` function, toolbar title `整体测试`, initial subtitle `测试总进度 0/0/0`, and `planId` validation in `initData()`.

- [ ] **Step 3: Connect progress observation**

Observe `productionOverallTestProgress` and call the inherited protected toolbar directly:

```kotlin
viewModel.productionOverallTestProgress.observe(this) { progress ->
    toolbar.setSubtitleText(progress.subtitle())
}
```

- [ ] **Step 4: Wire only operation type 2 to the new page**

Update the execution-list click handler to retain operation type 1 binding behavior and add:

```kotlin
OPERATION_TEST -> ProductionOverallTestActivity.start(this, planId)
```

- [ ] **Step 5: Register the Activity**

Add a non-exported manifest entry with `windowSoftInputMode="adjustResize|stateHidden"`.

- [ ] **Step 6: Compile resources and Kotlin**

Run: `./gradlew :app:assembleDebug`

Expected: resource linking and debug assembly succeed.

- [ ] **Step 7: Commit the page shell**

```powershell
git add app/src/main/res/layout/activity_production_overall_test.xml app/src/main/java/com/jj/zds/feature/production/ui/ProductionOverallTestActivity.kt app/src/main/java/com/jj/zds/feature/production/ui/ProductionExecuteProgressActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add production overall test page"
```

### Task 4: Scan and check-form interactions

**Files:**
- Modify: `app/src/main/java/com/jj/zds/feature/production/ui/ProductionOverallTestActivity.kt`

- [ ] **Step 1: Add independent page state and adapters**

Add `qrCode`, duplicate-scan timing fields, `scanInProgress`, `currentSnInfo`, `currentCheckGroup`, photo state, and a mutable check-row list. Configure independent header/footer `JBindingAdapter`s and the existing `JMultiBindingAdapter<DebugTaskCheckRow>` with `DebugTaskSwitchBinder`, `DebugTaskValueBinder`, `DebugTaskTextBinder`, and `DebugTaskPhotoBinder`.

- [ ] **Step 2: Bind scan input**

Use `onTextChanged`, `onInputDone`, and `onTextDebounce(260L)`. `submitScanSn` must trim the product code, prevent concurrent scans, and suppress the same code inside an 800 ms window before calling `viewModel.loadProductionOverallTestSnInfo(planId, code)`.

- [ ] **Step 3: Render the SN response**

Observe `productionOverallTestSnInfo`, clear the scanning flag, store the response and `checkGroupDto`, convert the group to existing `DebugTaskCheckRow` items, refresh the header/check/footer adapters, and display `proSn`, `currentProcessLabel`, group name, and version. Empty groups show a toast but retain SN context.

- [ ] **Step 4: Port check editing without submission coupling**

Implement the same row update behavior used by single debug: switch pass/fail, numeric range evaluation, text matching, remarks, NG phenomenon selection, photo capture/upload/preview/delete, keyboard inset handling, and focused-row scrolling. Reuse existing public row models and rules; do not call `submitSingleDebugCheck`.

- [ ] **Step 5: Keep save and submit as safe placeholders**

Enable both buttons only after SN info loads. Both click handlers must call:

```kotlin
JToast.show("整体测试提交接口暂未开放")
```

They must not mutate check rows, clear the scan input, or invoke any repository/ViewModel submit method.

- [ ] **Step 6: Handle scan and upload failures**

Override `onUiError` to clear scan/upload busy flags, restore the corresponding UI state, show scan failures through the input component, and return focus to the scan input. Clearing the input must reset SN, group, and rows.

- [ ] **Step 7: Assemble debug and manually inspect the page path**

Run: `./gradlew :app:assembleDebug`

Expected: debug APK assembles. Manually confirm production execution → overall test opens the standalone screen and binding still opens its existing screen.

- [ ] **Step 8: Commit the interaction slice**

```powershell
git add app/src/main/java/com/jj/zds/feature/production/ui/ProductionOverallTestActivity.kt
git commit -m "feat: implement overall test scan form"
```

### Task 5: Regression verification

**Files:**
- Verify only; no planned production changes.

- [ ] **Step 1: Run focused unit tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.jj.zds.data.dto.productionplan.*" --tests "com.jj.zds.feature.debugplan.ui.*"`

Expected: all selected tests pass.

- [ ] **Step 2: Run the project build entry point**

Run: `./gradlew asbzds`

Expected: debug and release APK assembly and copy tasks succeed.

- [ ] **Step 3: Review the final diff and scope**

Run: `git diff --check` and `git status --short`.

Confirm there are no dependency/version changes, no edits to `SingleDebugActivity`, and no generated APKs staged.

- [ ] **Step 4: Confirm the verification leaves no accidental files**

Check `git status --short`. Build outputs, APK files, local properties, and signing files must remain untracked or ignored; remove none of the user's unrelated files.
