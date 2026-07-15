---
navigation_title: "Manage access"
description: "Credential encryption, credential masking, and privileges for ES|QL federated data sources and datasets."
applies_to:
  stack: preview =9.5
  serverless: preview
products:
  - id: elasticsearch
---

# Manage credentials and privileges for {{esql}} Data Federation

Because federated data lives outside {{es}}, connecting to a private store means {{es}} holds that data source's credentials and must control who can manage connections and read external data. This page covers credential encryption and masking, and the privilege model for data sources and datasets.

## Credentials

When a data source connects to a private bucket, its credentials are stored in the cluster state. {{es}} protects these credentials with encryption at rest and redaction in API responses.

### Credential encryption

<!-- TODO: restore the link to the project encryption key page once PR #152731 (docs/reference/elasticsearch/project-encryption-key.md) merges. Linking now breaks the build because the target does not exist on this branch. Link markup: [project encryption key](../../elasticsearch/project-encryption-key.md) -->
When a data source includes credentials, {{es}} encrypts them before writing them to the cluster state. It uses the cluster's project encryption key, a single key that {{es}} manages on your behalf. Federated data is the first feature to rely on this key.

The project encryption key is available automatically in most deployments:

* On {{ech}}, {{ece}}, {{eck}}, and {{serverless-short}}, the platform provides the key's password for you.
* On self-managed deployments, the password is generated automatically the first time a node starts with security auto-configuration, the same process that configures TLS. You configure it yourself only if security auto-configuration did not run.

<!-- TODO: restore this line once PR #152731 (docs/reference/elasticsearch/project-encryption-key.md) merges. It links to a page not yet on this branch and would break the build.
For how to set the password, rotate the key, and check its health, refer to [project encryption key](../../elasticsearch/project-encryption-key.md). -->

For how to set the password, rotate the key, and check its health, refer to your deployment's encryption documentation.

By default, if the project encryption key is not available when you create a data source, a `PUT /_query/data_source` request that includes credentials returns a `503` error. Retry once the key is available, or configure the key first.

### Credential masking

All credential values are replaced by `::es_redacted::` in GET responses. Credentials are never returned in API responses.

A data source's credentials are masked when its definition is read back, and at query time the store is accessed using the data source's stored credentials.

## Privileges

Dataset operations are authorized by the standard {{es}} [index privileges](../../elasticsearch/security-privileges.md#privileges-list-indices), so a role that already administers or reads the matching index names covers datasets with no additional grant.

| Operation | Privilege | Type |
|---|---|---|
| Query a dataset | `read` | Index, on the dataset name |
| Create, read, or delete a dataset | `manage` or `all` | Index, on the dataset name |
| Dataset administration granted on its own | `create_dataset`, `read_dataset_metadata`, `delete_dataset`, `manage_dataset` | Index, on the dataset name |
| Create or replace a data source | `global.data_source` `create` / `cluster.manage` | Global (fine-grained) / Cluster |
| Read a data source definition | `global.data_source` `read_metadata` / `cluster.manage` | Global (fine-grained) / Cluster |
| Delete a data source | `global.data_source` `delete` / `cluster.manage` | Global (fine-grained) / Cluster |
| All data source operations | `global.data_source` `manage` / `cluster.manage` | Global (fine-grained) / Cluster |

Creating a dataset that references a data source also requires the `read` data source privilege for that data source; the two are authorized independently.

The `read` privilege granted on a dataset name must not carry document-level or field-level security; `FROM <dataset>` is rejected if it does.

`superuser` has full access to data sources and datasets. Data source management is reached only through `superuser` or a role explicitly granted `global.data_source`.

A role configures these privileges as follows. The example grants querying `sales` and `clicks`, dataset administration over `acme_*`, and management of the `acme_*` data sources:

```json
{
  "indices": [
    { "names": ["sales", "clicks"], "privileges": ["read"] },
    { "names": ["acme_*"], "privileges": ["manage"] }
  ],
  "global": {
    "data_source": [
      { "names": ["acme_*"], "privileges": ["manage"] }
    ]
  }
}
```
