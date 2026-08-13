# Introduction

In order to give a scene more than a static view of the structure you build, a **Storyboard** has to be created.
Storyboards are simplified java code, which queue **Instructions** on a scene. Upon opening the Ponder UI the script is assigned to, and the instructions will then be executed in the order they were created in.

**TLDR;**

- Any Item can be assigned multiple Ponder **Scenes**. In-game, these scenes can be navigated with ponder UI.
- A **Scene** consists of a reference to a **Schematic File**, as well as a **Storyboard**
- **Storyboards** are java code that define an ordered list of **Instructions** to be carried out while the scene plays.
- A large amount of **Instructions** are provided by Ponder's API. Examples are `showSection`, `idle`, `rotateCameraY`, `showText`.

---

# Creating your first ponder

### Creating the scene schematic

To actually start with creating ponders you'll need to create a schematic, this can either be created with Create's **Schematic and Quill** item if you've got Create installed or with structure blocks.

Before making a schematic there's a few things you should note:

- The **Base Plate** of the scene has to be included in the structure. The base plate itself must be a square. If you'd like to place blocks on the same layer of the base plate you can configure the size of the base plate with `SceneBuilder#configureBasePlate` instruction.
- In the initial camera angle, the **Origin** of the schematics' coordinates is **closest** to the camera. The origin is the position with the lowest x and z coordinate.
  Editors can use `/tp @s ~ ~ ~ -35 25` to orient themselves approximately to the initial camera angle Ponder UI will display the schematic with.
  With this you could ensure that your schematic will not be shown from an awkward angle in the Scene.
- Schematics should try to contain all the blocks that will become visible during the scene. The **Storyboard** can control which sections of the schematics are visible at any point in time. It will be much more convenient for you to just show and hide parts of the schematic as opposed to actually replacing blocks in the virtual world of the Scene at runtime. Sometimes this is unavoidable, however, so the option still exists.

Once you've prepared your schematic and exported it with Create or a Structure Block, you'll want to move it to `src/main/resources/<mod_id>/<category>/<scene_name>.nbt`

Where `<category>` and `<scene_name>` are up to the editor to decide. The names are only relevant to keep things organized. Mind that inside Minecrafts resource system, only lowercase letters and underscores are allowed.
Something like `My Category/My Scene.nbt` should be better off as `my_category/my_scene.nbt`

::: tip NOTE

Changes to Schematic files require a **Resource Reload**. It can be triggered by either re-launching or doing a rebuild and then pressing **F3** + **T** in-game.

:::

### Registering the Storyboard

To register a storyboard you'll need to override `PonderPlugin#registerStoryboards` in your Ponder Plugin, You can find an example of this below:

```java
public class ExamplePonderPlugin implements PonderPlugin {
    [...]

   	@Override
  	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<Item> HELPER = helper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
    }
}
```

::: tip NOTE

Since ponder works with `Identifier`'s `PonderSceneRegistrationHelper#withKeyFunction` allows you to specify how ponder should convert the passed type to an Identifier,
in the above example that's done via `BuiltInRegistries#ITEM` and the `getKey` method which takes in the `Item` type and provides an `Identifier`

:::

After you've done that you can create your storyboard class, an example can be found below:

```java
public class ExampleStoryboard {
    public static void examplePonder(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("example", "Example Ponder");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
    }
}
```
