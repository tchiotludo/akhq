const apiUrl = 'http://localhost:8080/api';

const uriClusters = id => {
  return `${apiUrl}/clusters${id ? '?id=' + id : ''}`;
};

const uriConnects = id => {
  console.log(id);
  return `${apiUrl}/connects${id ? '?clusterId=' + id : ''}`;
};

export default {apiUrl, uriClusters, uriConnects};
