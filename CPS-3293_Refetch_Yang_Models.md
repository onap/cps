# CPS-3293: Refetch Yang Models

## Issues & Decisions

| # | Issue / Topic | Notes | Decision |
|---|---|---|---|
| 1 | Choose approach(es) | • ModuleSetTag based<br>• DMI Identifier (service name) based<br>• Public (value?) Property Based<br>• ALL module set tags (total refresh)<br>• Support all of the above? | **@Csaba Kocsis** prefers just option 2 DMI Identifier.<br>**Proposal @Toine Siebelink**: use CM Handle Search interface as defined here: *CM Handle Query Endpoints — onap master documentation*. This includes the powerful CPSPath query, which could be used selecting nodes on ModuleSetTag. NCMP will then find the distinct ModuleSetTag’s to 'refresh'.<br>**@Csaba Kocsis @Márton Sági**: Agree to reuse search interface (body for criteria). |
| 2 | Agree format (URL, parameters, body) of new endpoint | | **@Csaba Kocsis**: `/ncmpInventory/v1/ch/module/refresh`<br>• Body identical to cm handle(id) search |
| 3 | Do we really need to change status to ADVISED for ALL affect models | • Compromise: Could just set ONE 'sample' node to that state... (impl detail: use 'REFRESH' in reason to force reloading off all models (yang version does not change)<br>• Can be used if process is easier. Preferred not to use because of performance/external processing due to state changes | **@Toine Siebelink @Csaba Kocsis**: Probably not needed. |
| 4 | Response format | • Synchronous. (refresh will be handled asynchronous)<br>• All CM Handles (alternate Id if available) grouped by Module Set Tag. Flag (first CM Handle which will change state) | **@Csaba Kocsis @Márton Sági**: need to know all cm handles and module set tags, preferably by alternated id. Also need to know which CM Handles are used by the process so know when ready. Exact output can be agreed by mail or chat. |

## Requirements

### Introduction
If a module set is persisted, then it is immutable. It is possible, that the DMI plugin model generation has been changed, and the models of a specific moduleSetTag should be refreshed.

*Note:* The YANG Revision of 'fixed' models will not be updated, so we need to FORCE update even if the module already exists...

### Functional Interface

| # | Requirement | Additional Information | Sign Off |
|---|---|---|---|
| 1 | CPS-NCMP-I-01 | DMI plugin can refresh existing models based on search criteria | New endpoint on existing inventory interface | **@Csaba Kocsis** 23 Jul 2026 |

## Error Scenarios

| # | Error Scenario | Expected Behaviour | Sign Off |
|---|---|---|---|
| 1 | NO CM Handles found | Response still 200, just empty arrays in response body | **@Csaba Kocsis** 23 Jul 2026 |
| 2 | Module sync delay/retry | • As per normal (new/update) module sync process<br>• Cm Handle state (notifications) will inform the user when ready | **@Csaba Kocsis** 23 Jul 2026 |

## Performance & Characteristics

| # | Parameter | Expectation | Additional Information | Sign Off |
|---|---|---|---|---|
| 1 | Requests per second | | | |

## Implementation Proposal

* New REST interface in `ncmpInventory`
* Update `ncmpInventory` OpenAPI specs

### Possible Solutions

There will be a new REST interface of NCMP inventory, which can trigger a model refresh.

#### Model Refresh Process

#### 1. ModuleSetTag Based Model Refresh
`ModuleSetTag` property defines a unique identifier for YANG (or any other model types) module set. The new interface should handle a list of `moduleSetTags` and it will delete the corresponding module resources, plus collect all of the connected cmhandles. After the list is prepared, cmhandles will go back to `ADVISED` state, and fetch the model like a normal discovery process.

*This is required, because this can be used for troubleshooting on live systems.*

* **Concern:** It is possible that one specific module is referenced by multiple `moduleSetTags`. In this case the original module should stay or all of the referenced `moduleSetTags` (and belonging cmhandles) should be refreshed.

#### 2. DMI Identifier Based Model Refresh
There is a `dmi-service-name`, `dmi-data-service-name`, `dmi-model-service-name` (`dmiPluginName`) of all cmhandles, this could be used as a query criteria. Based on this identifier, all cmhandles from that DMI plugin should go back to `ADVISED` state. The modules belonging to their `ModuleSetTag` should be discarded and fetched like a normal discovery process.

* **Concern #1:** The result of this approach is a full DMI plugin rediscovery process, which can impact performance on DMI plugin side.
* **Concern #2:** This way NCMP might re-fetch models, that weren’t changed.

#### 3. Public Property Based
There can be a list defined by public property. Based on this list, `moduleSetTags` and cmhandles will be identified. After this, modules will be deleted and cmhandles will go back to `ADVISED` state, and fetch the model like a normal discovery process.

* **Concern:** This can impact performance on DMI plugin side.

#### 4. All Approaches are Supported
Based on parameter, Option #1, Option #2 and Option #3 are supported by the new interface.

```json
{
  "conditionName": "cmHandleWithDmiPlugin",
  "conditionParameters": [
    {
      "dmiPluginName": "http://example-dmi-plugin-url"
    }
  ]
}
```
