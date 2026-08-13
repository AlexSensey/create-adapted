# Registering a Ponder Plugin

To start off with using ponder you'll first need to register a ponder plugin, and for this you'll need to create a new class that implements the `PonderPlugin` interface. You will need to override the `getModId` method and return your mods id inside of that.

```java
public class ExamplePonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return "example_mod";
    }
}
```

After you've done that you'll need to actually register the Ponder Plugin, and for that you'll need to call `PonderIndex.addPlugin(new ExamplePonderPlugin());` in your mods initializer. An example of this can be found below:

::: code-group

```java [NeoForge 1.21.1] {4}
@Mod(value = "example_mod", dist = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient() {
        PonderIndex.addPlugin(new ExamplePonderPlugin());
    }
}
```

```java [Forge 1.20.1] {4-5}
@Mod("example_mod")
public class ExampleMod {
    public ExampleModClient() {
        if (FMLLoader.getDist() == Dist.CLIENT)
            PonderIndex.addPlugin(new ExamplePonderPlugin());
    }
}
```

```java [Fabric] {4}
public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PonderIndex.addPlugin(new ExamplePonderPlugin());
    }
}
```

:::

Now you've got a Ponder Plugin that can be used to register ponder scenes, ponder tags, and shared ponder text!
