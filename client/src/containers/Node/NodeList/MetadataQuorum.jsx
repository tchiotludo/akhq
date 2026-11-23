import React from 'react';
import PropTypes from 'prop-types';
import { NodeRoles } from './NodeRoles';

const statusMessages = {
  UNSUPPORTED: 'Metadata quorum queries are not supported by this cluster.',
  FORBIDDEN: 'The Kafka connection is not authorized to describe the metadata quorum.',
  TIMED_OUT: 'The metadata quorum query timed out. Refresh to try again.',
  UNAVAILABLE: 'Metadata quorum information could not be loaded. Refresh to try again.'
};

const valueOrUnknown = value => value ?? 'Unknown';
const timestamp = value => {
  if (value === null || value === undefined || value < 0) return 'Unknown';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Unknown' : date.toISOString();
};

export default function MetadataQuorum({ quorum, loading, brokerIds }) {
  // A hidden or inconsistent leader must not make every visible voter look inactive.
  const activeController =
    quorum?.status === 'AVAILABLE'
      ? quorum.voters.find(voter => voter.id === quorum.leaderId)
      : null;
  return (
    <section className="mt-4" aria-label="KRaft controllers">
      <h2>KRaft controllers</h2>
      {loading ? (
        <p role="status">Loading metadata quorum…</p>
      ) : quorum?.status !== 'AVAILABLE' ? (
        <p role="status">{statusMessages[quorum?.status] || statusMessages.UNAVAILABLE}</p>
      ) : (
        <>
          <p>
            Active controller: {valueOrUnknown(activeController?.id)} · Leader epoch:{' '}
            {valueOrUnknown(quorum.leaderEpoch)} · High watermark:{' '}
            {valueOrUnknown(quorum.highWatermark)}
          </p>
          <div className="table-responsive">
            <table className="table table-bordered table-striped">
              <thead>
                <tr>
                  {[
                    'Node ID',
                    'Observed roles',
                    'Controller status',
                    'Controller endpoints',
                    'Log end offset',
                    'Lag',
                    'Last fetch (UTC)'
                  ].map(label => (
                    <th key={label} scope="col">
                      {label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {quorum.voters.map(voter => (
                  <tr key={voter.id}>
                    <td>{voter.id}</td>
                    <td>
                      <NodeRoles broker={brokerIds?.has(voter.id)} controller={true} />
                    </td>
                    <td>
                      <span
                        className={`badge ${activeController?.id === voter.id ? 'text-bg-primary' : 'text-bg-secondary'}`}
                        title="Whether this voter is the current quorum leader; this is not a node health check."
                      >
                        {!activeController
                          ? 'Unknown'
                          : activeController.id === voter.id
                            ? 'Active'
                            : 'Not active'}
                      </span>
                    </td>
                    <td>{voter.endpoints.length ? voter.endpoints.join(', ') : 'Not reported'}</td>
                    <td>{valueOrUnknown(voter.logEndOffset)}</td>
                    <td>{valueOrUnknown(voter.lag)}</td>
                    <td>{timestamp(voter.lastFetchTimestamp)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {quorum.voters.length === 0 && <p>No controller voters are visible to this user.</p>}
          <p className="small text-body-secondary">
            Roles reflect broker discovery and quorum membership, not configured process roles or
            node health. Missing roles, endpoints or timestamps do not mean a node is down. Last
            updated: {timestamp(quorum.fetchedAt)}
          </p>
        </>
      )}
    </section>
  );
}

MetadataQuorum.propTypes = {
  loading: PropTypes.bool.isRequired,
  brokerIds: PropTypes.shape({ has: PropTypes.func.isRequired }),
  quorum: PropTypes.shape({
    status: PropTypes.string.isRequired,
    leaderId: PropTypes.number,
    leaderEpoch: PropTypes.number,
    highWatermark: PropTypes.number,
    fetchedAt: PropTypes.number,
    voters: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.number.isRequired,
        leader: PropTypes.bool.isRequired,
        endpoints: PropTypes.arrayOf(PropTypes.string).isRequired,
        logEndOffset: PropTypes.number,
        lag: PropTypes.number,
        lastFetchTimestamp: PropTypes.number
      })
    )
  })
};
