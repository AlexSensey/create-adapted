---
supported_versions:
  - "1.20.1"
  - "1.21.1"
---

<script setup>
import { useData } from 'vitepress';
import { onMounted, ref } from "vue";
import { getVersionsForMinecraftVersions } from "../scripts/jenkins.ts";

const supported_versions = useData().frontmatter.value.supported_versions;

const ciData = ref(new Map());
supported_versions.forEach(version => ciData.value.set(version, "Loading..."));

onMounted(async () => await getVersionsForMinecraftVersions(supported_versions, ciData))
</script>

# Add Ponder to your project

---

To add Ponder to your project, you'll need to first add the required maven repository:

::: code-group

```groovy [build.gradle] {2}
repositories {
    maven { url = "https://maven.createmod.net" } // Ponder, Flywheel
}
```

```kotlin [build.gradle.kts] {2}
repositories {
    maven("https://maven.createmod.net") // Ponder, Flywheel
}
```

:::

Then you'll need to add the following to your `gradle.properties` file:

::: code-group

```properties-vue [1.21.1]
ponder_version = {{ ciData.get("1.21.1") }}
```

```properties-vue [26.1]
ponder_version = {{ ciData.get("26.1") }}
```

:::

After that, you'll need to add Ponder as a dependency, this varies based on how
your project is set up, but some common configurations are documented below:

::: code-group

```groovy [build.gradle] {3,6,9}
dependencies {
    // Fabric
    modImplementation(include("net.createmod.ponder:ponder-fabric:${ponder_version}+mc${minecraft_version}"))

    // NeoForge
    implementation(jarJar("net.createmod.ponder:ponder-neoforge:${ponder_version}+mc${minecraft_version}"))

    // Architectury/Jared's Multiloader Template
    implementation("net.createmod.ponder:ponder-common:${ponder_version}+mc${minecraft_version}")
}
```

```kotlin [build.gradle.kts] {3,6,9}
dependencies {
    // Fabric
    modImplementation(include("net.createmod.ponder:ponder-fabric:${ponder_version}+mc${minecraft_version}")!!)

    // NeoForge
    implementation(jarJar("net.createmod.ponder:ponder-neoforge:${ponder_version}+mc${minecraft_version}")!!)

    // Architectury/Jared's Multiloader Template
    implementation("net.createmod.ponder:ponder-common:${ponder_version}+mc${minecraft_version}")
}
```

:::
