---
navigation_title: "Secure federated data"
description: "Credential encryption, credential masking, and privileges for {{esql}} federated data sources and datasets."
applies_to:
  stack: preview =9.5
  serverless: preview
products:
  - id: elasticsearch
---

# Manage credentials and privileges for {{esql}} Data Federation

Federated data sources store credentials in the cluster state and use {{es}} privileges to control who can manage connections and query external data. This page covers credential encryption and masking, and the privilege model for data sources and datasets.

## Credentials

When a data source connects to a private bucket, its credentials are stored in the cluster state. {{es}} protects these credentials with encryption at rest and redaction in API responses.

### Credential encryption

When a data source includes credentials, {{es}} encrypts them before storing them in the cluster state. This encryption is configured automatically in {{ech}} and {{serverless-short}} deployments. For self-managed, {{ece}}, and {{eck}} deployments, you must configure encryption manually.

If encryption is not configured, any `PUT /_query/data_source` request that includes credentials returns a `503` error.

::::{applies-switch}
:::{applies-item} self:
Use the [elasticsearch-keystore](../../elasticsearch/command-line-tools/elasticsearch-keystore.md) tool to add a password and set the active password ID on every node:

```bash
bin/elasticsearch-keystore add cluster.state.encryption.password.1
bin/elasticsearch-keystore add cluster.state.encryption.active_password_id
```

When prompted for the active password ID, enter the ID that matches the password setting suffix (in this example, `1`). Then call the [reload secure settings API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-nodes-reload-secure-settings) to apply the new settings without a full restart:

```console
POST /_nodes/reload_secure_settings
```
:::
:::{applies-item} eck:
Add the encryption password and active password ID as [secure settings](docs-content://deploy-manage/security/k8s-secure-settings.md) through Kubernetes secrets referenced in `spec.secureSettings`.
:::
:::{applies-item} ece:
Add the encryption password and active password ID as [secure settings](docs-content://deploy-manage/security/secure-settings.md) using the Cloud UI or the [RESTful API](cloud://reference/cloud-enterprise/restful-api.md).
:::
::::

:::{warning}
To allow plaintext credential storage without encryption, set `cluster.state.encryption.required` to `false`. **This is not recommended for production use.**
:::

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
