package net.rg.mymod.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.rg.mymod.MyMod;
import net.rg.mymod.item.custom.PearlLauncherItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.rg.mymod.item.custom.PearlLauncherItem;

public class ModItems {

    public static final Item PEARL_LAUNCHER = registerItem("pearl_launcher", new PearlLauncherItem(new Item.Settings().maxDamage(260)));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MyMod.MOD_ID, name), item);
    }

    public static void registerModItems() {
        MyMod.LOGGER.info("Registering Mod Items for " + MyMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(PEARL_LAUNCHER);
        });
    }
}