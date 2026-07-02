---
navigation_title: "Query datasets"
description: "Query {{esql}} federated datasets with FROM, including metadata columns and unsupported operations."
applies_to:
  stack: preview =9.5
  serverless: preview
products:
  - id: elasticsearch
---

# Query external datasets with {{esql}} Data Federation

A dataset is a read source for the standard {{esql}} pipeline. Every processing command operates on it as on an index. Filters and limits are applied during the file scan. A single query can read more than one source:

* **Several datasets:** `FROM sales, returns` reads both and combines the results.
* **An index together with a dataset:** `FROM orders, sales` combines data held in {{es}} with data held in object storage.

## Metadata columns

Metadata columns are available through `METADATA`:

| Column | Returned for a dataset |
|---|---|
| `_index` | The dataset name. |
| `_id` | A stable per-row identifier. |
| `_version` | The source file's modification time. |
| `_source` | The row as a JSON object. |
| `_file.path`, `_file.name`, `_file.directory`, `_file.size`, `_file.modified` | The object each row was read from. |
| `_score` | null |
| `_ignored` | null |

```esql
FROM access_logs METADATA _file.path, _file.name, _file.size
| KEEP _file.path, _file.name, _file.size, status_code
| LIMIT 10
```

## Limitations

A dataset is a file, not an {{es}} index, so the operations below are not available. Each fails with a clear error rather than wrong results.

| Operation | Reason | Error |
|---|---|---|
| LOOKUP JOIN, with a dataset as the lookup target | A dataset works as the left (source) side of the join. The lookup target on the right must be an {{es}} index. | `LOOKUP JOIN against a dataset is not supported` |
| TS (time series) | A time-series source must be an {{es}} index. | `TS command is not supported for datasets` |
| LOGSDB and other non-standard index modes | These index modes apply only to {{es}} indices. | `LOGSDB index mode on FROM <dataset> is not supported` |
| MATCH, MATCH_PHRASE, KNN | These resolve a field from an index mapping, which a dataset does not have. | `… cannot operate on [<field>], which is not a field from an index mapping` |
| KQL, QSTR | These query an {{es}} index. | `… cannot be used after [FROM <dataset>]` |
| Document-level security (DLS) and field-level security (FLS) | Queries that apply DLS or FLS to a dataset fail at planning time. | |
| Snapshot and restore | Data sources and datasets cannot be snapshotted or restored. | |

Complex Parquet types MAP and nested LIST are not currently supported and return null. STRUCT is supported and flattened to dot-notation column names (for example, `address.city`).
