```js
{
  "knn": {
    "field": "dense-vector-field",
    "k": 10,
    "num_candidates": 100,
    "query_vector_builder": {
      "text_embedding": {
        "model_id": "my-text-embedding-model",
        "model_text": "The opposite of blue"
      }
    }
  }
}
```
% NOTCONSOLE
