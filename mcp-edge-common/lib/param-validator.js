const Ajv = require('ajv');

const ajv = new Ajv({
  strict: false,
  removeAdditional: false,
  coerceTypes: false,
  allErrors: false
});

function validateToolParams(schema, params) {
  if (!schema || !schema.properties) return null;
  const validate = ajv.compile(schema);
  const valid = validate(params ?? {});
  if (valid) return null;
  return validate.errors.map(e =>
    `${e.instancePath || '/'}: ${e.message}`
  ).join('; ');
}

module.exports = { validateToolParams };
