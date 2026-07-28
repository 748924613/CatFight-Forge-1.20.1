package com.catfight.item;

import com.catfight.CatFightMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CatFightMod.MOD_ID);

    public static final RegistryObject<Item> TRAINING_STICK = ITEMS.register("training_stick",
            () -> new TrainingStickItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DRIED_FISH = ITEMS.register("dried_fish",
            () -> new DriedFishItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SUPER_DRIED_FISH = ITEMS.register("super_dried_fish",
            () -> new SuperDriedFishItem(new Item.Properties().stacksTo(8)));

    private ModItems() {
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TRAINING_STICK);
        } else if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(DRIED_FISH);
            event.accept(SUPER_DRIED_FISH);
        }
    }
}
