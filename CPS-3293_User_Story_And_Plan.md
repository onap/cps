# CPS-3293: Refetch YANG Models — User Story & Implementation Plan

> Session handoff document. Captures the agreed design, constraints, acceptance
> criteria, concrete change set, and an incremental (small-commit) implementation
> plan so work can be picked up in a new session.

## Summary
Add a new NCMP inventory endpoint that lets an operator force NCMP to re-read YANG
module content from the nodes and update it in place when it has changed. This
handles the case where a DMI plugin regenerates models but keeps the same module
set tag and YANG revision (persisted module sets are immutable and revisions are
not bumped), so a normal module sync would never detect the change.

## How it works
- New endpoint `POST /ncmpInventory/v1/ch/module/refresh`. The request body is
  identical to the existing CM Handle search (reuses the CPS-Path / DMI-plugin /
  property query criteria), so all discussed selection options are supported.
- The response is **synchronous** and returns the matched CM Handles **grouped by
  module set tag** (alternate id preferred), regardless of whether any change is
  later found. Only **one sample node per module set tag** is actually refreshed
  (see below); the response shows the **state of each node** in the group so the
  caller can see which one has entered the refresh/sync state.
- Only **one sample node per module set tag needs to be refreshed** (performance):
  all nodes serving a tag are assumed to hold identical content and YANG resources
  are stored once and shared, so refreshing a single sample corrects the content for
  every CM Handle referencing that tag. The sample chosen is the **first READY** CM
  Handle for the tag.
- The actual refresh is **asynchronous**. The chosen sample CM Handle is set to
  LOCKED with a new `MODULE_REFRESH` lock reason. The existing module-sync watchdog
  resets it to ADVISED (retaining the lock reason) and processes it — so the work is
  naturally distributed across instances, exactly like the current initial
  sync/upgrade flow.
- During processing, NCMP force-reads the module references and content from the
  node, compares each module's content (by checksum) against what is stored, and
  where it differs, updates the stored YANG resource content **in place**. Because
  YANG resources are stored once and shared, updating the single row fixes the
  content for every schema set / module set tag referencing that module, and all
  schema-set links are preserved (same row id).
- On success the CM Handle returns to READY; on failure it is marked
  `MODULE_REFRESH_FAILED` and retried by the watchdog (same as upgrade failures).

## Key design decisions & constraints
- **Refresh only one sample node per module set tag (performance).** All nodes
  serving a tag are assumed to hold identical content and YANG resources are shared,
  so refreshing a single sample corrects every CM Handle referencing that tag. The
  sample is the **first READY** CM Handle for the tag. The synchronous response still
  lists all matched nodes with their state so the operator can see which sample was
  selected.
- **Reuse the existing async module-sync watchdog and multi-instance mechanism** —
  refresh is just another lock reason, distinguished from initial sync/upgrade.
- **No new Hazelcast structures** — only reuse the existing distributed work queue.
- **Do not delete existing YANG models.** Detect changes by force-reading +
  comparing, then update content in place only when a difference is found. If
  nothing changed, nothing is touched.
- **Simplicity over efficiency.** A module shared across several module set tags may
  be examined more than once; this is acceptable (few tags are expected to be
  affected) and updates are idempotent.
- **Assumption:** all nodes serving a given module already hold the correct/identical
  content.
- Empty search result → still returns 200 with empty arrays.
- **Logging:** when a module content change is detected, log it clearly including the
  node (CM Handle / alternate id) and the module set tag (plus module name/revision).

## Acceptance Criteria (Demo)

**AC1 — Search criteria via the synchronous response**
- Given registered CM Handles across more than one module set tag.
- When `POST /ncmpInventory/v1/ch/module/refresh` is called with search criteria
  (e.g. a CPS-Path or DMI-plugin condition).
- Then the call returns **200 synchronously** with the matched CM Handles **grouped
  by module set tag** (alternate id where available).
- The response lists every matched node with its state and identifies the single
  **sample node (first READY)** selected for refresh per module set tag.
- Demo a broad and a narrow criterion to show the grouping changes with the query,
  and show an empty criterion match returns 200 with empty arrays.

**AC2 — Model content before and after the refresh**
- Capture the current YANG resource content as the "before" baseline via
  `GET /ncmp/v1/ch/{cm-handle}/modules/definitions?module-name=<name>&revision=<rev>`.
- Configure the stubbed DMI to return **changed YANG content for the same module
  name and revision** (revision intentionally unchanged — proves the fixed-revision
  case).
- When `POST /ncmpInventory/v1/ch/module/refresh` is called with criteria matching
  the target CM Handle(s) (returns 200 with handles grouped by module set tag;
  refresh processed asynchronously by the watchdog).
- Then calling the module definitions endpoint again after processing shows the
  content matches the **new** stubbed content, while name and revision are unchanged
  — demonstrating a non-destructive in-place content update.

**AC3 — Logging of detected change**
- When a module content change is detected during the refresh.
- Then a log entry identifies **the node (CM Handle / alternate id)** and **the
  module set tag** (plus module name/revision).
- Demo the log line for the changed module in AC2, and show that an unchanged module
  produces no such "change detected" log.

**AC4 — Non-destructive / shared-module behaviour (optional to demo)**
- Show a module shared by multiple module set tags is corrected everywhere from the
  single in-place update, and CM Handles return to READY on success (or
  `MODULE_REFRESH_FAILED` and retried on failure).

## Incremental commit plan
Each step must build and pass tests on its own and must not break existing behaviour.
Steps 3–5 add code that is wired but dormant (nothing sets `MODULE_REFRESH` yet);
step 6 is the isolated "switch on".

**Commit 1 — Endpoint + synchronous response only (no async, no side effects).**
- OpenAPI: add `refreshModules` operation in `ncmp-inventory.yml`; wire
  `/v1/ch/module/refresh` in `openapi-inventory.yml`; add grouped-by-tag response
  schema in `components.yaml`.
- Facade `refreshModules(...)`: run the existing southbound search, group matched
  handles by module set tag, return the response. **No state changes, no async.**
- `NetworkCmProxyInventoryController` endpoint + a response mapper.
- Spock tests.
- Result: endpoint returns the grouping and does nothing else yet.

**Commit 2 — Sample-node selection + per-node state in the response (still synchronous, no side effects).**
- For each module set tag, choose the **first READY** CM Handle as the single sample
  to be refreshed (performance: one node per tag is enough).
- Enrich the response so every matched node in a group carries its **current CM
  Handle state**, and the selected sample is identifiable. This is the array of node
  states the operator inspects; once refresh is switched on (commit 6) the sample is
  the one that will show the refresh/sync state.
- Response schema change: each entry under a module set tag becomes
  `{ reference, state }` (was a plain reference string); mapper + tests updated.
- Read-only: reads composite state only, still no locking and no async.

**Commit 3 — Lock reason categories.**
- Add `MODULE_REFRESH` and `MODULE_REFRESH_FAILED` to `LockReasonCategory` (additive
  enum change). Verify no exhaustive switch needs updating.

**Commit 4 — Persistence: in-place YANG content update.**
- New method on `CpsModuleService` / `CpsModulePersistenceService` (+ impl) /
  `YangResourceRepository` to update content + checksum for an existing
  `moduleName`+`revision`, keeping the same row id (preserves schema-set FKs).
- New method, not yet called. Fully unit-testable. Non-breaking.

**Commit 5 — Processing side (wired but not triggered).**
- `ModuleSyncService.refreshModuleContent(handle)`: force-read references + content
  from DMI for the handle's own module set tag, compare each module's content by
  checksum, update changed content in place (via commit 4), log detected changes
  (node + module set tag + module name/revision).
- `ModuleOperationsUtils`: add refresh detection helper and extend the locked-handles
  CPS-path query to include `MODULE_REFRESH` / `MODULE_REFRESH_FAILED`.
- `ModuleSyncTasks.processCmHandle`: add refresh branch (READY on success,
  `MODULE_REFRESH_FAILED` on failure).
- Dormant because nothing sets `MODULE_REFRESH` yet. Testable directly.

**Commit 6 — Activation (small, isolated).**
- Facade: after the search, set **only the selected sample per module set tag** (the
  first READY handle from commit 2) to LOCKED with `MODULE_REFRESH`.
- Now end-to-end works; the watchdog picks the sample handles up and refreshes them,
  and the sample shows the refresh/sync state in the response.

**Commit 7 (follow-up) — Cross-instance yangSchema cache invalidation.**
- Problem: `refreshModuleContent` updates YANG resource content in place and evicts
  the `yangSchema` cache (parsed `YangTextSchemaSourceSet`) — but that cache is a
  local per-instance Caffeine cache (`type: caffeine`, `expireAfterAccess=10m`). So
  only the instance that processed the refresh gets a fresh parsed model; other
  instances keep a stale parsed schema for that schema set (a hot entry can stay
  stale well beyond the 10m idle TTL). The in-place DB update and the
  `modules/definitions` read path are already correct (DB-direct); this is purely
  about the parsed-schema cache used for data validation.
- Proposed approach: broadcast a cache-eviction signal across instances so each
  instance evicts its local `yangSchema` entry for the affected schema set. Preferred
  option is a Hazelcast `ITopic` (publish/subscribe): on a content refresh, publish
  the `(dataspace, schemaSetName)` to evict; every instance subscribes and calls
  `YangTextSchemaSourceSetCache.removeFromCache`. This is a new, self-contained
  concern (cache coherence) — it does not touch the module-sync distribution
  mechanism. Alternatives considered: convert `yangSchema` to a distributed cache
  (heavier, changes hot-path behaviour), or a per-schema-set content version checked
  before cache use (more invasive).
- Scope: applies to any in-place content update, so it also hardens the same-tag
  upgrade path. Keep it as a separate commit since it is broader than the refresh
  feature and involves a (scoped) Hazelcast addition.

## Validated technical notes (for whoever implements)
- `CompositeStateUtils.setCompositeStateForRetry` retains the lock reason (category +
  details) on the LOCKED→ADVISED transition — so the refresh intent survives to
  `ModuleSyncTasks.processCmHandle`, exactly like `MODULE_UPGRADE`.
- A READY handle cannot be moved straight to ADVISED
  (`LcmEventsCmHandleStateHandlerImpl.updateCmHandleState` only advises new or
  LOCKED→retry handles). Refresh must therefore set handles to LOCKED first, then let
  the watchdog reset them (mirrors the upgrade flow).
- Watchdog reset path: `ModuleSyncWatchdog.setPreviouslyLockedCmHandlesToAdvised` →
  `ModuleOperationsUtils.getCmHandlesThatFailedModelSyncOrUpgrade()` uses the CPS-path
  constant `CPS_PATH_CM_HANDLES_MODEL_SYNC_FAILED_OR_UPGRADE` (extend it for refresh).
- DMI reads: `DmiModelOperations.getModuleReferences(handle, moduleSetTag)` returns
  name+revision list; `DmiModelOperations.getNewYangResourcesFromDmi(handle,
  moduleSetTag, references)` fetches resource content (pass all references to force a
  full content fetch for compare).
- Storage: `YangResourceEntity` has `moduleName`, `revision`, `checksum`, `content`;
  unique constraint is on `checksum` (`yang_resource_checksum_key`). Look up by
  `YangResourceRepository.findByModuleNameAndRevision`. Fixed-revision content change
  ⇒ same name+revision but different checksum ⇒ `identifyNewModuleReferences`
  (name+revision only) will NOT detect it ⇒ must compare content/checksum explicitly.
- Existing content-view endpoint for the demo:
  `GET /ncmp/v1/ch/{cm-handle}/modules/definitions?module-name=&revision=`
  (`getModuleDefinitions`).

## Key files
- REST/OpenAPI: `cps-ncmp-rest/docs/openapi/{ncmp-inventory.yml, openapi-inventory.yml, components.yaml}`
- Controller: `cps-ncmp-rest/.../rest/controller/NetworkCmProxyInventoryController.java`
- Mapper: `cps-ncmp-rest/.../rest/util/` (e.g. `NcmpRestInputMapper`, new response mapper)
- Facade: `cps-ncmp-service/.../api/inventory/NetworkCmProxyInventoryFacade.java` +
  `cps-ncmp-service/.../impl/NetworkCmProxyInventoryFacadeImpl.java`
- Sync: `cps-ncmp-service/.../impl/inventory/sync/{ModuleSyncService, ModuleSyncTasks, ModuleOperationsUtils, ModuleSyncWatchdog, DmiModelOperations}.java`
- Lock reason: `cps-ncmp-service/.../api/inventory/models/LockReasonCategory.java`
- Persistence: `cps-service/.../api/CpsModuleService.java`,
  `cps-service/.../spi/CpsModulePersistenceService.java`,
  `cps-service/.../impl/CpsModuleServiceImpl.java`,
  `cps-ri/.../CpsModulePersistenceServiceImpl.java`,
  `cps-ri/.../repository/YangResourceRepository.java`

## Open items / to confirm with team
- Exact response schema field names (proposed: list of `{ moduleSetTag, cmHandles[] }`,
  alternate id preferred).
- Whether to emit an LCM event when content is refreshed, or logging only (currently
  logging only per the story).
- `yangSchema` cache eviction after in-place content refresh is currently local to the
  processing instance only; cross-instance invalidation is planned as Commit 7 (see
  incremental commit plan above).
- Commit/Jira: include Issue-ID `CPS-3293` in every commit footer.
