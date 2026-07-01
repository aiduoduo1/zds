# 生产执行整体测试页面设计

## 目标

在生产执行进度页点击“整体测试”后进入全新的整体测试页面。页面视觉和检查项交互与单个调试一致，但业务状态、接口调用和后续提交能力完全独立，不修改 `SingleDebugActivity` 的现有流程。

## 页面入口

- `ProductionExecuteProgressActivity` 中 `operationType == 2` 的条目打开 `ProductionOverallTestActivity`。
- 页面入口必须传入 `planId`；缺失时提示并关闭页面。
- 新页面注册到 `AndroidManifest.xml`，不对外导出。

## 接口与模型

### 获取整体测试进度

- 方法：`POST /pro/proPlaDtl/testProgress`
- 请求：`planId`
- 响应数据：`testCount`、`bindCount`、`planCount`、`checkModuleGroupId`
- 页面进入后请求一次，工具栏副标题显示：`测试总进度 testCount/bindCount/planCount`。
- 计数字段缺失时按 `0` 展示。

### 获取成品 SN 调试表

- 方法：`POST /pro/proPlaDtl/snTestInfo`
- 请求：`planId`、`proSn`
- 响应数据：`proSn`、`currentProcess`、`currentProcessLabel`、`checkGroupDto`
- `checkGroupDto` 使用现有 `DebugCheckGroupDto`，从而复用现有检查项展示和编辑组件。

接口声明放入 `ZdsApi`，请求/响应 DTO 放入 productionplan 数据目录，调用封装放入 `ZdsRepository`，页面状态通过 `ZdsViewModel` 暴露。

## 页面与交互

- 新建 `ProductionOverallTestActivity`，不继承或模式化 `SingleDebugActivity`。
- 页面使用独立布局文件，但结构与单个调试一致：扫描输入框、检查表头、动态检查项和底部操作区。
- 支持扫码枪输入、手动输入、防重复提交、扫码状态反馈和错误后重新聚焦。
- 扫码成功后展示解析后的 SN、当前工序及调试表检查项。
- 检查项复用现有开关、数值、文本和拍照 Binder，以及现有输入校验和 NG 统计规则。
- 照片上传沿用现有检查项照片上传接口和预览逻辑。
- 清空扫描框时清理当前 SN 和检查项状态。

## 保存与提交占位

- 底部保留“保存”和“提交”按钮，视觉与单个调试一致。
- 当已加载 SN 时按钮可点击。
- 在整体测试提交接口提供前，点击任一按钮只提示“整体测试提交接口暂未开放”，不构造或发送提交请求，也不清空当前填写内容。

## 错误处理

- 进度接口失败：使用页面现有错误处理展示提示，扫码功能仍可使用。
- SN 信息接口失败：扫描框显示失败状态，保留输入并重新聚焦，允许再次扫描。
- 返回空调试表：展示提示并保留当前 SN 信息，不产生崩溃。
- 照片上传失败：恢复对应检查项上传状态，允许重试。

## 测试范围

- DTO 能保留两个接口所需的请求字段和关键响应字段。
- 进度副标题按 `testCount/bindCount/planCount` 格式生成，并处理缺失计数。
- SN 响应能映射现有 `DebugCheckGroupDto`。
- 生产执行页只有整体测试条目进入新页面，扫码绑定入口不受影响。
- 保存、提交按钮在接口缺失期间不会调用现有单个调试提交接口。

## 非目标

- 不实现整体测试保存或提交网络接口。
- 不重构 `SingleDebugActivity` 或抽取公共 Activity 基类。
- 不修改单个调试、复检、扫码绑定等既有业务行为。
