---
navigation_title: "Connect external data sources"
description: "Create and manage {{esql}} federated data sources, including S3 connection settings and authentication models."
applies_to:
  stack: preview =9.5
  serverless: preview
products:
  - id: elasticsearch
---

# Connect external data sources for {{esql}} Data Federation

A data source defines the connection to an external storage system. It stores the connection type, region, endpoint, and credentials. A data source defines how to connect, not what data to query. One data source can serve many [datasets](esql-data-federation-datasets.md). When credentials rotate, you update the data source in one place without touching the datasets that reference it.

## Supported data source types

The following data source types are supported:

:::{include} _snippets/federated-data/supported-data-source-types.md
:::

<!-- TODO: Once the data source and dataset APIs are defined in elasticsearch-specification,
replace the inline examples below with a summary table linking to the generated
API reference at https://www.elastic.co/docs/api/doc/elasticsearch/ -->

## Data source API

Data sources are managed under the `/_query/data_source` endpoint. All data source operations require the cluster `manage` privilege or a `global.data_source` privilege (refer to [manage credentials and privileges](esql-data-federation-security.md)).

| Operation | Endpoint |
|---|---|
| [Create or update](#create-or-update-a-data-source) | `PUT /_query/data_source/{name}` |
| [Get](#get-a-data-source) | `GET /_query/data_source/{name}` |
| [List all](#list-all-data-sources) | `GET /_query/data_source` |
| [Delete](#delete-a-data-source) | `DELETE /_query/data_source/{name}` |

### Create or update a data source

`PUT` creates a new data source or replaces an existing one entirely. The create call does not validate connectivity to the external system. To verify that credentials and endpoint are correct, create a dataset that references the data source and query it.

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
PUT /_query/data_source/prod_s3_logs
{
  "type": "s3",
  "description": "Production S3 logs bucket, us-east-1",
  "settings": {
    "region": "us-east-1",
    "access_key": "<AWS_ACCESS_KEY_ID>",
    "secret_key": "<AWS_SECRET_ACCESS_KEY>"
  }
}
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X PUT "${ELASTICSEARCH_URL}/_query/data_source/prod_s3_logs" \
  -H "Authorization: ApiKey ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
  "type": "s3",
  "description": "Production S3 logs bucket, us-east-1",
  "settings": {
    "region": "us-east-1",
    "access_key": "<AWS_ACCESS_KEY_ID>",
    "secret_key": "<AWS_SECRET_ACCESS_KEY>"
  }
}'
```
:::

::::

### Get a data source

Retrieves a data source by name. Credential values are replaced by `::es_redacted::` in the response.

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
GET /_query/data_source/prod_s3_logs
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X GET "${ELASTICSEARCH_URL}/_query/data_source/prod_s3_logs" \
  -H "Authorization: ApiKey ${API_KEY}"
```
:::

::::

### List all data sources

Returns all registered data sources.

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
GET /_query/data_source
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X GET "${ELASTICSEARCH_URL}/_query/data_source" \
  -H "Authorization: ApiKey ${API_KEY}"
```
:::

::::

### Delete a data source

Deletes a data source by name.

:::{important}
A data source cannot be deleted while datasets still reference it. Delete the dependent datasets first, or the request returns a `409 Conflict` error.
:::

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
DELETE /_query/data_source/prod_s3_logs
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X DELETE "${ELASTICSEARCH_URL}/_query/data_source/prod_s3_logs" \
  -H "Authorization: ApiKey ${API_KEY}"
```
:::

::::

## Data source settings

Settings vary by data source type.

### S3

The following settings are available for `s3` data sources:

**Connection settings:**

| Setting | Required | Description |
|---|---|---|
| `region` | No | The bucket region. |
| `endpoint` | No | An explicit endpoint, for an S3-compatible store. |

**Authentication settings:**

| Setting | Required | Description |
|---|---|---|
| `access_key` | No | AWS access key ID. |
| `secret_key` | No | AWS secret access key. |
| `session_token` | No | Session token, when using temporary credentials. Use with `access_key` and `secret_key`. |
| `auth` | No | Authentication mode. Defaults to `auto`, which infers the mode from the other settings you provide. Set it explicitly to `anonymous`, `static_credentials`, or `managed_identity`. |

<!-- TODO: Confirm whether federated identity auth is in scope for the 9.5 technical preview.
     It is present in code but operator-gated by esql.datasource.federated_identity.enabled, and
     gated off by default in the Kibana UI (enableFederatedIdentityAuth). If in scope, document the
     `federated_identity` auth value and its S3 settings: role_arn (required), jwt_audience,
     role_session_name, sts_endpoint, sts_region. -->

## Authentication

A data source authenticates to its store with one of the models below. The models are mutually exclusive on a data source.

| Model | `auth` value | Description |
|---|---|---|
| Auto | `auto` (default) | Infers the authentication mode from the settings you provide. |
| Static credentials | `static_credentials` | A fixed access key and secret key, optionally with a session token for temporary credentials. The common form for a service account. |
| Anonymous | `anonymous` | For public data that needs no credentials. |
| Managed identity | `managed_identity` | Keyless, using the node's own cloud identity. Requires `esql.datasource.managed_identity.enabled: true`, an operator-only setting. Not available in serverless. API-only. |

:::{warning}
Managed identity authentication uses the cloud identity attached to each {{es}} node (for example, an IAM role on EC2 or a service account on GKE). Different nodes may have different identities, and the node that performs the connection is not guaranteed. You are responsible for configuring cloud IAM so that every node's identity has the required permissions on the target bucket. This model is best suited for single-cloud, single-tenant deployments where node identities are uniform.
:::

When `access_key` and `secret_key` are omitted and `auth` is left as `auto`, {{es}} uses the default AWS credential chain: IAM roles, environment variables, or instance profiles.
