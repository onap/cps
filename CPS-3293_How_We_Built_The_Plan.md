# CPS-3293: How We Built the Plan (Collaboration Retrospective)

A short, cleaned-up account of how the implementation plan for *Refetch YANG
Models* went from reading the spec to a small, reviewable commit sequence. It
highlights the assistant's analysis at each step and — more importantly — the
steering decisions from the team that shaped the final solution.

The takeaway: the AI did the code archaeology and drafting; the human corrected
direction on architecture, scope, and constraints at each iteration. Neither would
have reached this plan alone.

---

## Step 0 — Read the spec, examine the codebase
**Analysis:** Read `CPS-3293_Refetch_Yang_Models.md` and explored the NCMP
inventory layers (OpenAPI specs, `NetworkCmProxyInventoryController`, the facade,
the module-sync watchdog, `ModuleSyncService`, persistence). Confirmed the existing
CM Handle **search** interface already supports the DMI-plugin / CPS-Path / property
criteria the spec wanted, and that module sync is driven by CM Handle state.

**First proposal:** Reuse the search interface, then **force a re-fetch by deleting
the shared schema set** for each affected module set tag so the watchdog rebuilds it.

## Step 1 — Steer: don't delete, detect and compare
**Human correction:** "I don't want to delete existing YANG models. Instead I want a
flag to force a re-read from the node and then compare if there are any changes."

**Impact:** Dropped the destructive approach entirely. Reframed the feature as a
non-destructive **force-read + compare + update-in-place** flow.

**Analysis update:** Verified the real problem — YANG resources are keyed by content
**checksum** but looked up by name+revision; a regenerated model keeps the same
revision, so the standard delta (name+revision only) can never detect it. Confirmed a
content/checksum comparison is required, and that content can be updated in place on
the same row so schema-set links stay intact.

## Step 2 — Steer: follow the signed-off design (async + grouped response + check each module once)
**Human correction:** "Follow the design decision: async and a new URI endpoint. The
synchronous response is the list of CM Handles grouped per module set tag,
irrespective of whether a difference is found. Each unique module must be checked
only ONCE — it can appear in multiple nodes from different module set tags, but assume
all nodes already have the correct content."

**Impact:** Locked in the endpoint shape (`POST /v1/ch/module/refresh`), the
synchronous grouped-by-tag response, and asynchronous processing. Added module-level
de-duplication to the design.

## Step 3 — Steer: update on change; simplicity over efficiency
**Human correction:** "Update the content when a difference is found. I don't mind if
the same module gets examined when it appears in more than one module set tag — we
don't expect many tags to be affected. Simplicity over efficiency."

**Impact:** Chose in-place content update (not detect-only), and deliberately dropped
the cross-tag module de-dup optimisation in favour of a simpler per-tag pass (updates
are idempotent, so re-examination is harmless).

## Step 4 — Steer: no new Hazelcast, but remember it's multi-instance
**Human correction:** "Avoid Hazelcast changes. But be aware several instances might
be involved in syncing the models, as per the current initial sync."

**Analysis update:** Proposed running the async job on the receiving instance via a
plain Spring `@Async` pool (no new distributed structures) and made the in-place DB
update safe under concurrency (shared row, idempotent, FK links preserved).

## Step 5 — Steer: reuse the watchdog and encode intent in the lock reason
**Human correction (the key architectural insight):** "I was thinking to reuse the
async watchdog and multiple instances — put some extra information in the lock reason
to indicate it's a refresh instead of an initial sync."

**Impact:** This became the backbone of the design. Instead of a bespoke async path,
refresh reuses the **existing distributed work queue** with a new `MODULE_REFRESH`
lock reason — no new Hazelcast structures, work distributes across instances for free.

**Analysis update / validation:** Confirmed the mechanics in code:
- `setCompositeStateForRetry` retains the lock reason on LOCKED→ADVISED, so the
  refresh intent survives to `ModuleSyncTasks.processCmHandle` (same trick as
  `MODULE_UPGRADE`).
- A READY handle can't go straight to ADVISED, so refresh must set LOCKED first and
  let the watchdog reset it — exactly like the upgrade flow.

## Step 6 — Story, acceptance criteria, and logging
**Human input:** Asked for a shareable user story, then demo-oriented acceptance
criteria; asked to **combine the before/after model check into one AC**; and added a
requirement to **log when a YANG module change is detected (node + module set tag).**

**Impact:** Produced the user story + AC set (search via sync response; before/after
content via the existing `modules/definitions` endpoint using a stubbed DMI; log the
detected change).

## Step 7 — Steer: small, safe commits (not one big change)
**Human correction:** "Prevent one large commit. Small steps that don't break
anything and are easier to review. First commit = the new endpoint, run the search
using existing code, return the synchronous response — without starting the async
stuff."

**Impact:** Produced the 5-commit plan below, where steps 2–4 land dormant code and
step 5 is the tiny, isolated "switch on".

1. **Endpoint + synchronous response only** — search + group by tag, no state change,
   no async.
2. **Lock reason enum** — add `MODULE_REFRESH` / `MODULE_REFRESH_FAILED`.
3. **Persistence** — in-place YANG content update (new, unused, unit-tested).
4. **Processing side (dormant)** — `refreshModuleContent`, `processCmHandle` branch,
   watchdog query extension, change-detection logging.
5. **Activation** — facade sets matched handles to LOCKED with `MODULE_REFRESH`.

## Step 8 — Steer (team grooming): one sample node per tag + show node states
**Human correction (from team grooming of the user story):** Only **ONE sample node
per module set tag** needs to be refreshed — for performance. Choose the first READY
CM Handle as the sample, and extend the synchronous response with the **state of each
node** in the group, so the operator can see the array of nodes and which one enters
the refresh/sync state.

**Impact:** Confirmed only a single node per tag is refreshed (all nodes for a tag are
assumed to hold identical content and YANG resources are shared, so one update
corrects every CM Handle referencing that tag). Added a dedicated, still-synchronous
commit for sample selection + per-node state, growing the sequence to **6 commits**:

1. **Endpoint + synchronous response only** — search + group by tag.
2. **Sample selection + per-node state** — pick first READY sample per tag, enrich the
   response with each node's state (still synchronous, no side effects).
3. **Lock reason enum** — add `MODULE_REFRESH` / `MODULE_REFRESH_FAILED`.
4. **Persistence** — in-place YANG content update (new, unused, unit-tested).
5. **Processing side (dormant)** — `refreshModuleContent`, `processCmHandle` branch,
   watchdog query extension, change-detection logging.
6. **Activation** — facade locks only the selected sample per tag with
   `MODULE_REFRESH`.

---

## What the collaboration pattern looked like
- **AI strengths used:** fast code archaeology across many files, validating
  mechanics against the real implementation, drafting the design, story, ACs and the
  commit breakdown.
- **Human steering that mattered most:**
  1. Rejecting the destructive delete approach → non-destructive compare/update.
  2. Enforcing the signed-off async + grouped-response + check-once design.
  3. Choosing simplicity over efficiency.
  4. Setting the "no new Hazelcast, but multi-instance aware" constraint.
  5. The reuse-the-watchdog-with-a-lock-reason insight — the design's backbone.
  6. Demanding a small, safe, reviewable commit sequence.
  7. Team grooming: refresh one sample node per tag (performance) and surface each
     node's state in the response.

The full design and technical detail live in `CPS-3293_User_Story_And_Plan.md`.
