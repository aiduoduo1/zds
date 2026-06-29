# 生产关键部件绑定操作设计

## 目标

`ProductionSnBindActivity` 直接将关键部件原始扫码值提交给 `/pro/proPlaDtl/bind`，不再调用二维码解析接口，并支持绑定、解绑和换绑三种操作。

## 请求规则

- 扫描关键部件首次请求：`matSn=原始扫码值`、`changeMatSn=null`、`operateType=1`。
- 若首次请求返回的 `boundMatSns` 为空，则视为绑定成功。
- 若 `boundMatSns` 非空，使用项目现有单选 Dialog 展示可替换 SN；用户选择后再次请求：`matSn=原始扫码值`、`changeMatSn=选中旧SN`、`operateType=3`。
- 点击已绑定 SN 的删除按钮时立即请求：`matSn=该已绑定SN`、`changeMatSn=null`、`operateType=2`。

## 页面状态

- 不在前端解析关键件类型，也不乐观修改关键件列表。
- 绑定、换绑、解绑成功后均调用 `/pro/proPlaDtl/bindProgress` 和 `/pro/proPlaDtl/snBindInfo` 刷新进度与当前产品绑定信息。
- 成功提示根据操作类型显示绑定成功、换绑成功或解绑成功。
- 请求失败时保留当前服务器列表，恢复扫码框内容与焦点。
- 换绑候选 Dialog 取消时不发起第二次请求，保留扫码值便于再次操作。

## 范围

修改绑定请求 DTO 和 `ProductionSnBindActivity`，保留现有 API、Repository、ViewModel 调用链，不改变成品码第一步扫描流程。

## 验证

- 单元测试覆盖三种操作请求参数以及候选列表是否进入换绑选择。
- 执行 app Debug 单元测试和 Kotlin 编译。
