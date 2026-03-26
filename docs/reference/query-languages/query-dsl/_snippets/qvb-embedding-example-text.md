```js
{
  "knn": {
    "field": "embedding-field",
    "k": 10,
    "num_candidates": 100,
    "query_vector_builder": {
      "embedding": {
        "inference_id": "my-multimodal-endpoint",
        "input": "a dog playing in the snow"
      }
    }
  }
}
```
% NOTCONSOLE
