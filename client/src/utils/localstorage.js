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
