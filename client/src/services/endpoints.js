export const apiUrl = 'http://localhost:8080/api';
export const baseUrl = 'http://localhost:8080';

export const uriClusters = id => {
  return `${apiUrl}/clusters${id ? '?id=' + id : ''}`;
};

export const uriConnects = id => {
  console.log(id);
  return `${apiUrl}/connects${id ? '?clusterId=' + id : ''}`;
};

export default { apiUrl, uriClusters, uriConnects, baseUrl };
