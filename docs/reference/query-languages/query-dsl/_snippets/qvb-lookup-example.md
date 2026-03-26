```js
{
  "knn": {
    "field": "product-vector",
    "k": 10,
    "num_candidates": 100,
    "query_vector_builder": {
      "lookup": {
        "id": "product-123",
        "index": "seed-products",
        "path": "product-vector",
        "routing": "tenant-a"
      }
    }
  }
}
```
% NOTCONSOLE
