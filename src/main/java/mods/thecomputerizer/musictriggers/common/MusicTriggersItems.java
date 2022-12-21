package mods.thecomputerizer.musictriggers.common;

import mods.thecomputerizer.musictriggers.Constants;
import mods.thecomputerizer.musictriggers.common.objects.BlankRecord;
import mods.thecomputerizer.musictriggers.common.objects.EpicItem;
import mods.thecomputerizer.musictriggers.common.objects.EpicItemBlock;
import mods.thecomputerizer.musictriggers.common.objects.MusicTriggersRecord;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MusicTriggersItems {
    public static final Item MUSIC_TRIGGERS_RECORD = makeEpicItem("music_triggers_record", MusicTriggersRecord::new, item -> item.setCreativeTab(CreativeTabs.MISC));
    public static final Item BLANK_RECORD = makeEpicItem("blank_record", BlankRecord::new, item -> item.setCreativeTab(CreativeTabs.MISC));
    public static final Item MUSIC_RECORDER = makeEpicItemBlock(MusicTriggersBlocks.MUSIC_RECORDER, item -> item.setCreativeTab(CreativeTabs.MISC));

    private static EpicItem makeEpicItem(final String name, final Supplier<EpicItem> constructor, final Consumer<EpicItem> config) {
        final EpicItem item = constructor.get();
        config.accept(item);
        item.setUnlocalizedName(Constants.MODID + "." + name);
        item.setRegistryName(Constants.MODID, name);
        item.setMaxStackSize(1);
        return item;
    }

    private static EpicItemBlock makeEpicItemBlock(final Block constructor, final Consumer<EpicItemBlock> config) {
        final EpicItemBlock item = new EpicItemBlock(constructor);
        config.accept(item);
        item.setUnlocalizedName(constructor.getUnlocalizedName());
        item.setRegistryName(Objects.requireNonNull(constructor.getRegistryName()));
        item.setMaxStackSize(1);
        return item;
    }
}
