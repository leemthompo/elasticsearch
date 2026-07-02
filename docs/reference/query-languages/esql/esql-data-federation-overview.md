---
navigation_title: "Overview"
description: "Overview of querying data stored outside Elasticsearch using {{esql}}, including key concepts, supported data sources, and file formats."
applies_to:
  stack: preview =9.5
  serverless: preview
products:
  - id: elasticsearch
---

# {{esql}} Data Federation overview

You can query data stored in compatible external data sources, using the same syntax you use for native indices and other index abstractions, without any ingestion into {{es}}.

## Why use federated data

Many organizations store large volumes of data in cloud object storage for cost and compliance reasons. Querying that data typically requires a separate tool like Apache Spark, Amazon Athena, or Trino, which means managing extra infrastructure and switching between query languages.

{{esql}} federated data enables you to query this data directly from {{es}}, with several advantages:

- **One language for all your data.** Use the same {{esql}} syntax for both indexed data and external data. No context-switching, no second query engine.
- **No extra infrastructure.** Query external data natively in {{es}} without deploying or managing additional compute services, catalogs, or connectors.
- **Progressive acceleration.** Start by querying raw data directly in object storage. When specific datasets need faster performance, promote them into {{es}} for indexed search. Both tiers stay queryable with the same `FROM` syntax.

## How it works

Federated data requires two objects: a data source, which defines the connection, and one or more datasets, which define what to read. The following steps use the REST API workflow to walk through the setup.

<!-- TODO: Add link to Kibana UI docs for creating data sources and datasets once available.
     You can also create data sources and datasets in Kibana. -->

:::::::{stepper}

::::::{step} Your data lives in cloud storage
You have Parquet files, CSVs, or NDJSON sitting in a bucket. The data is not ingested into {{es}}.
::::::

::::::{step} You create a data source (the connection)
A [data source](esql-data-federation-sources.md) tells {{es}} where the storage is and how to authenticate. It stores the connection type, region, endpoint, and credentials. You set it up once.

```console
PUT /_query/data_source/my_s3_bucket
{
  "type": "s3",
  "settings": {
    "region": "us-east-1",
    "access_key": "<AWS_ACCESS_KEY_ID>",
    "secret_key": "<AWS_SECRET_ACCESS_KEY>"
  }
}
```
::::::

::::::{step} You create datasets (what to read)
Each [dataset](esql-data-federation-datasets.md) points at specific files in that storage. One data source can serve many datasets. When credentials rotate, you update the data source in one place without touching the datasets that reference it.

```console
PUT /_query/dataset/my_s3_bucket_logs
{
  "data_source": "my_s3_bucket",
  "resource": "s3://my-logs-bucket/access/**/*.parquet"
}
```

Datasets share the same namespace as indices, aliases, and views. A dataset cannot have the same name as an existing index, which is why `FROM` works the same way for both.
::::::

::::::{step} You query with FROM, just like a regular index
Once a dataset exists, you query it the same way you query any {{es}} index. There is no special syntax for federated data. Use `FROM` with the dataset name, and {{es}} handles file discovery, format detection, compression, and schema inference automatically. For example, to return the first 10 rows from the `my_s3_bucket_logs` dataset created in the previous step:

```esql
FROM my_s3_bucket_logs
| LIMIT 10
```

:::{tip}
For a full worked example, refer to [get started with {{esql}} Data Federation](esql-data-federation-quickstart.md).
:::
::::::

:::::::

## Supported data source types

The following data source types are supported:

:::{include} _snippets/federated-data/supported-data-source-types.md
:::

## Supported file formats

Federated data sources can read the following file formats:

:::{include} _snippets/federated-data/supported-file-formats.md
:::

The format is detected automatically from the file extension. You can override this in the dataset settings if needed.

For details on type-specific settings and format options, refer to [select external datasets](esql-data-federation-datasets.md).

## Capabilities and limitations

Most {{esql}} processing commands and functions work on datasets. The execution engine is the same one used for native indices. Some capabilities that depend on Lucene or specialized data structures are not currently available.

For the full list, refer to [query external datasets](esql-data-federation-querying.md#limitations).
