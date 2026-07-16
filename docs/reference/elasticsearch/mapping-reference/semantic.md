---
navigation_title: "Semantic"
applies_to:
  stack: preview 9.5
  serverless: preview
---

# Semantic field type [semantic-field]

:::::{warning}
The `semantic` field mapping can be added regardless of license state. However, it calls the [{{infer-cap}} API](https://www.elastic.co/docs/api/doc/elasticsearch/group/endpoint-inference), which requires an [appropriate license](https://www.elastic.co/subscriptions). Using a `semantic` field without the appropriate license causes operations such as indexing and reindexing to fail.
:::::

The `semantic` field type simplifies semantic and multimodal search across text, images, audio, video, and PDF files. With a compatible multimodal embedding model, you can search across content types—for example, use natural-language text to find images, or use an image to find related text and images. The field automatically:

- Generates embeddings when you index field values, without an ingest pipeline or {{infer}} processor.
- Splits long text into smaller passages, called chunks.
- Configures and stores the underlying dense vectors based on the field's {{infer}} endpoint.
- Searches the embeddings generated for each value or text chunk.

:::{include} _snippets/semantic-field-type-comparison.md
:::

The following example maps `content` as a `semantic` field:

```console
PUT my-semantic-index
{
  "mappings": {
    "properties": {
      "content": {
        "type": "semantic",
        "inference_id": "my-embedding-endpoint"
      }
    }
  }
}
```
% TEST[skip:Requires an embedding {{infer}} endpoint]

Unlike [`semantic_text`](./semantic-text.md), a `semantic` field has no default {{infer}} endpoint. You must configure an endpoint that uses the `embedding` task type and specify its ID in the field mapping.

## Parameters for `semantic` fields [semantic-params]

`inference_id`
:   (Required, string) ID of the `embedding` {{infer}} endpoint used to generate embeddings at index time. If you don't specify `search_inference_id`, the same endpoint is also used at query time.

    You can update `inference_id` with the [Update mapping API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-put-mapping) if the field contains no indexed values or if the new endpoint produces embeddings compatible with the existing endpoint. Compatible endpoints use the same dimensions, similarity measure, and element type, and should use the same underlying model.

`search_inference_id`
:   (Optional, string) ID of the `embedding` {{infer}} endpoint used to generate embeddings at query time. If omitted, `inference_id` is used at both index and query time.

    You can update `search_inference_id` with the [Update mapping API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-put-mapping). The search endpoint must produce embeddings that are compatible with those produced by `inference_id`.

`chunking_settings`
:   (Optional, object) Settings that control how text input is divided into chunks before {{infer}}. These settings override the chunking settings configured on `inference_id`. Chunking does not apply to non-text input; each non-text value produces one embedding.

    If you update `chunking_settings`, the new settings apply only to newly indexed or reindexed documents. To disable automatic text chunking, use the `none` strategy.

    Refer to [Configure chunking](docs-content://explore-analyze/elastic-inference/inference-api.md#infer-chunking-config) for the available strategies and settings.

`index_options`
:   (Optional, object) Settings that control how the dense vectors generated for the field are indexed. The `semantic` field supports `dense_vector` index options.

    :::{note}
    This parameter configures vector indexing structures. It is distinct from the [`index_options`](./index-options.md) parameter used by term-based fields.
    :::

    Specify dense-vector options inside the `dense_vector` object:

    ```json
    {
      "index_options": {
        "dense_vector": {
          "type": "int8_hnsw",
          "m": 16,
          "ef_construction": 100
        }
      }
    }
    ```

    Refer to [Dense vector index options](./dense-vector.md#dense-vector-index-options) for the available algorithms and parameters.

    For endpoints that produce `float` embeddings, `semantic` fields use the [`bfloat16`](./dense-vector.md#dense-vector-element-type) element type by default. You can set `index_options.dense_vector.element_type` to `float` to retain full float precision. For endpoints that produce `byte` or `bit` embeddings, an explicit element type must match the endpoint's element type.

`meta`
:   (Optional, object) Metadata about the field. Refer to [`meta`](./mapping-field-meta.md).

The following example customizes the search endpoint, text chunking, and dense-vector index options:

```console
PUT my-semantic-index
{
  "mappings": {
    "properties": {
      "content": {
        "type": "semantic",
        "inference_id": "my-index-embedding-endpoint", <1>
        "search_inference_id": "my-search-embedding-endpoint", <2>
        "chunking_settings": { <3>
          "strategy": "word",
          "max_chunk_size": 250,
          "overlap": 50
        },
        "index_options": { <4>
          "dense_vector": {
            "type": "int8_hnsw"
          }
        }
      }
    }
  }
}
```
% TEST[skip:Requires embedding {{infer}} endpoints]

1. Endpoint used to generate embeddings while indexing.
2. Compatible endpoint used to generate embeddings while querying.
3. Splits text into chunks of at most 250 words, with an overlap of 50 words.
4. Indexes the embeddings using `int8_hnsw` quantization.

## {{infer-cap}} endpoint requirements [semantic-inference-endpoint]

A `semantic` field requires an {{infer}} endpoint with the `embedding` task type. The endpoint determines:

- The input modalities supported by the model.
- The vector dimensions, similarity measure, and element type.
- The default text chunking settings.

The following example creates a Jina AI endpoint for a multimodal embedding model:

```console
PUT _inference/embedding/my-embedding-endpoint
{
  "service": "jinaai",
  "service_settings": {
    "model_id": "jina-embeddings-v4",
    "api_key": "JinaAI-API-key",
    "multimodal_model": true
  }
}
```
% TEST[skip:Requires a Jina AI account and API key]

The `embedding` task type does not guarantee that every endpoint supports every modality. Check the model and service documentation before indexing non-text input.

Removing an endpoint causes indexing and semantic queries to fail for fields that reference it. Elasticsearch prevents you from deleting an endpoint that is referenced by an inference field.

## Accepted input [semantic-input]

A `semantic` field accepts a single value or an array of values. An array can contain a mix of text and non-text values.

### Text input [semantic-text-input]

Provide text directly as a JSON string:

```console
POST my-semantic-index/_doc
{
  "content": "It was the best of times, it was the worst of times."
}
```
% TEST[skip:Requires a configured semantic field]

Long text is divided according to the field's `chunking_settings`. To provide text that is already chunked, set the chunking strategy to `none` and index an array of strings.

### Non-text input [semantic-non-text-input]

Provide non-text input as an object with the following properties:

`type`
:   (Required, string) Type of input. Valid values are `image`, `audio`, `video`, and `pdf`.

`value`
:   (Required, string) Input encoded as a [data URL](https://www.rfc-editor.org/rfc/rfc2397). The value must include its media type and Base64 encoding, for example `data:image/jpeg;base64,...`.

`format`
:   (Optional, string) Input format. The only supported format for non-text input is `base64`, which is also the default.

The following example indexes an image:

```console
POST my-semantic-index/_doc
{
  "content": {
    "type": "image",
    "value": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABA..."
  }
}
```
% TEST[skip:Requires a configured semantic field]

Each non-text value is processed as a single unit and produces one embedding. Elasticsearch does not split non-text input into chunks.

### Mixed input [semantic-mixed-input]

The following example indexes text and images in the same field:

```console
POST my-semantic-index/_doc
{
  "content": [
    "A cat sitting on a windowsill",
    {
      "type": "image",
      "value": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABA..."
    },
    "A dog running through a park"
  ]
}
```
% TEST[skip:Requires a configured semantic field]

## Chunking behavior [semantic-chunking]

Elasticsearch stores each embedding in a hidden nested document:

- Text chunks are associated with start and end character offsets in the original text.
- Non-text embeddings are associated with the position of the value in the input array.

At query time, Elasticsearch searches the individual embeddings and uses the best matching embedding to score the top-level document.

Because chunks are stored as nested documents, the `docs.count` value from the [`_cat/indices`](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-cat-indices) API can be higher than the number of documents you indexed. Use the Count API or `_cat/count` to count only top-level documents.

## Query a `semantic` field [query-semantic-field]

With a compatible multimodal endpoint, indexed content and query input can use different modalities. For example, a text query can find images, or an image query can find related text and images.

The supported query mechanism depends on the type of query input:

- Use a [`match` query](/reference/query-languages/query-dsl/query-dsl-match-query.md) for text query input. Elasticsearch uses the field's search {{infer}} endpoint to generate the query embedding.
- Use a [`knn` query](/reference/query-languages/query-dsl/query-dsl-knn-query.md) with the `embedding` query vector builder for text or non-text input.
- Use a `knn` query with `query_vector` if you already have a vector compatible with the field's endpoint.
- In ES|QL, use [`KNN`](/reference/query-languages/esql/functions-operators/dense-vector-functions/knn.md) with [`EMBEDDING`](/reference/query-languages/esql/functions-operators/dense-vector-functions/embedding.md).

The following example searches for images using image input:

```console
POST my-semantic-index/_search
{
  "query": {
    "knn": {
      "field": "content",
      "query_vector_builder": {
        "embedding": {
          "input": {
            "type": "image",
            "value": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABA..."
          }
        }
      },
      "k": 10,
      "num_candidates": 100
    }
  }
}
```
% TEST[skip:Requires a configured semantic field]

When all targeted fields are inference fields with the same endpoint, Elasticsearch obtains `inference_id` from the field mapping. Specify `inference_id` in the query vector builder when querying a `semantic` field together with a `dense_vector` field or fields that use different endpoints.

## Retrieve values, chunks, and embeddings [retrieve-semantic-field]

The original text and non-text values are returned in `_source`. The generated embeddings are excluded by default.

To retrieve the values as they were processed for {{infer}}, use the `fields` parameter with the `chunks` format:

```console
POST my-semantic-index/_search
{
  "fields": [
    {
      "field": "content",
      "format": "chunks"
    }
  ],
  "_source": false
}
```
% TEST[skip:Requires a configured semantic field]

For text, the response contains the text of each indexed chunk. For non-text input, the response contains the corresponding input object.

To include generated embeddings and their chunk metadata in `_source`, set `_source.exclude_vectors` to `false`:

```console
POST my-semantic-index/_search
{
  "_source": {
    "exclude_vectors": false
  },
  "query": {
    "match_all": {}
  }
}
```
% TEST[skip:Requires a configured semantic field]

The response returns the embeddings under `_inference_fields`.

## Highlight a `semantic` field [highlight-semantic-field]

The [`semantic` highlighter](/reference/elasticsearch/rest-apis/highlighting.md) is the default highlighter for `semantic` fields. It returns the field values or text passages whose embeddings best match the query:

- For text, it returns the most relevant text chunks.
- For non-text input, it returns the complete data URL of each matching value.

Use `number_of_fragments` to limit the number of matches and `order: score` to return the most relevant matches first.

## Limitations [semantic-limitations]

The `semantic` field has the following limitations:

- It can be used only in indices created in Elasticsearch 9.5 or later. You cannot add it to an index created in an earlier version.
- It cannot be used inside a [`nested`](./nested.md) field.
- It cannot be created by a [dynamic template](docs-content://manage-data/data-store/mapping/dynamic-templates.md).
- It cannot be placed in an object that has `subobjects` disabled.
- It does not support term-level queries, sorting, scripting, or aggregations.
- It supports only endpoints with the `embedding` task type and dense-vector embeddings. For sparse embeddings, use [`semantic_text`](./semantic-text.md).
