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

export const uriTopics = (id, view, search, pageNumber) => {
  return `${apiUrl}/topics?clusterId=${id}&view=${view}&search=${search}&pageNumber=${pageNumber}`;
};

export const uriTopicsCreate = () => `${apiUrl}/topic/create`;
export const uriTopicsProduce = () => `${apiUrl}/topic/produce`;

export const uriTopicsPartitions = (clusterId, topicId) => {
  return (
    `${apiUrl}/topic/partitions${clusterId ? '?clusterId=' + clusterId : ''}` +
    `${topicId ? '&topicId=' + topicId : ''}`
  );
};

export const uriTopicsLogs = (clusterId, topicId) => {
  return (
    `${apiUrl}/topic/logs${clusterId ? '?clusterId=' + clusterId : ''}` +
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

export const uriDeleteTopics = () => {
  return `${apiUrl}/topic/delete`;
};

export const uriConsumerGroups = (id, view, search, pageNumber) => {
  return `${apiUrl}/group?clusterId=${id}&view=${view}&search=${search}&pageNumber=${pageNumber}`;
};

export const uriConsumerGroupTopics = (id, groupId) => {
  return `${apiUrl}/group/topics?clusterId=${id}&groupId=${groupId}`;
};
export const uriConsumerGroupMembers = (id, groupId) => {
  return `${apiUrl}/group/members?clusterId=${id}&groupId=${groupId}`;
};

export default {
  apiUrl,
  uriClusters,
  uriConnects,
  uriNodes,
  uriNodesConfigs,
  uriTopics,
  uriTopicsLogs,
  uriConsumerGroups,
  uriConsumerGroupTopics,
  uriConsumerGroupMembers,
  uriDeleteTopics,
  uriTopicsProduce
};
