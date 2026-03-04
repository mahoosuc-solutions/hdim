const Ajv = require('ajv');

const ajv = new Ajv({
  strict: false,
  removeAdditional: false,
  coerceTypes: false,
  allErrors: false
});

const compiledCache = new WeakMap();

function validateToolParams(schema, params) {
  if (!schema || !schema.properties) return null;
  let validate = compiledCache.get(schema);
  if (!validate) {
    validate = ajv.compile(schema);
    compiledCache.set(schema, validate);
  }
  const valid = validate(params ?? {});
  if (valid) return null;
  return validate.errors.map(e =>
    `${e.instancePath || '/'}: ${e.message}`
  ).join('; ');
}

module.exports = { validateToolParams };
