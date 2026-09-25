export const getUIOptions = cluster => {
  const uiOptions = localStorage.getItem('uiOptions');
  if (uiOptions !== null) {
    const objParsed = JSON.parse(uiOptions);
    return objParsed[cluster];
  } else {
    return null;
  }
};

export const setUIOptions = (cluster, newUIOptions) => {
  const uiOptions = localStorage.getItem('uiOptions');
  if (uiOptions !== null) {
    const objParsed = JSON.parse(uiOptions);
    objParsed[cluster] = newUIOptions;
    localStorage.setItem('uiOptions', JSON.stringify(objParsed));
  } else {
    localStorage.setItem('uiOptions', JSON.stringify({ [cluster]: newUIOptions }));
  }
};

export const popProduceToTopicValues = () => {
  const produceToTopicValues = localStorage.getItem('produceToTopicValues');
  localStorage.removeItem('produceToTopicValues');
  return produceToTopicValues !== null ? JSON.parse(produceToTopicValues) : {};
};

export const setProduceToTopicValues = newProduceToTopicValues => {
  localStorage.setItem('produceToTopicValues', JSON.stringify(newProduceToTopicValues));
};

const TOPIC_FAVORITES_KEY = 'topicFavorites';
export const MAX_TOPIC_FAVORITES = 100;

const normalizeTopicFavorites = favorites => {
  if (!Array.isArray(favorites)) {
    return [];
  }

  return [...new Set(favorites.filter(favorite => typeof favorite === 'string' && favorite.length > 0))]
    .slice(0, MAX_TOPIC_FAVORITES);
};

const getTopicFavoritesByCluster = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(TOPIC_FAVORITES_KEY));
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
};

export const getTopicFavorites = cluster => {
  return normalizeTopicFavorites(getTopicFavoritesByCluster()[cluster]);
};

export const toggleTopicFavorite = (cluster, topic) => {
  const favoritesByCluster = getTopicFavoritesByCluster();
  const favorites = getTopicFavorites(cluster);
  if (typeof topic !== 'string' || topic.length === 0) {
    return favorites;
  }
  const nextFavorites = favorites.includes(topic)
    ? favorites.filter(favorite => favorite !== topic)
    : favorites.length < MAX_TOPIC_FAVORITES
      ? favorites.concat(topic)
      : favorites;

  localStorage.setItem(
    TOPIC_FAVORITES_KEY,
    JSON.stringify({
      ...favoritesByCluster,
      [cluster]: nextFavorites
    })
  );

  return nextFavorites;
};
