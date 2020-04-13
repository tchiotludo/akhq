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

export const uriGetDefinition = (clusterId, connectId, definitionId) => {
  // eslint-disable-next-line max-len
  return `${apiUrl}/connect/definition?clusterId=${clusterId}&connectId=${connectId}&definitionId=${definitionId}`;
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

export const uriDeleteGroups = () => {
  return `${apiUrl}/group/delete`;
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

export const uriConsumerGroups = (clusterId, view, search, pageNumber) => {
  // eslint-disable-next-line max-len
  return `${apiUrl}/group?clusterId=${clusterId}&view=${view}&search=${search}&pageNumber=${pageNumber}`;
};

export const uriConsumerGroupTopics = (clusterId, groupId) => {
  return `${apiUrl}/group/topics?clusterId=${clusterId}&groupId=${groupId}`;
};
export const uriConsumerGroupMembers = (clusterId, groupId) => {
  return `${apiUrl}/group/members?clusterId=${clusterId}&groupId=${groupId}`;
};

export const uriConsumerGroupGroupedTopicOffset = (clusterId, groupId, timestamp) => {
  let uri = `${apiUrl}/group/grouped-topic-offset?clusterId=${clusterId}&groupId=${groupId}`;

  if (timestamp !== '') {
    uri += `&timestamp=${timestamp}`;
  }

  return uri;
};

export const uriConsumerGroupUpdate = () => {
  return `${apiUrl}/group/update`;
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

export default {
  apiUrl,
  uriClusters,
  uriConnects,
  uriGetDefinition,
  uriPauseDefinition,
  uriResumeDefinition,
  uriRestartDefinition,
  uriRestartTask,
  uriConnectDefinitions,
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
  uriAclsByPrincipal,
  uriTopicsAcls
};
