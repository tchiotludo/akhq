import axios from "axios";

async function get(url) {
  const response = await axios.get(url);
  return response;
}

async function put(url) {}

async function post(url) {}

async function remove(url, id) {
  const response = await axios.delete(url + "/" + id);
  return response;
}

export default {get, put, post, remove};
