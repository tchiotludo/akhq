import { node } from 'prop-types';

export const apiUrl = 'http://localhost:8080/api';
export const baseUrl = 'http://localhost:8080';

export const uriClusters = id => {
  return `${apiUrl}/clusters${id ? '?clusterId=' + id : ''}`;
};

export const uriConnects = id => {
  return `${apiUrl}/connects${id ? '?clusterId=' + id : ''}`;
};

export const uriNodes = id => {
  return `${apiUrl}/cluster/nodes${id ? '?clusterId=' + id : ''}`;
};

export const uriTopics = (id, view, search) => {
  return `${apiUrl}/${
    search
      ? 'topics?clusterId=' + id + '&view=' + view + '&search=' + search
      : 'topics?clusterId=' + id + '&view=' + view
  } `;
};

export const uriTopicsCreate = () => `${apiUrl}/topic/create`;

export const uriTopicsPartitions = (clusterId, topicId) => {
  return (
    `${apiUrl}/topic/partitions${clusterId ? '?clusterId=' + clusterId : ''}` +
    `${topicId ? '&topicId=' + topicId : ''}`
  );
};

export const uriNodesConfigs = (clusterId, nodeId) => {
  return (
    `${apiUrl}/cluster/nodes/configs${clusterId ? '?clusterId=' + clusterId : ''}` +
    `${nodeId ? '&nodeId=' + nodeId : ''}`
  );
};

export const uriNodesUpdateConfigs = () => {
  return `${apiUrl}/cluster/nodes/update-configs`;
};

export const uriNodesLogs = (clusterId, nodeId) => {
  return (
    `${apiUrl}/cluster/nodes/logs${clusterId ? '?clusterId=' + clusterId : ''}` +
    `${nodeId ? '&nodeId=' + nodeId : ''}`
  );
};

export default { apiUrl, uriClusters, uriConnects, uriNodes, uriNodesConfigs, uriTopics };
