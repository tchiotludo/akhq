const apiUrl = "localhost:8080/api";
const uriClusters = (id) => {return `${apiUrl}/clusters${id && '/' + id}`}
const uriConnects = (id) => {return `${apiUrl}/connects${id && '/' + id}`}
