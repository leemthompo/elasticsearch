---
navigation_title: "Add datasets"
description: "Create and manage ES|QL federated datasets, including supported file formats, dataset settings, and schema inference."
applies_to:
  stack: preview =9.5
  serverless: preview
products:
  - id: elasticsearch
---

# Select external datasets for {{esql}} Data Federation

A dataset points at specific files within a [data source](esql-data-federation-sources.md) and makes them queryable as a virtual index. It references a data source by name and specifies a resource path that identifies the files to read. Datasets share the same namespace as indices, aliases, and [{{esql}} views](esql-views.md). A dataset cannot have the same name as an existing index.

## Supported file formats

Federated data sources can read the following file formats:

:::{include} _snippets/federated-data/supported-file-formats.md
:::

The format is detected automatically from the file extension. You can override this in the [dataset settings](#common-settings).

### Text formats

The following text formats are recognized by file extension:

| Format | Recognized extensions |
|---|---|
| CSV | `.csv` |
| TSV | `.tsv` |
| NDJSON | `.ndjson`, `.jsonl`, `.json` |

### Compression for text formats

A text resource is read uncompressed, or compressed with a codec identified from a trailing extension: `clicks.csv`, `clicks.csv.gz`, `clicks.csv.zst`.

| Codec | Extensions |
|---|---|
| uncompressed | none |
| gzip | `.gz`, `.gzip` |
| zstd | `.zst`, `.zstd` |

### Parquet

Parquet declares its compression internally, per column chunk, so Parquet resources are not externally compressed. They are recognized by the `.parquet` and `.parq` extensions. Whether a file can be read depends on the codec its writer used for its column chunks:

| Parquet codec | Status |
|---|---|
| UNCOMPRESSED | Read |
| SNAPPY | Read |
| ZSTD | Read |
| GZIP | Read |

SNAPPY, ZSTD, and GZIP account for the overwhelming majority of Parquet in practice, so the supported set covers nearly all real files.

## Manage datasets in the UI

In {{kib}}, you create and manage datasets from the **Datasets** tab under **Data management** > **{{esql}} Data Federation**.

The **Datasets** tab lists each dataset including:
- its data source and data source type
- its resource
- its description

From this tab you can search your datasets, filter by data source, add a new one, and edit or delete an existing one.

:::{image} images/data-federation/datasets-tab.png
:alt: The Datasets tab listing several datasets with their data sources, resources, and edit and delete row actions
:width: 800px
:::

### Add a new dataset

Click **Add dataset** to open a flyout where you define the dataset:

- **Data source**: the connected data source to read through.
- **Name**: a unique name for use in queries.
- **Description**: an optional description.
- **Resource**: the URI and glob pattern that selects the files to read.
- **Format**: the file format. This selection is required in the {{kib}} UI. The API can omit `settings.format` to auto-detect it from the file extension. Refer to [supported file formats](#supported-file-formats).

To configure how the format is read, expand **Advanced settings**. Refer to [dataset settings](#dataset-settings).

:::{dropdown} Show the Add dataset flyout
:::{image} images/data-federation/add-dataset.png
:alt: The Add dataset flyout configured for a Parquet dataset over an Amazon S3 data source
:width: 450px
:::
:::

<!-- TODO: Once the data source and dataset APIs are defined in elasticsearch-specification,
replace the inline examples below with a summary table linking to the generated
API reference at https://www.elastic.co/docs/api/doc/elasticsearch/ -->

## Manage datasets using the API

Datasets are managed under the `/_query/dataset` endpoint. All dataset operations require the index `manage` privilege on the dataset name, or a fine-grained dataset privilege (refer to [manage credentials and privileges](esql-data-federation-security.md)).

| Operation | Endpoint |
|---|---|
| [Create or update](#create-or-update-a-dataset) | `PUT /_query/dataset/{name}` |
| [Get](#get-a-dataset) | `GET /_query/dataset/{name}` |
| [List all](#list-all-datasets) | `GET /_query/dataset` |
| [Delete](#delete-a-dataset) | `DELETE /_query/dataset/{name}` |

### Create or update a dataset

`PUT` creates a new dataset or replaces an existing one entirely.

:::{important}
A dataset cannot have the same name as an existing index, alias, or view, because dataset names share the same namespace. Dataset names must be lowercase and cannot begin with `-`, `_`, or `+`.
:::

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
PUT /_query/dataset/access_logs
{
  "data_source": "prod_s3_logs",
  "resource": "s3://logs-bucket/access/**/*.parquet",
  "description": "Production access logs",
  "settings": {
    "partition_detection": "hive"
  }
}
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X PUT "${ELASTICSEARCH_URL}/_query/dataset/access_logs" \
  -H "Authorization: ApiKey ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
  "data_source": "prod_s3_logs",
  "resource": "s3://logs-bucket/access/**/*.parquet",
  "description": "Production access logs",
  "settings": {
    "partition_detection": "hive"
  }
}'
```
:::

::::

### Declare a dataset mapping

By default, {{es}} infers a dataset's schema from its files. You can instead add an optional `mappings` block to the create or update request to control column names and types. Dataset mappings are currently available only through the API; the {{kib}} **Add dataset** flyout does not expose them.

The following example declares the complete schema, renames the physical `event_time` column to `@timestamp`, supplies its date format, and uses `request_id` as the row's `_id`:

```console
PUT /_query/dataset/access_logs
{
  "data_source": "prod_s3_logs",
  "resource": "s3://logs-bucket/access/**/*.csv",
  "mappings": {
    "dynamic": "false",
    "properties": {
      "@timestamp": {
        "type": "date",
        "path": "event_time",
        "format": "yyyy-MM-dd HH:mm:ss"
      },
      "request_id": { "type": "keyword" },
      "service": { "type": "keyword" },
      "status_code": { "type": "integer" }
    },
    "_id": {
      "path": "request_id"
    }
  }
}
```

The `mappings` block supports:

- `properties`: Columns keyed by their logical name. Each column requires a `type`.
  - `path`: Optional physical column name. Use it to expose a file column under a different logical name, including renaming a timestamp column to `@timestamp`.
  - `format`: Optional date parsing pattern for a column with type `date`.
- `_id.path`: Optional source column whose value becomes the row's `_id`.
- `dynamic`: Controls undeclared columns. The default, `true`, overlays the declared columns on the inferred schema. Set it to `false` to treat the declaration as the complete schema, skip schema inference for text formats, and leave undeclared columns unavailable to queries.

:::{note}
With `dynamic: false`, declared columns in CSV and TSV files bind by position. For self-describing columnar formats such as Parquet, declared column names and types must match the file schema unless you use `path` to rename a column.
:::

### Get a dataset

Retrieves a dataset by name.

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
GET /_query/dataset/access_logs
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X GET "${ELASTICSEARCH_URL}/_query/dataset/access_logs" \
  -H "Authorization: ApiKey ${API_KEY}"
```
:::

::::

### List all datasets

Returns all registered datasets.

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
GET /_query/dataset
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X GET "${ELASTICSEARCH_URL}/_query/dataset" \
  -H "Authorization: ApiKey ${API_KEY}"
```
:::

::::

### Delete a dataset

Deletes a dataset by name.

::::{tab-set}
:group: api-ref

:::{tab-item} Console
:sync: console
```console
DELETE /_query/dataset/access_logs
```
:::

:::{tab-item} curl
:sync: curl
```bash
curl -X DELETE "${ELASTICSEARCH_URL}/_query/dataset/access_logs" \
  -H "Authorization: ApiKey ${API_KEY}"
```
:::

::::

## Dataset settings

Dataset settings configure how a resource's format is read. They are specified in the `settings` object of a dataset definition. They divide into settings users commonly change and advanced settings with sensible defaults.

### Common settings

The following settings apply to all file-based data sources:

| Setting | Default | Description |
|---|---|---|
| `format` | Auto-detect from extension | Override format detection. Valid values: `"parquet"`, `"csv"`, `"tsv"`, `"ndjson"`. |
| `partition_detection` | `auto` | Partition detection mode. Valid values: `"auto"`, `"hive"`, `"none"`. |
| `schema_resolution` | `union_by_name` | How schemas are reconciled across multiple files. Valid values: `"first_file_wins"`, `"strict"`, `"union_by_name"`. Refer to [schema merge strategies](#schema-merge-strategies). |

### CSV and TSV settings

**Commonly changed:**

| Setting | Default (CSV / TSV) | Description |
|---|---|---|
| `delimiter` | `,` / `\t` | The field separator. |
| `mode` | `quoted` / `plain` | A preset bundling quoting and escaping into one choice. Valid values: `"quoted"`, `"escaped"`, `"plain"`. |
| `header_row` | `true` | Whether the first row names the columns. |
| `null_value` | `""` (empty) | The token read as null (for example `NULL`, `NA`, `\N`). |
| `encoding` | `UTF-8` | The file's character encoding. |

**Advanced:**

| Setting | Default (CSV / TSV) | Description |
|---|---|---|
| `quote` | `"` / `"` | The quote character. Subsumed by `mode`. |
| `escape` | `\` / `\` | The escape character. Subsumed by `mode`. |
| `comment` | `//` | Lines beginning with this prefix are skipped. |
| `column_prefix` | `col` | Prefix for generated column names when `header_row` is `false`. |
| `schema_sample_size` | `20000` | Rows sampled to infer column types. |
| `datetime_format` | ISO-8601 | The pattern used to parse date and time values. |
| `multi_value_syntax` | `none` | Whether bracketed multi-values are recognized. Valid values: `"none"`, `"brackets"`. |
| `max_field_size` | `10485760` (10 MB) | The maximum size of a single field. `0` is unlimited. |
| `error_mode` | `fail_fast` | How a malformed row is handled. Valid values: `"fail_fast"`, `"skip_row"`, `"null_field"`. |
| `max_errors` | unbounded | Bad rows tolerated. Not valid with `fail_fast`. |
| `max_error_ratio` | `0.0` | Fraction of bad rows tolerated (0.0–1.0). Not valid with `fail_fast`. |

### NDJSON settings

**Commonly changed:**

| Setting | Default | Description |
|---|---|---|
| `schema_sample_size` | `20000` | Lines sampled to infer the schema. Determines whether sparse or late-appearing fields get a column. |

**Advanced:**

| Setting | Default | Description |
|---|---|---|
| `segment_size` | `4mb` | The unit a file is divided into for parallel reading. Minimum 64 KiB. |

### Parquet

Parquet is self-describing and is read with no settings in the common case. Its two settings are read-performance toggles, defaulted on.

| Setting | Default | Description |
|---|---|---|
| `optimized_reader` | `true` | Uses vectorized decoding, page skipping, and I/O prefetch for the next row group. Leave enabled for normal scans. Disable it only to troubleshoot a suspected optimized-reader issue by using the baseline read path. |
| `late_materialization` | `true` | When a filter can be pushed to the reader, reads predicate columns first and materializes other projected columns only for surviving rows. This is most useful for selective queries over wide files. Leave enabled unless you are troubleshooting filter or read-path behavior. |

## How schemas are inferred

<!-- TODO: Confirm whether the schema discovery API (GET /_query/data_source/{name}/_schema) is public.
     Tracked in https://github.com/elastic/esql-planning/issues/288 -->

Because federated data does not live in {{es}}, the system discovers schemas before queries can run. How this works depends on the file format.

### Schema sources by format

For **Parquet**, schemas are read from file headers. These formats also provide metadata like column statistics and bloom filters that the engine uses to skip irrelevant data.

For **CSV and NDJSON**, schemas are inferred by sampling rows from the data files.

### Schema merge strategies

When a dataset spans multiple files, the files may have different schemas. Set `schema_resolution` in the dataset's `settings` object to choose a strategy:

- `union_by_name` (default): Merges schemas from all files by column name. Lossless type widening is applied where possible; incompatible types cause an error. This is safer when files can vary, at the cost of reading and merging more file metadata.
- `first_file_wins`: Uses the first file alphabetically to define the schema and assumes later files match it. This is typically faster, but schema differences in later files can cause query errors or values to be read under the wrong assumptions.
- `strict`: Requires every file to have the same schema, apart from nullability, and returns an error when they differ. Use this when schema drift must fail explicitly.
