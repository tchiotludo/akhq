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

export default { apiUrl, uriClusters, uriConnects, uriNodes };
