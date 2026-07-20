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

## `semantic` field quickstart [semantic-quickstart]

This quickstart maps a `semantic` field, indexes an image, and searches for the image using natural-language text. Before you start, configure an {{infer}} endpoint with the `embedding` task type that supports both text and image input.

:::::{stepper}

::::{step} Create an index

Map `content` as a `semantic` field. Replace `my-embedding-endpoint` with the ID of your {{infer}} endpoint:

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

::::

::::{step} Index an image

Encode an image as a Base64 [data URL](https://developer.mozilla.org/en-US/docs/Web/URI/Reference/Schemes/data), then index it in the `content` field:

```console
PUT my-semantic-index/_doc/1
{
  "content": {
    "type": "image",
    "value": "data:image/jpeg;base64,<BASE64_ENCODED_IMAGE>"
  }
}
```
% TEST[skip:Requires a Base64-encoded image and a multimodal embedding endpoint]

Elasticsearch uses the field's {{infer}} endpoint to generate and index an embedding for the image.

::::

::::{step} Search for the image using text

Run a `match` query against the `content` field. For example, if the indexed image shows a cat on a windowsill:

```console
GET my-semantic-index/_search
{
  "_source": false,
  "query": {
    "match": {
      "content": "a cat sitting on a windowsill"
    }
  }
}
```
% TEST[skip:Requires an indexed image and a multimodal embedding endpoint]

The endpoint embeds the text query in the same vector space as the indexed image and returns the most semantically similar results.

::::

:::::

:::{include} _snippets/semantic-field-type-comparison.md
:::

## Reference [semantic-reference]

Refer to the [`semantic` field reference](./semantic-field-reference.md) for complete technical details:

- [Parameters](./semantic-field-reference.md#semantic-params): Mapping parameters for `semantic` fields.
- [{{infer-cap}} endpoint requirements](./semantic-field-reference.md#semantic-inference-endpoint): Supported endpoint task type, model settings, and modalities.
- [Accepted input](./semantic-field-reference.md#semantic-input): Text, non-text, and mixed input formats.
- [Chunking behavior](./semantic-field-reference.md#semantic-chunking): How field values are divided and stored.
- [Querying](./semantic-field-reference.md#query-semantic-field): Query DSL, retriever API, and ES|QL support.
- [Automatic pre-filtering](./semantic-field-reference.md#semantic-automatic-prefiltering): How filters are applied to semantic vector search.
- [Retrieval](./semantic-field-reference.md#retrieve-semantic-field): Retrieve original values, chunks, and embeddings.
- [Highlighting](./semantic-field-reference.md#highlight-semantic-field): Return the most relevant values or text passages.
- [Limitations](./semantic-field-reference.md#semantic-limitations): Current limitations of `semantic` fields.
