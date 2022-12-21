package mods.thecomputerizer.musictriggers.client.audio;

import mods.thecomputerizer.musictriggers.Constants;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.client.ClientSync;
import mods.thecomputerizer.musictriggers.config.ConfigChannels;
import mods.thecomputerizer.musictriggers.config.ConfigDebug;
import mods.thecomputerizer.musictriggers.config.ConfigRegistry;
import mods.thecomputerizer.musictriggers.util.CustomTick;
import mods.thecomputerizer.musictriggers.util.RegistryHandler;
import mods.thecomputerizer.musictriggers.util.packets.PacketQueryServerInfo;
import mods.thecomputerizer.theimpossiblelibrary.util.file.FileUtil;
import net.minecraft.block.BlockJukebox;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.Display;

import java.io.File;
import java.util.*;

public class ChannelManager {
    private static JukeboxChannel jukeboxChannel;
    private static final HashMap<String, ConfigChannels.ChannelInfo> channelBlueprints = new HashMap<>();
    private static final HashMap<String,Channel> channelMap = new HashMap<>();
    public static final HashMap<String, File> openAudioFiles = new HashMap<>();
    private static final List<Channel> wasAlreadyPaused = new ArrayList<>();

    private static int tickCounter = 0;
    public static boolean reloading = false;

    public static void createChannel(String channelName, String soundCategory, String mainFileName,
                                     String transitionsFileName, String commandsFileName, String togglesFileName,
                                     String redirectFileName, String localFolderPath, boolean pausedByJukeBox,
                                     boolean overridesNormalMusic, ConfigChannels.ChannelInfo blueprint) {
        if(verifyChannelParameters(channelName, mainFileName, transitionsFileName, commandsFileName, togglesFileName, redirectFileName)) {
            channelMap.put(channelName, new Channel(channelName, soundCategory, pausedByJukeBox, overridesNormalMusic,
                    mainFileName, transitionsFileName, commandsFileName, togglesFileName, redirectFileName, localFolderPath));
            channelBlueprints.put(channelName,blueprint);
        } else MusicTriggers.logExternally(Level.ERROR, "Channel {} failed to register! See the above errors for" +
                "more information.",channelName);
    }

    private static boolean verifyChannelParameters(String channelName, String mainFileName, String transitionsFileName,
                                                   String commandsFileName, String togglesFileName, String redirectFileName) {
        if(channelName.matches("jukebox") || channelName.matches("preview"))
            MusicTriggers.logExternally(Level.ERROR, "Channel name cannot be set to \"jukebox\" or \"preview\"" +
                    "as those are used for internal functions!");
        else if(Objects.nonNull(channelMap.get(channelName)))
            MusicTriggers.logExternally(Level.ERROR, "Channel with name "+channelName+ " already exists" +
                    "! Different channels must have unique names!");
        boolean verifiedFile = verifyFilePath(mainFileName,"main");
        if(!verifiedFile) return false;
        verifiedFile = verifyFilePath(transitionsFileName,"transitions");
        if(!verifiedFile) return false;
        verifiedFile = verifyFilePath(commandsFileName,"commands");
        if(!verifiedFile) return false;
        verifiedFile = verifyFilePath(togglesFileName,"toggles");
        if(!verifiedFile) return false;
        return verifyFilePath(redirectFileName,"redirect");
    }

    private static boolean verifyFilePath(String filePath, String configType) {
        if(filePath.endsWith("\\.toml") || filePath.endsWith("\\.txt")) {
            MusicTriggers.logExternally(Level.ERROR, "Please do not include the " +
                    "file extension in your file path. That is handled internally.");
            return false;
        }
        if(filePath.matches("debug") || filePath.matches("registration") || filePath.matches("channels")
                || filePath.matches("jukebox")) {
            MusicTriggers.logExternally(Level.ERROR, "Config type {} cannot be {} as that is the name of a " +
                    "preset config file!",configType,filePath);
            return false;
        }
        for(ConfigChannels.ChannelInfo registeredBlueprint : channelBlueprints.values()) {
            if(!registeredBlueprint.verifyOtherFilePathIsValid(filePath, configType))
                return false;
        }
        return true;
    }

    public static void createJukeboxChannel() {
        jukeboxChannel = new JukeboxChannel("jukebox");
    }

    public static void playCustomJukeboxSong(boolean start, String channel, String id, BlockPos pos) {
        if(!start) jukeboxChannel.stopTrack();
        else jukeboxChannel.playTrack(channelMap.get(channel).getCopyOfTrackFromID(id),pos);
    }

    public static void parseConfigFiles() {
        collectSongs();
        for(Channel channel : channelMap.values()) channel.parseConfigs();
        ConfigDebug.initialize(new File(Constants.CONFIG_DIR,"debug.toml"));
        ConfigRegistry.initialize(new File(Constants.CONFIG_DIR,"registration.toml"));
    }

    public static void readResourceLocations() {
        for(Channel channel : channelMap.values()) channel.readResourceLocations();
    }

    public static void collectSongs() {
        openAudioFiles.clear();
        for(Channel channel : channelMap.values()) {
            File folder = new File(channel.getLocalFolder());
            FileUtil.generateNestedFile(folder, false);
            File[] listOfFiles = folder.listFiles((dir, name) -> dir.canRead());
            if (listOfFiles != null)
                for (File f : listOfFiles)
                    openAudioFiles.putIfAbsent(channel.getLocalFolder(),f);
        }
    }

    public static Channel getChannel(String channel) {
        return channelMap.get(channel);
    }

    public static boolean channelExists(String channel) {
        return channelMap.containsKey(channel);
    }

    public static Collection<Channel> getAllChannels() {
        return channelMap.values();
    }

    public static List<String> getChannelNames() {
        return new ArrayList<>(channelMap.keySet());
    }

    public static boolean canAnyChannelOverrideMusic() {
        for(Channel channel : getAllChannels()) if(channel.overridesNormalMusic() && channel.isPlaying()) return true;
        return false;
    }

    public static void syncInfoFromServer(ClientSync sync) {
        try {
            getChannel(sync.getChannel()).sync(sync);
        } catch (NullPointerException exception) {
            MusicTriggers.logExternally(Level.ERROR, "Channel "+sync.getChannel()+" did not exist and could " +
                    "not be synced!");
        }
    }

    public static void pauseAllChannels(boolean fromJukebox) {
        for(Channel channel : channelMap.values()) {
            if(channel.isPaused()) wasAlreadyPaused.add(channel);
            channel.setPaused(true,fromJukebox);
        }
    }

    public static void unPauseAllChannels() {
        for(Channel channel : channelMap.values())
            if(!wasAlreadyPaused.contains(channel) && channel.isPaused())
                channel.setPaused(false,false);
        wasAlreadyPaused.clear();
    }

    public static void reloadAllChannels() {
        collectSongs();
        for(Channel channel : channelMap.values()) channel.reload();
        refreshDebug();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tickChannels(CustomTick event) {
        jukeboxChannel.checkStopPlaying(reloading);
        if(!reloading) {
            if(event.checkTickRate(20)) {
                tickCounter++;
                if (!Display.isCreated() || !Display.isActive())
                    pauseAllChannels(false);
                else if (!Minecraft.getMinecraft().isGamePaused()) {
                    if (checkForJukeBox()) {
                        pauseAllChannels(true);
                        for (Channel channel : channelMap.values()) if (!channel.isPaused()) channel.tickFast();
                        if (tickCounter % 4 == 0) {
                            for (Channel channel : channelMap.values()) if (!channel.isPaused()) channel.tickSlow();
                            sendUpdatePacket();
                        }
                    } else {
                        unPauseAllChannels();
                        for (Channel channel : channelMap.values()) if (!channel.isPaused()) channel.tickFast();
                        if (tickCounter % 4 == 0) {
                            for (Channel channel : channelMap.values()) channel.tickSlow();
                            sendUpdatePacket();
                        }
                    }
                }
                if(tickCounter>=100) tickCounter=5;
            }
        }
    }

    private static boolean checkForJukeBox() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if(player!=null)
            for (int x = player.chunkCoordX - 3; x <= player.chunkCoordX + 3; x++)
                for (int z = player.chunkCoordZ - 3; z <= player.chunkCoordZ + 3; z++) {
                    Map<BlockPos, TileEntity> currentChunkTE = player.getEntityWorld().getChunkFromChunkCoords(x, z).getTileEntityMap();
                    for (TileEntity te : currentChunkTE.values())
                        if (te instanceof BlockJukebox.TileEntityJukebox && te.getBlockMetadata() != 0)
                            return true;
                }
        return false;
    }

    public static void refreshDebug() {
        ConfigDebug.read();
    }

    private static void sendUpdatePacket() {
        if(Minecraft.getMinecraft().player!=null && !ConfigRegistry.CLIENT_SIDE_ONLY)
            RegistryHandler.network.sendToServer(
                    new PacketQueryServerInfo.PacketQueryServerInfoMessage(
                            new ArrayList<>(channelMap.values())));
    }
}
