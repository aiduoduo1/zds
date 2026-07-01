# Production Execution Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the existing production execution prototype so its key-part binding flow demonstrates the current Android bind, replace, unbind, refresh, validation, and reset behavior.

**Architecture:** Keep the existing single-file HTML/CSS/JS prototype and its `activePlan` mock state. Replace only the key-part binding renderer and handlers, add a project-style replacement dialog, and preserve the existing production overview and unrelated stations.

**Tech Stack:** Static HTML, CSS, vanilla JavaScript, existing in-file mock state.

---

### Task 1: Align binding state and request simulation

**Files:**
- Modify: `C:/Users/29634/Documents/Codex/zdsproject/pda/生产计划/index.html`

- [ ] **Step 1: Add binding request state and refresh helpers**

Add helpers that retain `pendingMatSn`, `boundMatSns`, and `busy`, treat `code === 200` as success, and simulate refreshing `/bindProgress` plus `/snBindInfo` after successful mutation.

- [ ] **Step 2: Replace the scan mutation contract**

Normal key-part scans call the local request simulator with:

```js
{ matSn: raw, changeMatSn: null, operateType: 1 }
```

Do not call or simulate QR decoding.

- [ ] **Step 3: Preserve error input for rescan**

On failure, set the page error message, re-render, restore the scanned value, focus the input, and select its contents.

### Task 2: Build the two-level binding UI

**Files:**
- Modify: `C:/Users/29634/Documents/Codex/zdsproject/pda/生产计划/index.html`

- [ ] **Step 1: Update product-scan and binding states**

Waiting state shows only the product-scan instruction and empty two-level material list. Loaded state shows product identity, plan binding progress, and raw key-part scan instruction.

- [ ] **Step 2: Render material parent and SN child rows**

Each parent row shows material code, name, required quantity, and bound count. Each child row shows the bound SN and an unbind button.

- [ ] **Step 3: Remove obsolete bottom actions**

Remove `data-clear-binding` and `data-next-binding` buttons. Right-top reset becomes the only operation that returns to product scanning.

- [ ] **Step 4: Rewrite the embedded development notes**

Document `/snBindInfo`, `/bind`, `/bindProgress`, `operateType` values 1/2/3, `boundMatSns`, validation feedback, refresh ordering, and reset boundaries.

### Task 3: Implement replace, unbind, and reset interactions

**Files:**
- Modify: `C:/Users/29634/Documents/Codex/zdsproject/pda/生产计划/index.html`

- [ ] **Step 1: Add the replacement single-choice dialog**

When the bind response contains candidates, render radio choices from `boundMatSns`. Confirm remains disabled until one candidate is selected.

- [ ] **Step 2: Submit replacement**

Confirm calls:

```js
{ matSn: pendingMatSn, changeMatSn: selectedOldSn, operateType: 3 }
```

Then close the dialog and refresh both binding endpoints.

- [ ] **Step 3: Submit unbind**

After confirmation, the child delete button calls:

```js
{ matSn: boundSn, changeMatSn: null, operateType: 2 }
```

Then refresh both binding endpoints.

- [ ] **Step 4: Wire right-top reset and Enter scanning**

For active binding work, `workMenuButton` resets only local product context. Enter on the scan input triggers the same handler as the scan button.

### Task 4: Verify the static prototype

**Files:**
- Verify: `C:/Users/29634/Documents/Codex/zdsproject/pda/生产计划/index.html`

- [ ] **Step 1: Run static contract checks**

Run targeted searches confirming the three endpoint names, `operateType` 1/2/3, replacement dialog, reset handler, raw scan request, and absence of obsolete bottom actions.

- [ ] **Step 2: Run markup/script syntax check**

Extract the inline JavaScript to a temporary parse-only check or use the bundled runtime to validate syntax without modifying the prototype.

- [ ] **Step 3: Manual scenario checklist**

Open the file directly and verify: product scan success/error, normal bind, same-type candidate dialog, replacement, child delete/unbind, Enter scan, and right-top reset.
