// Implementation of a small cache with localstorage

export function saveToCache(
  name: string,
  object: any,
  expireInHoursFromNow: number
) {
  const expiry = new Date(Date.now() + expireInHoursFromNow * (60 * 60 * 1000));

  localStorage.setItem(
    name,
    JSON.stringify({ expiry: expiry, data: object }, jsonEncoder)
  );
}

export function getFromCache(name: string): any {
  const data = localStorage.getItem(name);
  if (data != null) {
    const parsed = JSON.parse(data, jsonDecoder);

    if (parsed.expiry < Date.now()) {
      return null;
    } else {
      return parsed.data;
    }
  }
}

function jsonEncoder(key, value) {
  if (value instanceof Map) {
    return {
      dataType: "Map",
      value: Array.from(value.entries()), // or with spread: value: [...value]
    };
  } else {
    return value;
  }
}

function jsonDecoder(key, value) {
  if (typeof value === "object" && value !== null) {
    if (value.dataType === "Map") {
      return new Map(value.value);
    }
  }
  return value;
}
