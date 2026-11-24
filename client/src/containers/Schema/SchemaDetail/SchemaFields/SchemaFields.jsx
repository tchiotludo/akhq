import React, { useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { extractAvroFields, MAX_FIELDS } from '../../../../utils/avroFields';

const latestVersion = schemas =>
  schemas.reduce((latest, schema) => (schema.version > latest ? schema.version : latest), 0);

const recordDoc = definition => {
  try {
    const parsed = JSON.parse(definition);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed.doc : undefined;
  } catch (e) {
    return undefined;
  }
};

const SchemaFields = ({ schemas }) => {
  const versions = schemas || [];
  const [selectedVersion, setSelectedVersion] = useState(() => latestVersion(versions));
  const [filter, setFilter] = useState('');

  const schema =
    versions.find(version => version.version === selectedVersion) ??
    versions.find(version => version.version === latestVersion(versions));
  // A version the server could not parse comes back without schema and schemaType.
  const failed = schema && (schema.exception || !schema.schema);
  const isAvro = schema && !failed && (!schema.schemaType || schema.schemaType === 'AVRO');
  const fields = useMemo(() => (isAvro ? extractAvroFields(schema.schema) : []), [isAvro, schema]);

  if (!schema) {
    return null;
  }

  const needle = filter.trim().toLowerCase();
  const visibleFields = needle
    ? fields.filter(
        field =>
          field.path.toLowerCase().includes(needle) || field.doc.toLowerCase().includes(needle)
      )
    : fields;
  const documentedCount = fields.filter(field => field.doc).length;
  const description = isAvro ? recordDoc(schema.schema) : undefined;

  return (
    <div className="schema-fields">
      <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
        <label htmlFor="schema-fields-version" className="mb-0">
          Version
        </label>
        <select
          id="schema-fields-version"
          className="form-select w-auto"
          value={schema.version}
          onChange={event => setSelectedVersion(Number(event.target.value))}
        >
          {[...versions]
            .sort((a, b) => b.version - a.version)
            .map(version => (
              <option key={version.version} value={version.version}>
                {version.version} (id {version.id})
              </option>
            ))}
        </select>
        {isAvro && (
          <>
            <input
              type="search"
              className="form-control w-auto"
              placeholder="Filter fields"
              aria-label="Filter fields"
              value={filter}
              onChange={event => setFilter(event.target.value)}
            />
            <span className="text-muted">
              {documentedCount} / {fields.length} fields documented
            </span>
          </>
        )}
      </div>

      {failed && (
        <div className="alert alert-warning" role="alert">
          This version could not be parsed{schema.exception ? `: ${schema.exception}` : '.'}
        </div>
      )}

      {!failed && !isAvro && (
        <div className="alert alert-info" role="alert">
          Field descriptions are only available for Avro schemas.
        </div>
      )}

      {description && <p className="schema-fields-description">{description}</p>}

      {isAvro && fields.length >= MAX_FIELDS && (
        <div className="alert alert-warning" role="alert">
          Only the first {MAX_FIELDS} fields are listed.
        </div>
      )}

      {isAvro && (
        <div className="table-responsive">
          <table className="table table-sm table-bordered">
            <thead>
              <tr>
                <th>Field</th>
                <th>Type</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {visibleFields.map((field, index) => (
                <tr key={`${field.path}-${index}`}>
                  <td>
                    <code>{field.path}</code>
                  </td>
                  <td>
                    <code>{field.type}</code>
                  </td>
                  <td>{field.doc}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

SchemaFields.propTypes = {
  schemas: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number,
      version: PropTypes.number,
      schemaType: PropTypes.string,
      schema: PropTypes.string
    })
  )
};

export default SchemaFields;
