package mods.thecomputerizer.musictriggers.client.data;

import CoroUtil.util.Vec3;
import com.moandjiezana.toml.Toml;
import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.lunarevents.BloodMoon;
import de.ellpeck.nyx.lunarevents.HarvestMoon;
import de.ellpeck.nyx.lunarevents.StarShower;
import lumien.bloodmoon.Bloodmoon;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.client.EventsClient;
import mods.thecomputerizer.musictriggers.client.MusicPicker;
import mods.thecomputerizer.musictriggers.client.audio.ChannelManager;
import mods.thecomputerizer.musictriggers.common.ServerChannelData;
import mods.thecomputerizer.musictriggers.config.ConfigRegistry;
import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.Level;
import org.orecruncher.dsurround.client.weather.Weather;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import weather2.api.WeatherDataHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static mods.thecomputerizer.musictriggers.MusicTriggers.stringBreaker;

public class Trigger {

    private static final String[] allTriggers = new String[]{"loading","menu","generic","difficulty","time","light",
            "height","raining","storming","snowing","lowhp","dead","creative","spectator","riding","pet","underwater",
            "elytra","fishing","drowning","home","dimension","biome","structure","mob","victory","gui","effect","zones",
            "pvp", "advancement","statistic","command","gamestage","bloodmoon","harvestmoon","fallingstars",
            "rainintensity","tornado","hurricane","sandstorm","season","raid","bluemoon","moon","acidrain","blizzard",
            "cloudy","lightrain"};
    private static final String[] acceptedTriggers = new String[]{"loading","menu","generic","difficulty","time","light",
            "height","raining","storming","snowing","lowhp","dead", "creative","spectator","riding","pet","underwater",
            "elytra","fishing","drowning","home", "dimension","biome", "structure","mob","victory","gui","effect","zones",
            "pvp","advancement","statistic","command","gamestage","bloodmoon","harvestmoon","fallingstars","rainintensity",
            "tornado","hurricane","sandstorm","season"};
    private static final String[] modTriggers = new String[]{"gamestage","bloodmoon","harvestmoon","fallingstars",
            "rainintensity","tornado","hurricane","sandstorm","season"};
    private static final String[] allParameters = new String[]{"priority","identifier","fade_in","fade_out",
            "trigger_delay","song_delay","level","persistence","start_delay","time_bundle","start_hour","end_hour",
            "lowest_day_number","highest_day_number","zone_min_x","zone_max_x","zone_min_y","zone_max_y","zone_min_z",
            "zone_max_z","resource_name","start_toggled","not","passive_persistence","toggle_inactive_playable",
            "detection_range","mob_targeting","health","horde_targeting_percentage","horde_health_percentage","mob_nbt",
            "infernal","champion","victory_id","victory_timeout","moon_phase","light_type","is_whitelist",
            "biome_category","rain_type","biome_temperature","check_lower_temp","biome_rainfall","check_higher_rainfall",
            "check_for_sky","check_above_level"};
    private static final HashMap<String, String> defaultParameterMap = setDefaultParameters();
    private static final HashMap<String, String[]> acceptedParameters = setAcceptedParameters();
    private static final HashMap<String, String[]> requiredParameters = setRequiredParameters();
    private static final HashMap<String, String[]> choiceRequiredParameters = setChoiceRequiredParameters();
    private static final HashMap<String, BiFunction<Trigger, EntityPlayerSP, Boolean>> triggerConditions = setTriggerConditions();
    private static final HashMap<String, HashMap<String, HashMap<String, Trigger>>> registeredTriggers = new HashMap<>();
    private static final HashMap<Trigger, List<Audio>> attachedAudio = new HashMap<>();

    private static HashMap<String, String[]> setRequiredParameters() {
        HashMap<String, String[]> ret = new HashMap<>();
        ret.put("difficulty",new String[]{"identifier","level"});
        ret.put("time",new String[]{"identifier"});
        ret.put("light",new String[]{"identifier","level"});
        ret.put("height",new String[]{"identifier","level"});
        ret.put("riding",new String[]{"identifier"});
        ret.put("dimension",new String[]{"identifier","resource_name"});
        ret.put("biome",new String[]{"identifier"});
        ret.put("structure",new String[]{"identifier","resource_name"});
        ret.put("mob",new String[]{"identifier","level","resource_name"});
        ret.put("victory",new String[]{"identifier","persistence","victory_id"});
        ret.put("gui",new String[]{"identifier","resource_name"});
        ret.put("effect",new String[]{"identifier","resource_name"});
        ret.put("zones",new String[]{"identifier"});
        ret.put("pvp",new String[]{"identifier","persistence"});
        ret.put("advancement",new String[]{"identifier","persistence","resource_name"});
        ret.put("statistic",new String[]{"identifier","resource_name"});
        ret.put("command",new String[]{"identifier","persistence"});
        ret.put("gamestage",new String[]{"identifier","resource_name"});
        ret.put("rainintensity",new String[]{"identifier"});
        ret.put("tornado",new String[]{"identifier","level"});
        ret.put("moon",new String[]{"identifier","resource_name"});
        ret.put("season",new String[]{"identifier","level"});
        return ret;
    }

    private static HashMap<String, String[]> setChoiceRequiredParameters() {
        HashMap<String, String[]> ret = new HashMap<>();
        ret.put("time",new String[]{"time_bundle","start_hour"});
        ret.put("biome",new String[]{"resource_name","biome_category","rain_type","biome_temperature","biome_rainfall"});
        ret.put("zones",new String[]{"zone_min_x","zone_max_x","zone_min_y","zone_max_y","zone_min_z","zone_max_z"});
        return ret;
    }

    private static HashMap<String, String[]> setAcceptedParameters() {
        HashMap<String, String[]> ret = new HashMap<>();
        ret.put("loading",new String[]{});
        ret.put("menu",new String[]{});
        ret.put("generic",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled"});
        ret.put("difficulty",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("time",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "start_toggled","not","persistence","start_delay","time_bundle","start_hour","end_hour",
                "passive_persistence","toggle_inactive_playable","moon_phase","lowest_day_number","highest_day_number"});
        ret.put("light",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable",
                "light_type"});
        ret.put("height",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable",
                "check_for_sky","check_above_level"});
        ret.put("raining",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("storming",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("snowing",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("lowhp",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("dead",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("creative",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("spectator",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("riding",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "start_toggled","not","persistence","resource_name","start_delay","passive_persistence",
                "toggle_inactive_playable"});
        ret.put("pet",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable","detection_range"});
        ret.put("underwater",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("elytra",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("fishing",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("drowning",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("home",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay", "start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable","detection_range"});
        ret.put("dimension",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable"});
        ret.put("biome",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable","biome_category","rain_type","biome_temperature","check_lower_temp",
                "biome_rainfall","check_higher_rainfall"});
        ret.put("structure",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable"});
        ret.put("mob",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay","level",
                "persistence","start_delay","resource_name","start_toggled","not","passive_persistence",
                "toggle_inactive_playable","detection_range","mob_targeting","health","horde_targeting_percentage",
                "horde_health_percentage","mob_nbt","infernal","champion","victory_id","victory_timeout"});
        ret.put("victory",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable",
                "victory_id"});
        ret.put("gui",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable"});
        ret.put("effect",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable"});
        ret.put("zones",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "start_toggled","not","persistence","start_delay","zone_min_x","zone_max_x","zone_min_y",
                "zone_max_y","zone_min_z","zone_max_z","passive_persistence","toggle_inactive_playable"});
        ret.put("pvp",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable",
                "victory_id","victory_timeout"});
        ret.put("advancement",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable"});
        ret.put("statistic",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "level","resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable","check_above_level"});
        ret.put("command",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("gamestage",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "resource_name","start_toggled","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable","is_whitelist"});
        ret.put("bloodmoon",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("harvestmoon",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("fallingstars",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("rainintensity",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "level","start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("tornado",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable",
                "detection_range"});
        ret.put("hurricane",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable","detection_range"});
        ret.put("sandstorm",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable","detection_range"});
        ret.put("season",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("raid",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay","level",
                "start_toggled","not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("bluemoon",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("moon",new String[]{"priority","identifier","fade_in","fade_out","trigger_delay","song_delay",
                "start_toggled","resource_name","not","persistence","start_delay","passive_persistence",
                "toggle_inactive_playable"});
        ret.put("acidrain",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("blizzard",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("cloudy",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        ret.put("lightrain",new String[]{"priority","fade_in","fade_out","trigger_delay","song_delay","start_toggled",
                "not","persistence","start_delay","passive_persistence","toggle_inactive_playable"});
        return ret;
    }

    private static HashMap<String, String> setDefaultParameters() {
        HashMap<String, String> ret = new HashMap<>();
        ret.put("priority","0");
        ret.put("identifier","not_set");
        ret.put("fade_in","0");
        ret.put("fade_out","0");
        ret.put("trigger_delay","0");
        ret.put("song_delay","0");
        ret.put("level",""+Integer.MIN_VALUE);
        ret.put("persistence","0");
        ret.put("start_delay","0");
        ret.put("time_bundle","any");
        ret.put("start_hour","0");
        ret.put("end_hour","0");
        ret.put("lowest_day_number","0");
        ret.put("highest_day_number",""+Integer.MAX_VALUE);
        ret.put("zone_min_x",""+Integer.MIN_VALUE);
        ret.put("zone_min_y",""+Integer.MIN_VALUE);
        ret.put("zone_min_z",""+Integer.MIN_VALUE);
        ret.put("zone_max_x",""+Integer.MAX_VALUE);
        ret.put("zone_max_y",""+Integer.MAX_VALUE);
        ret.put("zone_max_z",""+Integer.MAX_VALUE);
        ret.put("resource_name","any");
        ret.put("start_toggled","true");
        ret.put("not","false");
        ret.put("passive_persistence","true");
        ret.put("toggle_inactive_playable","false");
        ret.put("detection_range","16");
        ret.put("mob_targeting","true");
        ret.put("health","100");
        ret.put("horde_targeting_percentage","50");
        ret.put("horde_health_percentage","50");
        ret.put("mob_nbt","any");
        ret.put("infernal","any");
        ret.put("champion","any");
        ret.put("victory_id","0");
        ret.put("victory_timeout","20");
        ret.put("moon_phase","0");
        ret.put("light_type","any");
        ret.put("is_whitelist","true");
        ret.put("biome_category","any");
        ret.put("rain_type","any");
        ret.put("biome_temperature",""+Float.MIN_VALUE);
        ret.put("check_lower_temp","false");
        ret.put("biome_rainfall",""+Float.MIN_VALUE);
        ret.put("check_higher_rainfall","true");
        ret.put("check_for_sky","true");
        ret.put("check_above_level","false");
        return ret;
    }

    private static HashMap<String, BiFunction<Trigger, EntityPlayerSP, Boolean>> setTriggerConditions() {
        HashMap<String, BiFunction<Trigger, EntityPlayerSP, Boolean>> ret = new HashMap<>();
        ret.put("loading",(trigger,player) -> false);
        ret.put("menu",(trigger,player) -> false);
        ret.put("generic",(trigger,player) -> false);
        ret.put("time",(trigger,player) -> {
            World world = player.getEntityWorld();
            double time = (double) world.getWorldTime() / 24000.0;
            if (time > 1) time = time - (long) time;
            boolean pass;
            String bundle = trigger.getParameter("time_bundle");
            double min;
            double max;
            if (bundle.matches("day")) {
                min = 0d;
                max = 0.54166666666d;
            } else if (bundle.matches("night")) {
                min = 0.54166666666d;
                max = 1d;
            } else if (bundle.matches("sunset")) {
                min = 0.5d;
                max = 0.54166666666d;
            } else if (bundle.matches("sunrise")) {
                min = 0.95833333333d;
                max = 1d;
            } else {
                double doubleStart = trigger.getParameterFloat("start_hour");
                double doubleEnd = trigger.getParameterFloat("end_hour");
                if (doubleEnd == -1) {
                    if (doubleStart <= 21d) doubleEnd = doubleStart + 3d;
                    else doubleEnd = doubleStart - 21d;
                }
                min = doubleStart / 24d;
                max = doubleEnd / 24d;
            }
            if (min < max) pass = time >= min && time < max;
            else pass = time >= min || time < max;
            return pass && trigger.timeTriggerExtras(world.getTotalWorldTime(),world.getMoonPhase() + 1);
        });
        ret.put("light",(trigger,player) ->
                trigger.averageLight(trigger.roundedPos(player),trigger.getParameter("light_type"), player.getEntityWorld())
                        <= trigger.getParameterInt("level"));
        ret.put("height",(trigger,player) ->
                trigger.handleHeight((int) player.posY,player.getEntityWorld().canSeeSky(trigger.roundedPos(player))));
        ret.put("elytra",(trigger,player) -> player.getTicksElytraFlying() > 0);
        ret.put("fishing",(trigger,player) -> player.fishEntity != null && player.fishEntity.isOverWater());
        ret.put("raining",(trigger,player) -> player.getEntityWorld().isRaining());
        ret.put("snowing",(trigger,player) -> {
            BlockPos pos = trigger.roundedPos(player);
            return player.getEntityWorld().isRaining() &&
                    player.getEntityWorld().getBiomeProvider().getTemperatureAtHeight(
                            player.getEntityWorld().getBiome(pos).getTemperature(pos),
                            player.getEntityWorld().getPrecipitationHeight(pos).getY()) < 0.15f;
        });
        ret.put("storming",(trigger,player) -> player.getEntityWorld().isThundering());
        ret.put("lowhp",(trigger,player) -> trigger.handleHP(player.getHealth(), player.getMaxHealth()));
        ret.put("dead",(trigger,player) -> player.getHealth() <= 0f || player.isDead);
        ret.put("spectator",(trigger,player) -> player.isSpectator());
        ret.put("creative",(trigger,player) -> player.isCreative());
        ret.put("riding",(trigger,player) -> trigger.checkRiding(trigger.getResource(),player));
        ret.put("underwater",(trigger,player) ->
                player.getEntityWorld().getBlockState(trigger.roundedPos(player)).getMaterial() == Material.WATER &&
                player.getEntityWorld().getBlockState(trigger.roundedPos(player).up()).getMaterial() == Material.WATER);
        ret.put("pet",(trigger,player) -> {
            boolean pass = false;
            for (EntityLiving ent : player.getEntityWorld().getEntitiesWithinAABB(EntityLiving.class,
                    new AxisAlignedBB(player.posX - 16, player.posY - 8, player.posZ - 16,
                            player.posX + 16, player.posY + 8, player.posZ + 16))) {
                if (ent instanceof EntityTameable && ent.serializeNBT().getString("Owner").matches(player.getName()))
                    pass = true;
            }
            return pass;
        });
        ret.put("drowning",(trigger,player) -> player.getAir() < trigger.getParameterInt("level"));
        ret.put("pvp",(trigger,player) -> false); //TODO
        ret.put("home",(trigger,player) -> !ConfigRegistry.CLIENT_SIDE_ONLY &&
                ChannelManager.getChannel(trigger.channel).getSyncStatus().isHomeTriggerActive());
        ret.put("dimension",(trigger,player) -> trigger.checkDimensionList(player.dimension,trigger.getResource()));
        ret.put("biome",(trigger,player) ->
                trigger.checkBiome(player.getEntityWorld().getBiome(trigger.roundedPos(player)), trigger.getResource(),
                trigger.getParameter("biome_category"),trigger.getParameter("rain_type"),
                trigger.getParameterFloat("biome_temperature"),trigger.getParameterBool("check_lower_temp"),
                trigger.getParameterFloat("biome_rainfall"),trigger.getParameterBool("check_higher_rainfall")));
        ret.put("structure",(trigger,player) -> !ConfigRegistry.CLIENT_SIDE_ONLY &&
                ChannelManager.getChannel(trigger.channel).getSyncStatus().isStructureTriggerActive(trigger.getNameWithID()));
        ret.put("mob",(trigger,player) -> !ConfigRegistry.CLIENT_SIDE_ONLY &&
                ChannelManager.getChannel(trigger.channel).getSyncStatus().isMobTriggerActive(trigger.getNameWithID()));
        ret.put("zones",(trigger,player) -> {
            BlockPos pos = player.getPosition();
            return trigger.zoneHelper(pos.getX(),pos.getY(),pos.getZ());
        });
        ret.put("effect",(trigger,player) -> {
            boolean pass = false;
            MusicPicker.effectList.clear();
            for (PotionEffect p : player.getActivePotionEffects()) {
                MusicPicker.effectList.add(p.getEffectName());
                if (trigger.checkResourceList(p.getEffectName(),trigger.getResource(),false))
                    pass = true;
            }
            return pass;
        });
        ret.put("victory",(trigger,player) ->
                ChannelManager.getChannel(trigger.channel).getVictory(trigger.getParameterInt("victory_id")));
        ret.put("gui",(trigger,player) -> {
            Minecraft mc = Minecraft.getMinecraft();
            String resource = trigger.getResource();
            return (mc.currentScreen!=null && trigger.checkResourceList(mc.currentScreen.getClass().getName(),resource,false)) ||
                    (resource.matches("CREDITS") && mc.currentScreen instanceof GuiWinGame);
        });
        ret.put("difficulty",(trigger,player) -> {
            Minecraft mc = Minecraft.getMinecraft();
            return trigger.difficultyHelper(mc.world.getDifficulty(), mc.world.getWorldInfo().isHardcoreModeEnabled());
        });
        ret.put("advancement",(trigger,player) -> {
            String resource = trigger.getResource();
            boolean pass = (EventsClient.advancement && trigger.checkResourceList(EventsClient.lastAdvancement,resource,false)) ||
                    resource.matches("any");
            if(pass) EventsClient.advancement = false;
            return pass;
        });
        ret.put("statistic",(trigger,player) ->
                trigger.checkStat(trigger.getResource(),trigger.getParameterInt("level"),player));
        ret.put("command",(trigger,player) -> {
            boolean pass = EventsClient.commandHelper(trigger);
            if(pass) EventsClient.commandFinish(trigger);
            return pass;
        });
        ret.put("gamestage",(trigger,player) -> Loader.isModLoaded("gamestages") &&
                trigger.whitelistHelper(GameStageHelper.clientHasAnyOf(
                        player,trigger.parseGamestageList(trigger.getResource()))));
        ret.put("bloodmoon",(trigger,player) -> (Loader.isModLoaded("bloodmoon") &&
                Bloodmoon.proxy.isBloodmoon()) || (Loader.isModLoaded("nyx") &&
                NyxWorld.get(player.getEntityWorld()).currentEvent instanceof BloodMoon));
        ret.put("harvestmoon",(trigger,player) -> Loader.isModLoaded("nyx") &&
                NyxWorld.get(player.getEntityWorld()).currentEvent instanceof HarvestMoon);
        ret.put("fallingstars",(trigger,player) -> Loader.isModLoaded("nyx") &&
                NyxWorld.get(player.getEntityWorld()).currentEvent instanceof StarShower);
        ret.put("rainintensity",(trigger,player) -> Loader.isModLoaded("dsurround") &&
                Weather.getIntensityLevel()>(((float)trigger.getParameterInt("level"))/100f));
        ret.put("tornado",(trigger,player) -> Loader.isModLoaded("weather2") &&
                WeatherDataHelper.getWeatherManagerForClient() != null &&
                WeatherDataHelper.getWeatherManagerForClient().getClosestStormAny(new Vec3(player.getPosition())
                        ,trigger.getParameterInt("detection_range")) !=null &&
                WeatherDataHelper.getWeatherManagerForClient().getClosestStormAny(new Vec3(player.getPosition())
                        ,trigger.getParameterInt("detection_range")).levelCurIntensityStage >= trigger.getParameterInt("level"));
        ret.put("hurricane",(trigger,player) -> Loader.isModLoaded("weather2") &&
                WeatherDataHelper.getWeatherManagerForClient() != null &&
                WeatherDataHelper.getWeatherManagerForClient().getClosestStormAny(new Vec3(player.getPosition())
                        , trigger.getParameterInt("detection_range")) != null &&
                WeatherDataHelper.getWeatherManagerForClient().getClosestStormAny(new Vec3(player.getPosition())
                        , trigger.getParameterInt("detection_range")).isHurricane());
        ret.put("sandstorm",(trigger,player) -> Loader.isModLoaded("weather2") &&
                WeatherDataHelper.getWeatherManagerForClient() != null &&
                WeatherDataHelper.getWeatherManagerForClient().getClosestStormAny(new Vec3(player.getPosition())
                        , trigger.getParameterInt("detection_range")) != null &&
                WeatherDataHelper.getWeatherManagerForClient().getClosestStormAny(new Vec3(player.getPosition())
                        , trigger.getParameterInt("detection_range")).isHurricane());
        ret.put("season",(trigger,player) ->
                trigger.seasonHelper(SeasonHelper.getSeasonState(player.getEntityWorld()).getSeason()));
        return ret;
    }

    public static List<String> getAcceptedParameters(String name) {
        return Arrays.asList(acceptedParameters.get(name));
    }

    private static void checkAndRemoveEmptyTrigger(String channel, String name) {
        boolean remove = registeredTriggers.get(channel).get(name).isEmpty();
        if(remove) registeredTriggers.get(channel).remove(name);
    }

    public static List<String> getAllTriggers() {
        return Arrays.asList(allTriggers);
    }

    public static List<String> getAcceptedTriggers() {
        return Arrays.asList(acceptedTriggers);
    }

    public static boolean acceptsID(String triggerName) {
        return Arrays.asList(acceptedParameters.get(triggerName)).contains("identifier");
    }

    public static Collection<Trigger> getTriggerInstances(String channel, String triggerName) {
        return registeredTriggers.get(channel).get(triggerName).values();
    }

    public static List<Trigger> getRegisteredTriggers(String channel) {
        return new ArrayList<>(registeredTriggers.get(channel).values()).stream()
                .map(HashMap::values)
                .flatMap(Collection::stream)
                .distinct().collect(Collectors.toList());
    }

    public static boolean isRegistered(String channel, String triggerName) {
        return registeredTriggers.get(channel).containsKey(triggerName);
    }

    public static boolean isRegistered(String channel) {
        return registeredTriggers.containsKey(channel);
    }


    public static Trigger parseAndGetTrigger(String channel, String triggerIdentifier) {
        String[] split = triggerIdentifier.split("-",2);
        return split.length==1 ? getTriggerWithNoID(channel, triggerIdentifier) :
                registeredTriggers.get(channel).get(split[0]).get(split[1]);
    }

    public static void parseAndRemoveTrigger(String channel, String triggerIdentifier) {
        String[] split = triggerIdentifier.split("-", 2);
        String id = split.length==1 ? "not_accepted" : split[1];
        registeredTriggers.get(channel).get(split[0]).remove(id);
        checkAndRemoveEmptyTrigger(channel,split[0]);
    }

    public static Trigger getTriggerWithNoID(String channel, String triggerName) {
        if(registeredTriggers.get(channel).containsKey(triggerName))
            return registeredTriggers.get(channel).get(triggerName).get("not_accepted");
        return null;
    }

    public static void removeAudio(String channel, Audio audio) {
        Trigger attached = null;
        for(Trigger trigger : attachedAudio.keySet())
            if(attachedAudio.get(trigger).contains(audio)) {
                attachedAudio.get(trigger).remove(audio);
                if(attachedAudio.get(trigger).isEmpty())
                    attached = trigger;
                break;
            }
        if(Objects.nonNull(attached))
            parseAndRemoveTrigger(channel,attached.getNameWithID());
    }

    public static List<Audio> getPotentialSongs(Trigger trigger) {
        return attachedAudio.get(trigger);
    }

    public static void clearInitialized() {
        registeredTriggers.clear();
        attachedAudio.clear();
    }

    private final String channel;
    private final String name;
    private final HashMap<String, String> parameters;
    private final List<String> acceptedList;

    private Trigger(String name, String channel) {
        this.name = name;
        this.channel = channel;
        this.acceptedList = Arrays.stream(acceptedParameters.get(name)).collect(Collectors.toList());
        this.parameters = buildDefaultParameters(name);
    }

    private HashMap<String, String> buildDefaultParameters(String trigger) {
        HashMap<String, String> ret = new HashMap<>();
        for(String parameter : acceptedParameters.get(trigger)) ret.put(parameter,defaultParameterMap.get(parameter));
        return ret;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public String getRegID() {
        return hasID() ? getParameter("identifier") : "not_accepted";
    }

    public String getNameWithID() {
        if(hasID()) return this.name+"-"+getParameter("identifier");
        return getName();
    }

    public void setParameter(String parameter, String value) {
        this.parameters.put(parameter, value);
    }

    private boolean isDefault(String parameter) {
        return this.parameters.get(parameter).matches(defaultParameterMap.get(parameter));
    }

    private boolean hasAllRequiredParameters() {
        if(requiredParameters.containsKey(this.name)) {
            for(String parameter : requiredParameters.get(this.name)) {
                if(isDefault(parameter)) {
                    MusicTriggers.logExternally(Level.WARN, "Channel[{}] - Trigger {} is missing required parameter {} so it"+
                            " will be skipped!",this.channel,this.name,parameter);
                    return false;
                }
            }
        }
        if(choiceRequiredParameters.containsKey(this.name)) {
            boolean fail = true;
            StringBuilder builder = new StringBuilder();
            for(String parameter : requiredParameters.get(this.name)) {
                builder.append(parameter).append(" ");
                if (!isDefault(parameter))
                    fail = false;
            }
            if(fail) {
                MusicTriggers.logExternally(Level.WARN, "Channel[{}] -  Trigger {} requires one the following triggers: {}",
                        this.channel,this.name,builder.toString());
                return false;
            }
        }
        return true;
    }

    public boolean hasID() {
        return this.acceptedList.contains("identifier");
    }

    private boolean hasIDSet() {
        return !this.acceptedList.contains("identifier") || (this.acceptedList.contains("identifier") &&
                !this.parameters.get("identifier").matches("not_set"));
    }

    public String getParameter(String parameter) {
        return this.parameters.get(parameter);
    }

    public boolean getParameterBool(String parameter) {
        return Boolean.parseBoolean(getParameter(parameter));
    }

    public int getParameterInt(String parameter) {
        return MusicTriggers.randomInt(parameter,getParameter(parameter),
                Integer.parseInt(defaultParameterMap.get(parameter)));
    }

    public float getParameterFloat(String parameter) {
        return MusicTriggers.randomFloat(parameter,getParameter(parameter),
                Float.parseFloat(defaultParameterMap.get(parameter)));
    }

    public boolean runActivationFunction(EntityPlayerSP player) {
        return isActive(triggerConditions.get(getName()).apply(this,player));
    }

    private boolean isActive(boolean active) {
        if(getParameterBool("not")) return !active;
        return active;
    }

    public boolean timeTriggerExtras(long time, int worldPhase) {
        int day = (int)(time/24000);
        int phase = getParameterInt("moon_phase");
        return (day>=getParameterInt("lowest_day_number") && day<=getParameterInt("highest_day_number")) &&
                (phase<=0 || phase>8 || phase==worldPhase);
    }

    public boolean canStart(int fromMap, int fromUniversal) {
        int check = getParameterInt("start_delay");
        return fromMap>=(check>0 ? check : fromUniversal);
    }

    public boolean handleHeight(int playerY, boolean visibleSky) {
        return (getParameterBool("check_above_level") && playerY>getParameterInt("level")) ||
                ((!visibleSky || !getParameterBool("check_for_sky")) && playerY<getParameterInt("level"));
    }

    public boolean handleHP(float health, float maxHealth) {
        return health<(maxHealth*(((float) getParameterInt("level"))/100f));
    }

    public String getResource() {
        return getParameter("resource_name");
    }

    public ServerChannelData.Home makeHomePacket() {
        return new ServerChannelData.Home(getParameterInt("detection_range"));
    }

    public ServerChannelData.Structure makeStructurePacket(long pos, int dimID) {
        return new ServerChannelData.Structure(getNameWithID(),getResource(),pos,dimID);
    }

    public ServerChannelData.Mob makeMobPacket() {
        return new ServerChannelData.Mob(getNameWithID(),getResource(),getParameterInt("detection_range"),
                getParameterBool("mob_targeting"),getParameterInt("horde_targeting_percentage"),
                getParameterInt("health"),getParameterInt("horde_health_percentage"),getParameterInt("victory_id"),
                getParameter("infernal"),getParameterInt("level"),getParameterInt("victory_timeout"),
                getParameter("mob_nbt"),getParameter("champion"));
    }

    public boolean zoneHelper(int x, int y, int z) {
        return x>getParameterInt("zone_min_x") && x<getParameterInt("zone_max_x") &&
                y>getParameterInt("zone_min_y") && y<getParameterInt("zone_max_y") &&
                z>getParameterInt("zone_min_z") && z<getParameterInt("zone_max_z");
    }

    public boolean difficultyHelper(EnumDifficulty difficulty, boolean hardcore) {
        int diff = getParameterInt("level");
        return (diff==4 && hardcore) || (diff==3 && difficulty==EnumDifficulty.HARD) ||
                (diff==2 && difficulty==EnumDifficulty.NORMAL) || (diff==1 && difficulty==EnumDifficulty.EASY) ||
                (diff==0 && difficulty==EnumDifficulty.PEACEFUL);
    }

    public boolean whitelistHelper(boolean initial) {
        boolean whitelist = getParameterBool("is_whitelist");
        return (whitelist && initial) || (!whitelist && !initial);
    }

    public boolean seasonHelper(Season season) {
        int id = getParameterInt("level");
        return (id==0 && season==Season.SPRING) || (id==1 && season==Season.SUMMER) || (id==2 && season==Season.AUTUMN) ||
                (id==3 && season==Season.WINTER);
    }

    public BlockPos roundedPos(EntityPlayer p) {
        return new BlockPos((Math.round(p.posX * 2) / 2.0), (Math.round(p.posY * 2) / 2.0), (Math.round(p.posZ * 2) / 2.0));
    }


    public double averageLight(BlockPos p, String lightType, World world) {
        if(lightType.matches("block") || lightType.matches("sky"))
            return lightType.matches("block") ?
                    world.getLightFor(EnumSkyBlock.BLOCK, p) : world.getLightFor(EnumSkyBlock.SKY, p);
        return world.getLight(p, true);
    }

    public boolean checkBiome(Biome b, String name, String category, String rainType, float temperature, boolean cold, float rainfall, boolean togglerainfall) {
        if(checkResourceList(Objects.requireNonNull(b.getRegistryName()).toString(),name, false) || name.matches("any")) {
            if(checkResourceList(b.getTempCategory().toString(),category,false) || category.matches("any")) {
                boolean pass = false;
                if(rainfall==Float.MIN_VALUE) pass = true;
                else if(b.getRainfall()>rainfall && togglerainfall) pass = true;
                else if(b.getRainfall()<rainfall && !togglerainfall) pass = true;
                if(pass) {
                    if (rainType.matches("any"))
                        return biomeTemp(b,temperature,cold);
                    else if (rainType.matches("none") && !b.canRain())
                        return biomeTemp(b,temperature,cold);
                    else if (b.canRain() && rainType.matches("rain"))
                        return biomeTemp(b,temperature,cold);
                    else if (b.isSnowyBiome() && rainType.matches("snow"))
                        return biomeTemp(b,temperature,cold);
                }
            }
        }
        return false;
    }

    private boolean biomeTemp(Biome b, float temperature, boolean cold) {
        float bt = b.getDefaultTemperature();
        if (temperature == Float.MIN_VALUE) return true;
        else if (bt >= temperature && !cold) return true;
        else return bt <= temperature && cold;
    }

    public boolean checkResourceList(String type, String resourceList, boolean match) {
        for(String resource : stringBreaker(resourceList,";")) {
            if(match && type.matches(resource)) return true;
            else if(!match && type.contains(resource)) return true;
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    public boolean checkDimensionList(int playerDim, String resourceList) {
        for(String resource : stringBreaker(resourceList,";")) if((playerDim+"").matches(resource)) return true;
        if(DimensionType.getById(playerDim)==null) return false;
        String dimName = DimensionType.getById(playerDim).getName();
        return dimName != null && checkResourceList(dimName, resourceList, false);
    }

    public List<String> parseGamestageList(String resourceList) {
        return Arrays.asList(resourceList.split(";"));
    }

    public boolean checkRiding(String resource, EntityPlayerSP player) {
        if(!player.isRiding() || player.getRidingEntity()==null) return false;
        else if(resource.matches("any")) return true;
        else if(checkResourceList(Objects.requireNonNull(player.getRidingEntity()).getName(),resource,true)) return true;
        else if(EntityList.getKey(player.getRidingEntity())==null) return false;
        return checkResourceList(Objects.requireNonNull(EntityList.getKey(player.getRidingEntity())).toString(),resource,false);
    }

    public boolean checkStat(String stat, int level, EntityPlayerSP player) {
        if(Minecraft.getMinecraft().getConnection()!=null) {
            Objects.requireNonNull(Minecraft.getMinecraft().getConnection()).sendPacket(
                    new CPacketClientStatus(CPacketClientStatus.State.REQUEST_STATS));
            for (StatBase s : StatList.ALL_STATS) {
                return checkResourceList(s.statId, stat, false) && player.getStatFileWriter().readStat(s) > level;
            }
        }
        return false;
    }

    public boolean defaultToggle() {
        return getParameterBool("start_toggled");
    }

    public List<String> getAsTomlLines(String songName, boolean multi) {
        List<String> lines = new ArrayList<>();
        lines.add(multi ? "\t[["+songName+".trigger]]" : "\t["+songName+".trigger]");
        lines.add("\t\tname = \""+this.name+"\"");
        for(Map.Entry<String, String> parameter : this.parameters.entrySet())
            if(!isDefault(parameter.getKey()))
                lines.add("\t\t"+parameter.getKey()+" = \""+parameter.getValue()+"\"");
        return lines;
    }

    private static void logRegister(String channel, String triggerName, String id, String songName) {
        if(id.matches("not_accepted"))
            MusicTriggers.logExternally(Level.INFO,"Registered trigger {} to song {} in channel {}",
                    triggerName,songName,channel);
        else MusicTriggers.logExternally(Level.INFO,"Registered instance of trigger {} with identifier {} to "+
                        "song {} in channel {}", triggerName,id,songName,channel);
    }

    private static String getIDOrFiller(String name, Toml triggerTable) {
        if(!Arrays.stream(acceptedParameters.get(name)).collect(Collectors.toList()).contains("identifier"))
            return "not_accepted";
        if(!triggerTable.contains("id") && !triggerTable.contains("identifier")) return "missing_id";
        if(triggerTable.contains("identifier")) return triggerTable.getString("identifier");
        return triggerTable.getString("id");
    }

    public static Trigger createOrGetInstance(String name, String channel, Audio audio, Toml triggerTable) {
        registeredTriggers.putIfAbsent(channel, new HashMap<>());
        registeredTriggers.get(channel).putIfAbsent(name, new HashMap<>());
        String id = getIDOrFiller(name, triggerTable);
        if(id.matches("missing_id")) return null;
        else if(registeredTriggers.get(channel).get(name).containsKey(id)) {
            logRegister(channel,name,id,audio.getName());
            Trigger trigger = registeredTriggers.get(channel).get(name).get(id);
            attachedAudio.putIfAbsent(trigger, new ArrayList<>());
            attachedAudio.get(trigger).add(audio);
            return registeredTriggers.get(channel).get(name).get(id);
        }
        Trigger trigger = new Trigger(name,channel);
        for (String parameter : allParameters) {
            if (triggerTable.contains(parameter)) {
                if (!trigger.acceptedList.contains(parameter))
                    MusicTriggers.logExternally(Level.WARN, "Channel[{}] - Parameter {} is not accepted for "+
                            "trigger {} so it will be skipped!",channel,parameter,name);
                else trigger.setParameter(parameter,triggerTable.getString(parameter));
            }
        }
        if(!trigger.hasIDSet() && triggerTable.contains("id"))
            trigger.setParameter("identifier", triggerTable.getString("id"));
        if(trigger.hasAllRequiredParameters()) {
            registeredTriggers.get(channel).get(name).put(id,trigger);
            attachedAudio.putIfAbsent(trigger, new ArrayList<>());
            attachedAudio.get(trigger).add(audio);
            logRegister(channel,name,id,audio.getName());
            return trigger;
        }
        return null;
    }

    public static Trigger createEmptyForGui(String triggerName, String channel) {
        return new Trigger(triggerName,channel);
    }

    public static Trigger createEmptyWithIDForGui(String channel, String triggerIdentifier) {
        String[] split = triggerIdentifier.split("-",2);
        Trigger trigger = new Trigger(triggerIdentifier,channel);
        if(split.length!=1) trigger.setParameter("identifier",split[1]);
        return trigger;
    }
}