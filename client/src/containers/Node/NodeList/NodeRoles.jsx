import React from 'react';
import PropTypes from 'prop-types';

// Badges describe positive discovery results, not the configured process.roles.
export function NodeRoles({ broker, controller }) {
  return (
    <span className="d-inline-flex flex-wrap align-items-center gap-1">
      {broker && <span className="badge text-bg-info">Broker</span>}
      {controller && <span className="badge text-bg-secondary">Controller</span>}
      {broker === undefined && (
        <span className="small text-body-secondary">Broker role unknown</span>
      )}
      {controller === undefined && (
        <span className="small text-body-secondary">Controller role unknown</span>
      )}
    </span>
  );
}

NodeRoles.propTypes = { broker: PropTypes.bool, controller: PropTypes.bool };

export function NodeRoleSummary({ brokerIds, quorum, loading }) {
  const controllerIds =
    quorum?.status === 'AVAILABLE' ? new Set(quorum.voters.map(voter => voter.id)) : null;
  const bothCount =
    brokerIds !== null && controllerIds !== null
      ? [...brokerIds].filter(id => controllerIds.has(id)).length
      : null;
  const count = value => value ?? (loading ? 'Loading…' : 'Unknown');
  return (
    <section className="mb-4" aria-label="Visible node roles">
      <div className="d-flex flex-wrap gap-3 mb-2">
        {[
          ['Brokers', brokerIds?.size],
          ['Controller voters', controllerIds?.size],
          ['Both roles', bothCount]
        ].map(([label, value]) => (
          <div className="border rounded px-3 py-2" key={label}>
            <span className="small text-body-secondary">{label}</span>{' '}
            <strong>{count(value)}</strong>
          </div>
        ))}
      </div>
      <p className="small text-body-secondary mb-0">
        Counts and badges show visible roles. A node with both roles appears in both tables.
      </p>
    </section>
  );
}

NodeRoleSummary.propTypes = {
  brokerIds: PropTypes.instanceOf(Set),
  quorum: PropTypes.shape({
    status: PropTypes.string.isRequired,
    voters: PropTypes.arrayOf(PropTypes.shape({ id: PropTypes.number.isRequired }))
  }),
  loading: PropTypes.bool.isRequired
};
