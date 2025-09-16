---
applies_to:
  stack:
  serverless: unavailable
navigation_title: "Combine data with ENRICH"
mapped_pages:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/esql-enrich-data.html
---

# Combine data from multiple indices with ENRICH [esql-enrich-data]

The {{esql}} [`ENRICH`](/reference/query-languages/esql/commands/enrich) processing command combines, at query-time, data from one or more source indexes with field-value combinations found in Elasticsearch enrich indexes.

For complete syntax details and examples, see the [ENRICH command reference](/reference/query-languages/esql/commands/enrich).

## Overview

You can use `ENRICH` to:
* Identify web services or vendors based on known IP addresses
* Add product information to retail orders based on product IDs
* Supplement contact information based on an email address

[`ENRICH`](/reference/query-languages/esql/commands/enrich) is similar to [`LOOKUP join`](/reference/query-languages/esql/commands/lookup-join) in the fact that they both help you join data together. You should use `ENRICH` when:
* Enrichment data doesn't change frequently
* You can accept index-time overhead
* You can accept having multiple matches combined into multi-values
* You can accept being limited to predefined match fields
* You do not need fine-grained security: There are no restrictions to specific enrich policies or document and field level security.
* You want to match using ranges or spatial relations

## How the ENRICH command works [esql-how-enrich-works]

The `ENRICH` command adds new columns to a table, with data from Elasticsearch indices. It requires a few special components:

:::{image} ../images/esql-enrich.png
:alt: esql enrich
:::

$$$esql-enrich-policy$$$

Enrich policy
:   A set of configuration options used to add the right enrich data to the input table.

An enrich policy contains:
* A list of one or more *source indices* which store enrich data as documents
* The *policy type* which determines how the processor matches the enrich data to incoming documents
* A *match field* from the source indices used to match incoming documents
* *Enrich fields* containing enrich data from the source indices you want to add to incoming documents

After [creating a policy](#create-an-enrich-policy), it must be [executed](#execute-the-enrich-policy) before it can be used. Executing an enrich policy uses data from the policy's source indices to create a streamlined system index called the *enrich index*. The `ENRICH` command uses this index to match and enrich an input table.

$$$esql-source-index$$$

Source index
:   An index which stores enrich data that the `ENRICH` command can add to input tables. You can create and manage these indices just like a regular Elasticsearch index. You can use multiple source indices in an enrich policy. You also can use the same source index in multiple enrich policies.

$$$esql-enrich-index$$$

Enrich index
:   A special system index tied to a specific enrich policy.

Directly matching rows from input tables to documents in source indices could be slow and resource intensive. To speed things up, the `ENRICH` command uses an enrich index.

Enrich indices contain enrich data from source indices but have a few special properties to help streamline them:
* They are system indices, meaning they're managed internally by Elasticsearch and only intended for use with enrich processors and the {{esql}} `ENRICH` command.
* They always begin with `.enrich-*`.
* They are read-only, meaning you can't directly change them.
* They are [force merged](/reference/api/indices/forcemerge) for fast retrieval.

## Cross-cluster enrichment

You can use `ENRICH` to add data from indices located in remote clusters. This enables you to centralize enrichment data in one cluster while querying from another.

Key considerations for cross-cluster enrichment:
* Remote enrich policies must be executed on the remote cluster before use
* Network latency may impact query performance
* Ensure proper cluster connectivity and security configuration

For detailed information about cross-cluster enrichment syntax and configuration, see [enrich across clusters](/reference/query-languages/esql/esql-cross-clusters#ccq-enrich).

## Set up an enrich policy [esql-set-up-enrich-policy]

To start using `ENRICH`, follow these steps:
1. Check the [prerequisites](#prerequisites).
2. [Add enrich data](#add-enrich-data).
3. [Create an enrich policy](#create-an-enrich-policy).
4. [Execute the enrich policy](#execute-the-enrich-policy).
5. [Use the enrich policy](#use-the-enrich-policy)

Once you have enrich policies set up, you can [update your enrich data](#update-an-enrich-index) and [update your enrich policies](#update-an-enrich-policy).

::::{important}
The `ENRICH` command performs several operations and may impact the speed of your query.
::::

### Prerequisites [esql-enrich-prereqs]

To use enrich policies, you must have:
* `read` index privileges for any indices used
* The `enrich_user` [built-in role](/reference/elasticsearch/roles)

### Add enrich data [esql-create-enrich-source-index]

To begin, add documents to one or more source indices. These documents should contain the enrich data you eventually want to add to incoming data.

You can manage source indices just like regular Elasticsearch indices using the [document](/reference/api/document) and [index](/reference/api/indices) APIs.

You also can set up [Beats](/reference/beats), such as a [Filebeat](/reference/beats/filebeat/filebeat-installation-configuration), to automatically send and index documents to your source indices. See [Getting started with Beats](/reference/beats).

### Create an enrich policy [esql-create-enrich-policy]

After adding enrich data to your source indices, use the [create enrich policy API](/reference/api/enrich/put-enrich-policy) or [Index Management in Kibana](/reference/index-mgmt.html#manage-enrich-policies) to create an enrich policy.

::::{warning}
Once created, you can't update or change an enrich policy. See [Update an enrich policy](#update-an-enrich-policy).
::::

### Execute the enrich policy [esql-execute-enrich-policy]

Once the enrich policy is created, you need to execute it using the [execute enrich policy API](/reference/api/enrich/execute-enrich-policy) or [Index Management in Kibana](/reference/index-mgmt.html#manage-enrich-policies) to create an [enrich index](#esql-enrich-index).

:::{image} ../images/esql-enrich-policy.png
:alt: esql enrich policy
:::

The *enrich index* contains documents from the policy's source indices. Enrich indices always begin with `.enrich-*`, are read-only, and are [force merged](/reference/api/indices/forcemerge).

::::{warning}
Enrich indices should only be used by the [enrich processor](/reference/enrich-processor/enrich-processor) or the [{{esql}} `ENRICH` command](/reference/query-languages/esql/commands/enrich). Avoid using enrich indices for other purposes.
::::

### Use the enrich policy [esql-use-enrich]

After the policy has been executed, you can use the [`ENRICH` command](/reference/query-languages/esql/commands/enrich) to enrich your data.

:::{image} ../images/esql-enrich-command.png
:alt: esql enrich command
:::

The following example uses the `languages_policy` enrich policy to add a new column for each enrich field defined in the policy. The match is performed using the `match_field` defined in the [enrich policy](#esql-enrich-policy) and requires that the input table has a column with the same name (`language_code` in this example). `ENRICH` will look for records in the [enrich index](#esql-enrich-index) based on the match field value.

```esql
ROW language_code = "1"
| ENRICH languages_policy
```

| --- | --- |
| language_code:keyword | language_name:keyword |
| --- | --- |
| 1 | English |

To use a column with a different name than the `match_field` defined in the policy as the match field, use `ON <column-name>`:

```esql
ROW a = "1"
| ENRICH languages_policy ON a
```

| --- | --- |
| a:keyword | language_name:keyword |
| --- | --- |
| 1 | English |

By default, each of the enrich fields defined in the policy is added as a column. To explicitly select the enrich fields that are added, use `WITH <field1>, <field2>, ...`:

```esql
ROW a = "1"
| ENRICH languages_policy ON a WITH language_name
```

| --- | --- |
| a:keyword | language_name:keyword |
| --- | --- |
| 1 | English |

You can rename the columns that are added using `WITH new_name=<field1>`:

```esql
ROW a = "1"
| ENRICH languages_policy ON a WITH name = language_name
```

| --- | --- |
| a:keyword | name:keyword |
| --- | --- |
| 1 | English |

In case of name collisions, the newly created columns will override existing columns.

For complete syntax details and all available options, see the [ENRICH command reference](/reference/query-languages/esql/commands/enrich).

## Manage enrich policies

### Update an enrich index [esql-update-enrich-data]

Once created, you cannot update or index documents to an enrich index. Instead, update your source indices and [execute](/reference/api/enrich/execute-enrich-policy) the enrich policy again. This creates a new enrich index from your updated source indices. The previous enrich index will be deleted with a delayed maintenance job that executes by default every 15 minutes.

### Update an enrich policy [esql-update-enrich-policies]

Once created, you can't update or change an enrich policy. Instead, you can:
1. Create and [execute](/reference/api/enrich/execute-enrich-policy) a new enrich policy.
2. Replace the previous enrich policy with the new enrich policy in any in-use enrich processors or {{esql}} queries.
3. Use the [delete enrich policy](/reference/api/enrich/delete-enrich-policy) API or [Index Management in Kibana](/reference/index-mgmt.html#manage-enrich-policies) to delete the previous enrich policy.

## Enrich policy types and limitations [_enrich_policy_types_and_limitations]

The {{esql}} `ENRICH` command supports all three enrich policy types:

geo_match
:   Matches enrich data to incoming documents based on a [`geo_shape` query](/reference/query-languages/query-dsl/query-dsl-geo-shape-query). For an example, see [Example: Enrich your data based on geolocation](/manage-data/ingest/transform-enrich/example-enrich-data-based-on-geolocation).

match
:   Matches enrich data to incoming documents based on a [`term` query](/reference/query-languages/query-dsl/query-dsl-term-query). For an example, see [Example: Enrich your data based on exact values](/manage-data/ingest/transform-enrich/example-enrich-data-based-on-exact-values).

range
:   Matches a number, date, or IP address in incoming documents to a range in the enrich index based on a [`term` query](/reference/query-languages/query-dsl/query-dsl-term-query). For an example, see [Example: Enrich your data by matching a value to a range](/manage-data/ingest/transform-enrich/example-enrich-data-by-matching-value-to-range).

While all three enrich policy types are supported, there are some limitations to be aware of:
* The `geo_match` enrich policy type only supports the `intersects` spatial relation.
* It is required that the `match_field` in the `ENRICH` command is of the correct type. For example, if the enrich policy is of type `geo_match`, the `match_field` in the `ENRICH` command must be of type `geo_point` or `geo_shape`. Likewise, a `range` enrich policy requires a `match_field` of type `integer`, `long`, `date`, or `ip`, depending on the type of the range field in the original enrich index.
* However, this constraint is relaxed for `range` policies when the `match_field` is of type `KEYWORD`. In this case the field values will be parsed during query execution, row by row. If any value fails to parse, the output values for that row will be set to `null`, an appropriate warning will be produced and the query will continue to execute.

## Related pages

* [ENRICH command reference](/reference/query-languages/esql/commands/enrich) - Complete syntax documentation and examples
* [Enrich across clusters](/reference/query-languages/esql/esql-cross-clusters#ccq-enrich) - Cross-cluster enrichment configuration
* [LOOKUP JOIN command](/reference/query-languages/esql/commands/lookup-join) - Alternative approach for joining data