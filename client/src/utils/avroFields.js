const PRIMITIVES = new Set([
  'null',
  'boolean',
  'int',
  'long',
  'float',
  'double',
  'bytes',
  'string'
]);
const NAMED = new Set(['record', 'error', 'enum', 'fixed']);

const fullName = (name, namespace) => {
  if (name.includes('.') || !namespace) {
    return name;
  }
  return `${namespace}.${name}`;
};

const shortName = name => name.substring(name.lastIndexOf('.') + 1);

const namespaceOf = (schema, enclosing) => {
  if (schema.name && schema.name.includes('.')) {
    return schema.name.substring(0, schema.name.lastIndexOf('.'));
  }
  return schema.namespace ?? enclosing;
};

// Each named type keeps the namespace it was defined in: short names inside it resolve against
// that namespace, not against the namespace of the field that references it.
const registerNamedTypes = (schema, namespace, registry) => {
  if (Array.isArray(schema)) {
    schema.forEach(branch => registerNamedTypes(branch, namespace, registry));
    return;
  }
  if (!schema || typeof schema !== 'object') {
    return;
  }

  if (NAMED.has(schema.type) && schema.name) {
    const ns = namespaceOf(schema, namespace);
    const name = fullName(schema.name, ns);
    if (registry.has(name)) {
      return;
    }
    registry.set(name, { schema, namespace: ns, name });
    (schema.fields || []).forEach(field => registerNamedTypes(field.type, ns, registry));
    return;
  }

  if (schema.type === 'array') {
    registerNamedTypes(schema.items, namespace, registry);
  } else if (schema.type === 'map') {
    registerNamedTypes(schema.values, namespace, registry);
  }
};

const resolve = (schema, namespace, registry) => {
  if (typeof schema === 'string') {
    if (PRIMITIVES.has(schema)) {
      return { schema, namespace };
    }
    return (
      registry.get(fullName(schema, namespace)) ??
      registry.get(schema) ?? { schema, namespace, name: schema }
    );
  }
  if (schema && typeof schema === 'object' && NAMED.has(schema.type) && schema.name) {
    const ns = namespaceOf(schema, namespace);
    return { schema, namespace: ns, name: fullName(schema.name, ns) };
  }
  return { schema, namespace };
};

const isRecord = schema =>
  !!schema && typeof schema === 'object' && (schema.type === 'record' || schema.type === 'error');

const typeLabel = (schema, namespace, registry) => {
  if (Array.isArray(schema)) {
    return schema.map(branch => typeLabel(branch, namespace, registry)).join(' | ');
  }
  if (typeof schema === 'string') {
    return PRIMITIVES.has(schema) ? schema : shortName(schema);
  }
  if (!schema || typeof schema !== 'object') {
    return String(schema);
  }
  const logical = schema.logicalType ? ` (${schema.logicalType})` : '';
  if (NAMED.has(schema.type)) {
    return `${shortName(schema.name)}${logical}`;
  }
  if (schema.type === 'array') {
    return `array<${typeLabel(schema.items, namespace, registry)}>`;
  }
  if (schema.type === 'map') {
    return `map<${typeLabel(schema.values, namespace, registry)}>`;
  }
  return `${typeLabel(schema.type, namespace, registry)}${logical}`;
};

/** Upper bound on listed fields: types shared at every level would otherwise grow exponentially. */
export const MAX_FIELDS = 5000;

const collect = (schema, namespace, path, registry, visiting, result) => {
  if (result.length >= MAX_FIELDS) {
    return;
  }

  if (Array.isArray(schema)) {
    const records = schema.filter(branch => isRecord(resolve(branch, namespace, registry).schema));
    schema.forEach(branch => {
      const { schema: resolved } = resolve(branch, namespace, registry);
      // With several record branches, the same field name can exist in more than one of them.
      const branchPath =
        records.length > 1 && isRecord(resolved) ? `${path}<${shortName(resolved.name)}>` : path;
      collect(branch, namespace, branchPath, registry, visiting, result);
    });
    return;
  }

  const { schema: resolved, namespace: ns, name } = resolve(schema, namespace, registry);
  if (!resolved || typeof resolved !== 'object') {
    return;
  }

  if (resolved.type === 'array') {
    collect(resolved.items, ns, `${path}[]`, registry, visiting, result);
    return;
  }
  if (resolved.type === 'map') {
    collect(resolved.values, ns, `${path}{}`, registry, visiting, result);
    return;
  }
  if (!isRecord(resolved)) {
    return;
  }

  // Recursive types (e.g. a linked list) would otherwise expand forever.
  if (visiting.has(name)) {
    return;
  }
  visiting.add(name);

  for (const field of resolved.fields || []) {
    if (result.length >= MAX_FIELDS) {
      break;
    }
    const fieldPath = path ? `${path}.${field.name}` : field.name;
    result.push({
      path: fieldPath,
      type: typeLabel(field.type, ns, registry),
      doc: field.doc ?? ''
    });
    collect(field.type, ns, fieldPath, registry, visiting, result);
  }

  visiting.delete(name);
};

/**
 * Lists every field of an Avro schema, nested ones included, as `{ path, type, doc }` in
 * declaration order, up to MAX_FIELDS entries. Array items are addressed with `[]`, map values
 * with `{}`, and a record branch of a union holding several records with `<Name>`.
 */
export const extractAvroFields = schemaDefinition => {
  let schema;
  try {
    schema = typeof schemaDefinition === 'string' ? JSON.parse(schemaDefinition) : schemaDefinition;
  } catch (e) {
    return [];
  }

  const registry = new Map();
  registerNamedTypes(schema, undefined, registry);

  const result = [];
  collect(schema, undefined, '', registry, new Set(), result);
  return result;
};
