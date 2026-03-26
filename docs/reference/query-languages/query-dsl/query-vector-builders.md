---
navigation_title: "Query vector builders"
applies_to:
  stack: ga
  serverless: ga
description: Reference for query vector builders used in kNN search to generate query vectors at search time from text, images, or existing indexed vectors.
---

# Query vector builders [query-vector-builders]

A query vector builder generates a query vector at search time, so you don't need to compute and supply the vector yourself. Use query vector builders in the `query_vector_builder` parameter of the [`knn` query](/reference/query-languages/query-dsl/query-dsl-knn-query.md) and the [kNN retriever](/reference/elasticsearch/rest-apis/retrievers/knn-retriever.md).

Use `query_vector_builder` when you want to pass raw input (text, images, or a reference to an existing document) and let Elasticsearch build the query vector automatically. You can also pre-compute the vector and supply it directly using `query_vector`.

Three query vector builders are available:

| Builder | Use case |
|---------|----------|
| [`text_embedding`](#text-embedding-qvb) | Generate a query vector from input text using a deployed ML model |
| [`lookup`](#lookup-qvb) | Use a vector from an existing document in an index |
| [`embedding`](#embedding-qvb) | Generate a query vector from text or images using an inference service |

## `text_embedding` [text-embedding-qvb]

Generates a query vector from input text using a [text embedding model](docs-content://explore-analyze/machine-learning/nlp/ml-nlp-search-compare.md#ml-nlp-text-embedding) deployed in Elasticsearch. Start the model before use.

### Parameters [text-embedding-qvb-params]

:::{include} _snippets/qvb-text-embedding-params.md
:::

### Example [text-embedding-qvb-example]

:::{include} _snippets/qvb-text-embedding-example.md
:::

For a full end-to-end example, refer to [Perform semantic search](docs-content://solutions/search/vector/knn.md#knn-semantic-search).

---

## `lookup` [lookup-qvb]

```{applies_to}
stack: ga 9.4+
serverless: ga
```

Builds the query vector by fetching a vector stored in an existing document at search time. Use this builder to avoid a separate client round-trip when the query vector already exists in Elasticsearch.

The source document must contain a `dense_vector` field with a single vector value. The fetched vector must be compatible with the target kNN field (same dimensions and same embedding model semantics).

### Parameters [lookup-qvb-params]

:::{include} _snippets/qvb-lookup-params.md
:::

### Example [lookup-qvb-example]

:::{include} _snippets/qvb-lookup-example.md
:::

For more detail, refer to [Use `lookup` to build the query vector](docs-content://solutions/search/vector/knn.md#knn-query-vector-lookup).

---

## `embedding` [embedding-qvb]

```{applies_to}
stack: ga 9.4+
serverless: ga
```

Generates a query vector from multimodal inputs using an [inference service](https://www.elastic.co/docs/api/doc/elasticsearch/group/endpoint-inference) that supports the `EMBEDDING` task type. Supported input types are text and base64-encoded images. Use this builder when you want to use an external inference service rather than a locally deployed ML model, or when querying multimodal embeddings.

### Parameters [embedding-qvb-params]

:::{include} _snippets/qvb-embedding-params.md
:::

### Examples [embedding-qvb-examples]

The following examples show how to use the `embedding` builder with different input types.

#### Text input [embedding-qvb-text]

Pass a plain string and Elasticsearch converts it to an embedding using the specified inference endpoint:

:::{include} _snippets/qvb-embedding-example-text.md
:::

#### Image input [embedding-qvb-image]

Pass a base64-encoded image as a single input object:

:::{include} _snippets/qvb-embedding-example-image.md
:::

#### Multimodal input [embedding-qvb-multimodal]

Pass multiple inputs together to create a combined multimodal embedding:

:::{include} _snippets/qvb-embedding-example-multimodal.md
:::
