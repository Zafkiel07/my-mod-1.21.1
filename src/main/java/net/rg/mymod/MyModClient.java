package net.rg.mymod;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.util.Identifier;
import net.rg.mymod.item.ModItems;

public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register model predicate for pearl_launcher
        ModelPredicateProviderRegistry.register(
                ModItems.PEARL_LAUNCHER,
                Identifier.of("mymod", "pearls"),
                (stack, world, entity, seed) -> {
                    NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    int pearls = comp.copyNbt().getInt("Pearls");
                    return pearls / 4.0f; // Returns 0.0, 0.25, 0.5, 0.75, or 1.0
                }
        );
    }
}