// Please, comment the localhost one before PR to dev

export const baseUrl = process.env.REACT_APP_BASE_URL ||
     `${window.location.protocol}//${window.location.hostname}:${window.location.port}`;

//export const baseUrl = 'http://localhost:8081';

export const apiUrl = `${baseUrl}/api`;

export const uriLogin = () => {
  return `${baseUrl}/login`;
};

export const uriLogout = () => {
  return `${baseUrl}/logout`;
};

export const uriCurrentUser = () => {
  return `${apiUrl}/me`;
};

export const uriClusters = () => {
  return `${apiUrl}/cluster`;
};

export const uriTopics = (clusterId, search, show, page) => {
  return `${apiUrl}/${clusterId}/topic?search=${search}&show=${show}&page=${page}`;
};

export const uriTopicsCreate = clusterId => `${apiUrl}/${clusterId}/topic`;

export const uriTopicsProduce = (clusterId, topicName) =>
  `${apiUrl}/${clusterId}/topic/${topicName}/data`;

export const uriDeleteTopics = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}`;
};

export const uriTopicData = (
  clusterId,
  topicId,
  offset,
  partition,
  sort,
  timestamp,
  search,
  nextPage = ''
) => {
  if (nextPage !== '') {
    return baseUrl + nextPage;
  }

  let uri = `${apiUrl}/${clusterId}/topic/${topicId}/data?sort=${sort}`;
  if (offset !== undefined) {
    uri += `&after=${offset}`;
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
  return uri;
};

export const uriTopicsPartitions = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/partitions`;
};

export const uriTopicsGroups = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/groups`;
};
export const uriTopicsLogs = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/logs`;
};
export const uriTopicsConfigs = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/configs`;
};
export const uriTopicsAcls = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/acls`;
};

export const uriTopicsUpdateConfigs = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/configs`;
};

export const uriConnects = id => {
  return `${apiUrl}/connects${id ? '?clusterId=' + id : ''}`;
};

export const uriConnectDefinitions = (clusterId, connectId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}`;
};

export const uriConnectPlugins = (clusterId, connectId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/plugins`;
};

export const uriConnectPlugin = (clusterId, connectId, pluginId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/plugins/${pluginId}`;
};

export const uriCreateConnect = (clusterId, connectId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}`;
};

export const uriGetDefinition = (clusterId, connectId, definitionId) => {
  // eslint-disable-next-line max-len
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}`;
};

export const uriConnectDefinitionConfigs = (clusterId, connectId, definitionId) => {
  // eslint-disable-next-line max-len
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}/configs`;
};

export const uriUpdateDefinition = (clusterId, connectId, definitionId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}/configs`;
};

export const uriPauseDefinition = (clusterId, connectId, definitionId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}/pause`;
};

export const uriResumeDefinition = (clusterId, connectId, definitionId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}/resume`;
};

export const uriRestartDefinition = (clusterId, connectId, definitionId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}/restart`;
};

export const uriRestartTask = (clusterId, connectId, definitionId, taskId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}/tasks/${taskId}/restart`;
};

export const uriDeleteDefinition = (clusterId, connectId, definitionId) => {
  return `${apiUrl}/${clusterId}/connect/${connectId}/${definitionId}`;
};

export const uriSchemaRegistry = (clusterId, search, pageNumber) => {
  return `${apiUrl}/${clusterId}/schema?&search=${search}&page=${pageNumber}`;
};
export const uriSchemaVersions = (clusterId, subject) => {
  return `${apiUrl}/${clusterId}/schema/${subject}/version`;
};

export const uriDeleteSchema = (clusterId, subject) => {
  return `${apiUrl}/${clusterId}/schema/${subject}`;
};

export const uriPreferredSchemaForTopic = (clusterId, topicId) => {
  return `${apiUrl}/${clusterId}/schema/topic/${topicId}`;
};

export const uriDeleteSchemaVersion = (clusterId, subject, version) => {
  return `${apiUrl}/${clusterId}/schema/${subject}/version/${version}`;
};

export const uriLatestSchemaVersion = (clusterId, subject) => {
  return `${apiUrl}/${clusterId}/schema/${subject}`;
};

export const uriUpdateSchema = (clusterId, subject) => {
  return `${apiUrl}/${clusterId}/schema/${subject}`;
};

export const uriSchemaCreate = clusterId => {
  return `${apiUrl}/${clusterId}/schema`;
};

export const uriDeleteGroups = () => {
  return `${apiUrl}/group/delete`;
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

export const uriAclsList = (clusterId, search) => {
  let url = `${apiUrl}/${clusterId}/acls`;
  return search ? `${url}?search=${search}` : url;
};

export const uriConsumerGroupAcls = (clusterId, groupId) => {
  return `${apiUrl}/${clusterId}/group/${groupId}/acls`;
};

export const uriAclsByPrincipal = (clusterId, principalEncoded, resourceType = 'ANY') => {
  return `${apiUrl}/${clusterId}/acls/${principalEncoded}?resourceType=${resourceType}`;
};

export const uriLiveTail = (clusterId, search, topics, size) => {
  let searchUrl = `search=${search}`;
  let topicsUrl = search.length > 0 ? '&' : '';
  topics.forEach((topic, index) => {
    if (index > 0) {
      topicsUrl += '&topics=' + topic;
    } else {
      topicsUrl += 'topics=' + topic;
    }
  });

  let sizeUrl = `${search.length > 0 || topics.length > 0 ? '&' : ''}size=${size}`;
  return `${apiUrl}/${clusterId}/tail/sse?${search.length > 0 ? searchUrl : ''}${
    topics.length > 0 ? topicsUrl : ''
  }${size.length > 0 ? sizeUrl : ''}`;
};

export const uriTopicDataSearch = (clusterId, topicId, search) => {
  return `${apiUrl}/${clusterId}/topic/${topicId}/data/search/${search}`;
};

export const uriTopicDataDelete = (clusterId, topicName, partition, key) => {
  return `${apiUrl}/${clusterId}/topic/${topicName}/data?partition=${partition}&key=${key}`;
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
  uriPreferredSchemaForTopic,
  uriSchemaCreate,
  uriConsumerGroupGroupedTopicOffset,
  uriConsumerGroupUpdate,
  uriTopicsConfigs,
  uriLatestSchemaVersion,
  uriSchemaVersions,
  uriAclsList,
  uriAclsByPrincipal,
  uriLiveTail,
  uriTopicDataSearch,
  uriTopicDataDelete
};
