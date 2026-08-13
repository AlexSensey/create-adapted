import { Ref } from "vue";
import { getFromCache, saveToCache } from "./cache";

export async function getVersionsForMinecraftVersions(
  supported_versions: Array<string>,
  ciData: Ref<Map<string, string>>
) {
  const cache: Map<string, string> = getFromCache(
    "getVersionsForMinecraftVersions"
  );
  if (cache != null && cache.size != 0) {
    ciData.value = cache;
    return;
  }

  let res = await fetch(
    "https://ci.createmod.net/job/createmod/job/Ponder/api/json"
  );
  if (res.ok) {
    let data = await res.json();

    for (const job of data.jobs) {
      const name = decodeURIComponent(job.name);
      const mc_version = name.replace("mc", "").replace("/dev", "");

      if (supported_versions.find((v) => v === mc_version) != null) {
        res = await fetch(job.url + "api/json");
        if (res.ok) {
          data = await res.json();

          res = await fetch(data.lastSuccessfulBuild.url + "api/json");
          if (res.ok) {
            data = await res.json();

            for (const artifact of data.artifacts) {
              let filename: string = artifact.fileName;
              let version = filename.match(
                /ponder-common-\d+\.\d+\.\d+-(\d+\.\d+\.\d+)\.jar/i
              );
              if (version != null) {
                ciData.value.set(mc_version, version[1]);
              }
            }
          }
        }
      }
    }
  } else {
    console.error("Failed to fetch CI data:", res.status, res.statusText);
  }

  saveToCache("getVersionsForMinecraftVersions", ciData.value, 1);
}
