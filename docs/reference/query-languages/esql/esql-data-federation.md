---
navigation_title: "Data Federation"
description: "Query data stored in external cloud storage using {{esql}} without ingesting it into Elasticsearch."
applies_to:
  stack: preview =9.5
  serverless: preview
products:
  - id: elasticsearch
---

# {{esql}} Data Federation

Query data stored in compatible external storage systems using the same {{esql}} syntax you use for native indices, without any ingestion into {{es}}.

[Overview](esql-data-federation-overview.md)
: What federated data is, why you would use it, how the pieces fit together, and what file formats and data source types are supported.

[Quickstart](esql-data-federation-quickstart.md)
: Register a data source, create a dataset, and run your first federated query end-to-end.

[Connect external data sources](esql-data-federation-sources.md)
: Supported data source types, the data source API, S3 connection settings, and authentication models.

[Select datasets to query](esql-data-federation-datasets.md)
: Supported file formats, the dataset API, format-specific settings, and schema inference.

[Query datasets](esql-data-federation-querying.md)
: How `FROM` works with datasets, metadata columns, and unsupported operations.

[Secure federated data](esql-data-federation-security.md)
: Privileges for data sources and datasets, credential encryption, and credential masking.

[Configure cluster settings](esql-data-federation-cluster-settings.md)
: Object limits, request concurrency, file-discovery limits, authentication gates, and caching.
