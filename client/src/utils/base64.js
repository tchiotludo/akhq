const bytesToBinaryString = bytes => {
  let binaryString = '';
  bytes.forEach(byte => {
    binaryString += String.fromCharCode(byte);
  });
  return binaryString;
};

export const encodeUtf8ToBase64 = value => {
  return btoa(bytesToBinaryString(new TextEncoder().encode(value)));
};

export const decodeUtf8FromBase64 = value => {
  const binaryString = atob(value);
  const bytes = Uint8Array.from(binaryString, character => character.charCodeAt(0));
  return new TextDecoder().decode(bytes);
};

export const encodeBase64PathSegment = value => encodeURIComponent(value);

export const decodeBase64PathSegment = value => decodeURIComponent(value);
