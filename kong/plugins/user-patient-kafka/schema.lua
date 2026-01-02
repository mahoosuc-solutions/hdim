local typedefs = require "kong.db.schema.typedefs"

return {
  name = "user-patient-kafka",
  fields = {
    { consumer = typedefs.no_consumer },
    { protocols = typedefs.protocols_http },
    {
      config = {
        type = "record",
        fields = {
          {
            kafka = {
              type = "record",
              required = true,
              fields = {
                { brokers = { type = "array", elements = { type = "string" }, required = true } },
                { topic = { type = "string", required = true } },
                { timeout_ms = { type = "number", default = 1000 } },
                { required_acks = { type = "number", default = 1 } },
                { compression = { type = "string", one_of = { "gzip", "snappy", "lz4", "zstd", "none" }, default = "gzip" } },
                { batch_num = { type = "number", default = 200 } },
                { flush_time = { type = "number", default = 1000 } },
              },
            },
          },
          { user_claim = { type = "string", default = "sub" } },
          { tenant_claim = { type = "string", default = "tenant" } },
          { user_header_fallback = { type = "string", default = "X-User-ID" } },
          { tenant_header_fallback = { type = "string", default = "X-Tenant-ID" } },
          { request_id_header = { type = "string", default = "X-Request-ID" } },
          { require_patient = { type = "boolean", default = false } },
          {
            patient_sources = {
              type = "array",
              elements = {
                type = "record",
                fields = {
                  { type = { type = "string", one_of = { "header", "path_regex", "query" }, required = true } },
                  { name = { type = "string" } }, -- for header
                  { format = { type = "string", one_of = { "json_array", "csv", "raw" }, default = "json_array" } },
                  { allow_repeated_headers = { type = "boolean", default = false } },
                  {
                    fields = {
                      type = "record",
                      required = false,
                      fields = {
                        { id_key = { type = "string", default = "patient_id" } },
                        { assigning_key = { type = "string", default = "assigning_authority" } },
                      },
                    },
                  },
                  { regex = { type = "string" } },
                  { assigning_authority = { type = "string", default = "default" } },
                  { param = { type = "string" } }, -- for query
                },
              },
              default = {},
            },
          },
        },
      },
    },
  },
}
