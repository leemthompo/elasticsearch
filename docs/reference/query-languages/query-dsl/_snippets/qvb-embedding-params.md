`inference_id`
:   (Required, string) The ID of the inference endpoint to use. The endpoint must support the `EMBEDDING` task type.

`input`
:   (Required) The input to generate the embedding from. Accepts one of:

    - **String shorthand**: A plain text string. Equivalent to `{ "type": "text", "format": "text", "value": "<string>" }`.

    - **Single input object**: An object with the following fields:

      `type`
      :   (Required, string) The modality of the input. Valid values: `text`, `image`.

      `format`
      :   (Optional, string) The format of the value. Valid values: `text`, `base64`. Defaults to the default format for the given `type` (`text` for `text` inputs, `base64` for `image` inputs).

      `value`
      :   (Required, string) The input value to embed.

    - **List of input objects**: An array of input objects (as described above) for multimodal inputs that combine text and images in a single embedding request.

`timeout`
:   (Optional, [time value](/reference/elasticsearch/rest-apis/api-conventions.md#time-units)) The timeout for the inference request. Defaults to `30s`.
