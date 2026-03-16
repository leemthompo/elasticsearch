### `project_routing` [esql-project_routing]
```{applies_to}
serverless: preview
```
Limits the scope of a [cross-project search (CPS)](/reference/query-languages/esql/esql-cross-serverless-projects.md) to specific projects before query execution, based on a [Lucene query expression](docs-content://explore-analyze/cross-project-search/cross-project-search-project-routing.md) evaluated against project tags. Excluded projects are not queried, which can reduce cost and latency.

The expression is evaluated against project tags. Currently, only the `_alias` tag is supported for routing.

If `project_routing` is specified in both the `SET` directive and the API request body, the `SET` directive takes precedence.

**Type**: `keyword`

#### Example

Route a query to a specific project by alias:

```esql
SET project_routing="_alias:my-project";
FROM logs*
| STATS COUNT(*)
```

