import { node } from 'prop-types';

export const baseUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}`;
export const apiUrl = `${baseUrl}/api`;

export const uriClusters = id => {
  return `${apiUrl}/clusters${id ? '?clusterId=' + id : ''}`;
};

export const uriConnects = id => {
  return `${apiUrl}/connects${id ? '?clusterId=' + id : ''}`;
};

export const uriConnectDefinitions = (clusterId, connectId) => {
  return `${apiUrl}/connect/definitions?clusterId=${clusterId}&connectId=${connectId}`;
};

export const uriConnectPlugins = (clusterId, connectId) => {
  return `${apiUrl}/connect/plugins?clusterId=${clusterId}&connectId=${connectId}`;
};

export const uriGetDefinition = (clusterId, connectId, definitionId) => {
  // eslint-disable-next-line max-len
  return `${apiUrl}/connect/definition?clusterId=${clusterId}&connectId=${connectId}&definitionId=${definitionId}`;
};

export const uriConnectDefinitionConfigs = (clusterId, connectId, definitionId) => {
  // eslint-disable-next-line max-len
  return `${apiUrl}/connect/definition/configs?clusterId=${clusterId}&connectId=${connectId}&definitionId=${definitionId}`;
};

export const uriUpdateDefinition = () => {
  return `${apiUrl}/connect/definition/update`;
};

export const uriPauseDefinition = () => {
  return `${apiUrl}/connect/definition/pause`;
};

export const uriResumeDefinition = () => {
  return `${apiUrl}/connect/definition/resume`;
};

export const uriRestartDefinition = () => {
  return `${apiUrl}/connect/definition/restart`;
};

export const uriRestartTask = () => {
  return `${apiUrl}/connect/definition/task/restart`;
};

export const uriDeleteDefinition = () => {
  return `${apiUrl}/connect/delete`;
};

export const uriSchemaRegistry = (id, search, pageNumber) => {
  return `${apiUrl}/schema?clusterId=${id}&search=${search}&pageNumber=${pageNumber}`;
};
export const uriSchemaVersions = (clusterId, subject) => {
  return `${apiUrl}/schema/versions?clusterId=${clusterId}&subject=${subject}`;
};

export const uriDeleteSchema = () => {
  return `${apiUrl}/schema/delete`;
};

export const uriDeleteSchemaVersion = () => {
  return `${apiUrl}/schema/version`;
};
export const uriTopics = (id, view, search, pageNumber) => {
  return `${apiUrl}/topics?clusterId=${id}&view=${view}&search=${search}&pageNumber=${pageNumber}`;
};

export const uriTopicsCreate = () => `${apiUrl}/topic/create`;

export const uriTopicsProduce = () => `${apiUrl}/topic/produce`;

export const uriDeleteTopics = () => {
  return `${apiUrl}/topic/delete`;
};

export const uriTopicData = (clusterId, topicId, sort, partition, timestamp, search, offsets) => {
  let uri = `${apiUrl}/topic/data?clusterId=${clusterId}&topicId=${topicId}`;

  if (sort !== undefined) {
    uri += `&sort=${sort}`;
  }
  if (partition !== undefined) {
    uri += `&partition=${partition}`;
  }
  if (timestamp !== undefined) {
    uri += `&timestamp=${timestamp}`;
  }
  if (search !== undefined) {
    uri += `&search=${search}`;
  }
  if (offsets !== undefined) {
    uri += `&offsets=${offsets}`;
  }

  return uri;
};

export const uriTopicsPartitions = (clusterId, topicId) => {
  return (
    `${apiUrl}/topic/partitions${clusterId ? '?clusterId=' + clusterId : ''}` +
    `${topicId ? '&topicId=' + topicId : ''}`
  );
};

export const uriTopicsGroups = (clusterId, topicId) => {
  return `${apiUrl}/topic/groups?clusterId=${clusterId}&topicId=${topicId}`;
};
export const uriTopicsLogs = (clusterId, topicId) => {
  return (
    `${apiUrl}/topic/logs${clusterId ? '?clusterId=' + clusterId : ''}` +
    `${topicId ? '&topicId=' + topicId : ''}`
  );
};

export const uriNodes = id => {
  return `${apiUrl}/${id}/node`;
};

export const uriNodesConfigs = (clusterId, nodeId) => {
  return `${apiUrl}/${clusterId}/node/${nodeId}/configs`;
};

export const uriNodesUpdateConfigs = (clusterId, nodeId) => {
  return `${apiUrl}/${clusterId}/node/${nodeId}/configs`;
};

export const uriNodesLogs = (clusterId, nodeId) => {
  return `${apiUrl}/${clusterId}/node/${nodeId}/logs`;
};

export const uriConsumerGroups = (clusterId, search, pageNumber) => {
  // eslint-disable-next-line max-len
  return `${apiUrl}/${clusterId}/group?search=${search}&pageNumber=${pageNumber}`;
};

export const uriConsumerGroup = (clusterId, groupId) => {
  return `${apiUrl}/${clusterId}/group/${groupId}`;
};

export const uriConsumerGroupTopics = (clusterId, groupId) => {
  return `${apiUrl}/group/topics?clusterId=${clusterId}&groupId=${groupId}`;
};
export const uriConsumerGroupMembers = (clusterId, groupId) => {
  return `${apiUrl}/${clusterId}/group/${groupId}/members`;
};

export const uriConsumerGroupOffsets = (clusterId, groupId) => {
  return `${apiUrl}/${clusterId}/group/${groupId}/offsets`;
};

export const uriConsumerGroupOffsetsByTimestamp = (clusterId, groupId, timestamp) => {
  return `${apiUrl}/${clusterId}/group/${groupId}/offsets/start?timestamp=${timestamp}`;
};

export const uriConsumerGroupGroupedTopicOffset = (clusterId, groupId, timestamp) => {
  let uri = `${apiUrl}/group/grouped-topic-offset?clusterId=${clusterId}&groupId=${groupId}`;

  if (timestamp !== '') {
    uri += `&timestamp=${timestamp}`;
  }

  return uri;
};

export const uriConsumerGroupDelete = (clusterId, groupId) => {
  return `${apiUrl}/${clusterId}/group/${groupId}`;
};

export const uriConsumerGroupUpdate = (clusterId, groupId) => {
  return `${apiUrl}/${clusterId}/group/${groupId}/offsets`;
};

export const uriTopicsConfigs = (clusterId, topicId) => {
  return (
    `${apiUrl}/cluster/topic/configs${clusterId ? '?clusterId=' + clusterId : ''}` +
    `${topicId ? '&topicId=' + topicId : ''}`
  );
};

export const uriTopicsUpdateConfigs = () => {
  return `${apiUrl}/cluster/topic/update-configs`;
};

export const uriLatestSchemaVersion = (clusterId, subject) => {
  return `${apiUrl}/schema/version?clusterId=${clusterId}&subject=${subject}`;
};

export const uriUpdateSchema = () => {
  return `${apiUrl}/schema/update`;
};

export const uriSchemaCreate = () => {
  return `${apiUrl}/schema/create`;
};

export const uriAclsList = (clusterId, search) => {
  let url = `${apiUrl}/${clusterId}/acls`;
  return search ? `${url}?search=${search}` : url;
};

export const uriTopicsAcls = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/acls`;
};

export const uriConsumerGroupAcls = (clusterId, groupId) => {
  return `${apiUrl}/${clusterId}/group/${groupId}/acls`;
};

export const uriAclsByPrincipal = (clusterId, principalEncoded, resourceType = 'ANY') => {
  return `${apiUrl}/${clusterId}/acls/${principalEncoded}?resourceType=${resourceType}`;
};

export const uriCreateConnect = () => {
  return `${apiUrl}/connect/definition/create`;
};

export default {
  apiUrl,
  uriClusters,
  uriConnects,
  uriCreateConnect,
  uriConnectPlugins,
  uriGetDefinition,
  uriUpdateDefinition,
  uriPauseDefinition,
  uriResumeDefinition,
  uriRestartDefinition,
  uriRestartTask,
  uriConnectDefinitions,
  uriConnectDefinitionConfigs,
  uriNodes,
  uriNodesConfigs,
  uriTopicsLogs,
  uriTopicsGroups,
  uriTopicsPartitions,
  uriTopicData,
  uriTopicsProduce,
  uriTopicsCreate,
  uriTopics,
  uriDeleteTopics,
  uriNodesLogs,
  uriConsumerGroups,
  uriConsumerGroupTopics,
  uriConsumerGroupMembers,
  uriSchemaRegistry,
  uriDeleteSchema,
  uriSchemaCreate,
  uriConsumerGroupGroupedTopicOffset,
  uriConsumerGroupUpdate,
  uriTopicsConfigs,
  uriLatestSchemaVersion,
  uriSchemaVersions,
  uriAclsList,
  uriAclsByPrincipal
};
