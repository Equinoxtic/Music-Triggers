package mods.thecomputerizer.musictriggers.client;

import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.client.gui.GuiCurPlaying;
import mods.thecomputerizer.musictriggers.common.SoundHandler;
import mods.thecomputerizer.musictriggers.common.objects.MusicTriggersRecord;
import mods.thecomputerizer.musictriggers.config.configRegistry;
import mods.thecomputerizer.musictriggers.config.configTitleCards;
import mods.thecomputerizer.musictriggers.config.configToml;
import mods.thecomputerizer.musictriggers.util.RegistryHandler;
import mods.thecomputerizer.musictriggers.util.audio.SoundManipulator;
import mods.thecomputerizer.musictriggers.util.audio.setVolumeSound;
import mods.thecomputerizer.musictriggers.util.packets.packetCurSong;
import net.minecraft.block.BlockJukebox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = MusicTriggers.MODID, value = Side.CLIENT)
public class MusicPlayer {

    public static final KeyBinding RELOAD = new KeyBinding("key.reload_musictriggers", Keyboard.KEY_R, "key.categories.musictriggers");

    public static String[] curTrackList;
    public static String[] holder;
    public static String curTrack;
    public static String curTrackHolder;
    public static ISound curMusic;
    public static Random rand = new Random();
    public static Minecraft mc = Minecraft.getMinecraft();
    public static net.minecraft.client.audio.SoundHandler sh = Minecraft.getMinecraft().getSoundHandler();
    public static int tickCounter = 0;
    public static boolean fading = false;
    public static boolean reverseFade = false;
    private static int tempFade = 0;
    private static float saveVol = 1;
    public static boolean delay = false;
    public static int delayTime = 0;
    public static boolean playing = false;
    public static SoundEvent fromRecord = null;
    public static boolean reloading = false;
    public static boolean cards = true;
    public static boolean finish = false;
    public static HashMap<String, setVolumeSound> musicLinker = new HashMap<>();
    public static HashMap<String, String[]> triggerLinker = new HashMap<>();
    public static HashMap<String, Float> volumeLinker = new HashMap<>();
    public static HashMap<String, Map<Integer, String[]>> loopLinker = new HashMap<>();
    public static HashMap<String, Map<Integer, Integer>> loopLinkerCounter = new HashMap<>();
    public static List<String> oncePerTrigger = new ArrayList<>();
    public static List<String> onceUntilEmpty = new ArrayList<>();
    private static String trackToDelete;
    private static int indexToDelete;
    private static List<String> playedEvents = new ArrayList<>();
    private static ISound playedMusic;

    @SuppressWarnings("rawtypes")
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTick(TickEvent.ClientTickEvent event) {
        if(!reloading && tickCounter % 2 == 0) {
            if (MusicPicker.fishBool) MusicPicker.fishingStart++;
            if (MusicPicker.waterBool) MusicPicker.waterStart++;
            for (Map.Entry<String, Integer> stringListEntry : MusicPicker.triggerPersistence.entrySet()) {
                String eventID = ((Map.Entry) stringListEntry).getKey().toString();
                MusicPicker.triggerPersistence.putIfAbsent(eventID, 0);
                if (MusicPicker.triggerPersistence.get(eventID) > 0) MusicPicker.triggerPersistence.put(eventID, MusicPicker.triggerPersistence.get(eventID) - 1);
            }
            if(curTrack!=null && sh.isSoundPlaying(curMusic) && configToml.loopPoints.containsKey(curTrack)) {
                for(String key : musicLinker.keySet()) {
                    if(loopLinker.get(key)!=null) {
                        for (int i : loopLinker.get(key).keySet()) {
                            try {
                                if (loopLinkerCounter.get(key).get(i) < Integer.parseInt(loopLinker.get(key).get(i)[0]) && Integer.parseInt(loopLinker.get(key).get(i)[2]) <= SoundManipulator.getMillisecondTimeForSource(sh.sndManager.sndSystem, sh.sndManager.invPlayingSounds.get(musicLinker.get(key)))) {
                                    MusicTriggers.logger.info("Loop boundary passed");
                                    SoundManipulator.setMillisecondTimeForSource(sh.sndManager.sndSystem, sh.sndManager.invPlayingSounds.get(musicLinker.get(key)), Integer.parseInt(loopLinker.get(key).get(i)[1]));
                                    loopLinkerCounter.get(key).put(i, loopLinkerCounter.get(key).get(i) + 1);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new RuntimeException("There was a problem while trying to loop - Check the log for details");
                            }
                        }
                    }
                }
            }
            if(curTrack!=null && sh.isSoundPlaying(curMusic) && mc.currentScreen instanceof GuiCurPlaying) {
                ((GuiCurPlaying)mc.currentScreen).setSlider(GuiCurPlaying.getSongPosInSeconds(curMusic));
            }
            if (fading && !reverseFade) {
                if (tempFade == 0) {
                    fading = false;
                    curTrackList = null;
                    sh.stopSound(curMusic);
                    sh.setSoundLevel(SoundCategory.MASTER, saveVol);
                    eventsClient.IMAGE_CARD = null;
                    eventsClient.fadeCount = 1000;
                    eventsClient.timer = 0;
                    eventsClient.activated = false;
                    eventsClient.ismoving = false;
                    cards = true;
                } //else if((MusicPicker.curFade-tempFade)%10==0) {
                //if(Arrays.equals(curTrackList,holder)) {
                //MusicTriggers.logger.info("beginning reverse fading");
                //reverseFade = true;
                //}
                //}
                else {
                    sh.setSoundLevel(SoundCategory.MASTER, saveVol * (float) (((double) tempFade) / ((double) MusicPicker.curFade)));
                    tempFade -= 1;
                }
            } else if (reverseFade) {
                if (tempFade >= MusicPicker.curFade) {
                    fading = false;
                    reverseFade = false;
                    sh.setSoundLevel(SoundCategory.MASTER, saveVol);
                } else {
                    sh.setSoundLevel(SoundCategory.MASTER, saveVol * (float) (((double) tempFade) / ((double) MusicPicker.curFade)));
                    tempFade += 1;
                }
            }
            if (delay) {
                delayTime -= 1;
                if (delayTime <= 0) delay = false;
            }
            if (tickCounter % 10 == 0 && !fading && !delay) {
                if (MusicPicker.player != null && (MusicPicker.player.getHeldItemMainhand().getItem() instanceof MusicTriggersRecord)) fromRecord = ((MusicTriggersRecord) MusicPicker.player.getHeldItemMainhand().getItem()).getSound();
                else fromRecord = null;
                playing = false;
                if (MusicPicker.player != null) {
                    for (int x = MusicPicker.player.chunkCoordX - 3; x <= MusicPicker.player.chunkCoordX + 3; x++) {
                        for (int z = MusicPicker.player.chunkCoordZ - 3; z <= MusicPicker.player.chunkCoordZ + 3; z++) {
                            Map<BlockPos, TileEntity> currentChunkTE = MusicPicker.world.getChunkFromChunkCoords(x, z).getTileEntityMap();
                            for (TileEntity te : currentChunkTE.values()) {
                                if (te != null) {
                                    if (te instanceof BlockJukebox.TileEntityJukebox) {
                                        if (te.getBlockMetadata() != 0) playing = true;
                                    }
                                }
                            }
                        }
                    }
                }
                holder = MusicPicker.playThese();
                if (holder != null && !Arrays.asList(holder).isEmpty() && !playing) {
                    for(String playable : MusicPicker.playableList) {
                        if(!MusicPicker.titleCardEvents.contains(playable)) {
                            if(Boolean.parseBoolean(SoundHandler.TriggerInfoMap.get(playable)[34])) {
                                if(!SoundHandler.TriggerInfoMap.get(playable)[10].matches("_")) {
                                    String[] trigger = playable.split("-");
                                    SoundHandler.TriggerSongMap.get(trigger[0]).entrySet().removeIf(stringStringEntry -> stringStringEntry.getValue().matches(trigger[1]));
                                } else SoundHandler.TriggerSongMap.remove(playable);
                                SoundHandler.TriggerInfoMap.remove(playable);
                            }
                        }
                    }
                    if (curTrackList == null && !finish) curTrackList = holder;
                    if (curMusic != null) {
                        if (!sh.isSoundPlaying(curMusic) || mc.gameSettings.getSoundLevel(SoundCategory.MUSIC) == 0 || mc.gameSettings.getSoundLevel(SoundCategory.MASTER) == 0) {
                            finish = false;
                            sh.stopSounds();
                            curMusic = null;
                            delay = true;
                            delayTime = MusicPicker.curDelay;
                            removeTrack(trackToDelete,indexToDelete,playedEvents,playedMusic);
                        }
                    }
                    if (!finish) {
                        if (MusicPicker.shouldChange || !Arrays.equals(curTrackList, holder)) {
                            removeTrack(trackToDelete,indexToDelete,playedEvents,playedMusic);
                            if(curTrackList.length!=0) changeTrack();
                            else curTrackList = null;
                        } else if (curMusic == null && mc.gameSettings.getSoundLevel(SoundCategory.MUSIC) > 0 && mc.gameSettings.getSoundLevel(SoundCategory.MASTER) > 0) {
                            triggerLinker = new HashMap<>();
                            musicLinker = new HashMap<>();
                            eventsClient.GuiCounter = 0;
                            curTrackList = Arrays.stream(curTrackList).filter(track -> !oncePerTrigger.contains(track)).toArray(String[]::new);
                            curTrackList = Arrays.stream(curTrackList).filter(track -> !onceUntilEmpty.contains(track)).toArray(String[]::new);
                            if (curTrackList.length >= 1) {
                                int i = ThreadLocalRandom.current().nextInt(0, curTrackList.length);
                                if (curTrackList.length > 1 && curTrack != null) {
                                    int total = Arrays.stream(curTrackList).mapToInt(s -> Integer.parseInt(configToml.otherinfo.get(s)[3])).sum();
                                    int j;
                                    for (j = 0; j < 1000; j++) {
                                        int r = ThreadLocalRandom.current().nextInt(1, total + 1);
                                        MusicTriggers.logger.debug("Random was between 1 and " + (total + 1) + " " + r + " was chosen");
                                        String temp = " ";
                                        for (String s : curTrackList) {
                                            if (r < Integer.parseInt(configToml.otherinfo.get(s)[3])) {
                                                temp = s;
                                                break;
                                            }
                                            r -= Integer.parseInt(configToml.otherinfo.get(s)[3]);
                                        }
                                        if (!temp.matches(curTrack) && !temp.matches(" ")) {
                                            curTrack = temp;
                                            break;
                                        }
                                    }
                                    if (j >= 1000) MusicTriggers.logger.warn("Attempt to get non duplicate song passed 1000 tries! Forcing current song " + configToml.songholder.get(curTrack) + " to play.");
                                } else {
                                    curTrack = curTrackList[i];
                                }
                                MusicTriggers.logger.debug(curTrack + " was chosen");
                                if (curTrack != null) {
                                    finish = Boolean.parseBoolean(configToml.otherinfo.get(curTrack)[2]);
                                    curTrackHolder = configToml.songholder.get(curTrack);
                                    MusicTriggers.logger.info("Attempting to play track: " + curTrackHolder);
                                    if (configToml.triggerlinking.get(curTrack) != null) {
                                        triggerLinker.put("song-" + 0, configToml.triggerlinking.get(curTrack).get(curTrack));
                                        musicLinker.put("song-" + 0, new setVolumeSound(new ResourceLocation(MusicTriggers.MODID, "music." + curTrackHolder), SoundCategory.MUSIC, Float.parseFloat(configToml.otherinfo.get(curTrack)[4]), Float.parseFloat(configToml.otherinfo.get(curTrack)[0]), false, 1, ISound.AttenuationType.NONE, 0F, 0F, 0F));
                                        volumeLinker.put("song-" + 0, Float.parseFloat(configToml.otherinfo.get(curTrack)[4]));
                                        for(int l : configToml.loopPoints.get(curTrack).keySet()) {
                                            loopLinker.putIfAbsent("song-" + 0, new HashMap<>());
                                            loopLinker.get("song-" + 0).put(l, configToml.loopPoints.get(curTrack).get(l));
                                            loopLinkerCounter.putIfAbsent("song-" + 0, new HashMap<>());
                                            loopLinkerCounter.get("song-" + 0).put(l, 0);
                                        }
                                        int linkcounter = 0;
                                        for (String song : configToml.triggerlinking.get(curTrack).keySet()) {
                                            if (!song.matches(curTrack)) {
                                                triggerLinker.put("song-" + linkcounter, configToml.triggerlinking.get(curTrack).get(song));
                                                musicLinker.put("song-" + linkcounter, new setVolumeSound(new ResourceLocation(MusicTriggers.MODID, "music." + song), SoundCategory.MUSIC, Float.parseFloat(configToml.otherlinkinginfo.get(curTrack).get(song)[1]),
                                                        Float.parseFloat(configToml.otherlinkinginfo.get(curTrack).get(song)[0]), false, 1, ISound.AttenuationType.NONE, 0F, 0F, 0F));
                                                volumeLinker.put("song-" + linkcounter, Float.parseFloat(configToml.otherlinkinginfo.get(curTrack).get(song)[1]));
                                                if(configToml.linkingLoopPoints.get(curTrack)!=null && configToml.linkingLoopPoints.get(curTrack).get(song)!=null) {
                                                    for (int l : configToml.linkingLoopPoints.get(curTrack).get(song).keySet()) {
                                                        loopLinker.putIfAbsent("song-" + linkcounter, new HashMap<>());
                                                        loopLinker.get("song-" + linkcounter).put(l, configToml.linkingLoopPoints.get(curTrack).get(song).get(l));
                                                        loopLinkerCounter.putIfAbsent("song-" + linkcounter, new HashMap<>());
                                                        loopLinkerCounter.get("song-" + linkcounter).put(l, 0);
                                                    }
                                                }
                                            }
                                            linkcounter++;
                                        }
                                    } else {
                                        musicLinker.put("song-" + 0, new setVolumeSound(new ResourceLocation(MusicTriggers.MODID, "music." + curTrackHolder), SoundCategory.MUSIC, Float.parseFloat(configToml.otherinfo.get(curTrack)[4]), Float.parseFloat(configToml.otherinfo.get(curTrack)[0]), false, 1, ISound.AttenuationType.NONE, 0F, 0F, 0F));
                                    }
                                    if (configRegistry.registerDiscs && MusicPicker.player != null) {
                                        RegistryHandler.network.sendToServer(new packetCurSong.packetCurSongMessage(curTrackHolder, MusicPicker.player.getUniqueID()));
                                    }
                                    sh.stopSounds();
                                    if (cards) renderCards();
                                    for (Map.Entry<String, setVolumeSound> stringListEntry : musicLinker.entrySet()) {
                                        String checkThis = ((Map.Entry) stringListEntry).getKey().toString();
                                        if (!checkThis.matches("song-0")) musicLinker.get(checkThis).setVolume(Float.MIN_VALUE);
                                        else curMusic = musicLinker.get(checkThis);
                                        sh.playSound(musicLinker.get(checkThis));
                                    }
                                    if (Integer.parseInt(configToml.otherinfo.get(curTrack)[1])==1) onceUntilEmpty.add(curTrack);
                                    if (Integer.parseInt(configToml.otherinfo.get(curTrack)[1])==2) oncePerTrigger.add(curTrack);
                                    else if (Integer.parseInt(configToml.otherinfo.get(curTrack)[1])==3) {
                                        trackToDelete = curTrack;
                                        indexToDelete = i;
                                        playedEvents = MusicPicker.titleCardEvents;
                                        playedMusic = curMusic;
                                    }
                                } else curTrackList = null;
                            }
                            else onceUntilEmpty = new ArrayList<>();
                        }
                    }
                } else if (!finish || playing) {
                    curTrack = null;
                    curTrackHolder = null;
                    eventsClient.IMAGE_CARD = null;
                    eventsClient.fadeCount = 1000;
                    eventsClient.timer = 0;
                    eventsClient.activated = false;
                    eventsClient.ismoving = false;
                    cards = true;
                    if (curMusic != null) {
                        for (String is : musicLinker.keySet()) {
                            sh.stopSound(musicLinker.get(is));
                        }
                        curMusic = null;
                        removeTrack(trackToDelete,indexToDelete,playedEvents,playedMusic);
                    }
                }
            }
        }
        tickCounter++;
    }

    @SuppressWarnings("ConstantConditions")
    public static void renderCards() {
        MusicTriggers.logger.info("Finding cards to render");
        int markForDeletion = -1;
        for (int i : configTitleCards.titlecards.keySet()) {
            boolean pass = false;
            if(MusicPicker.titleCardEvents.containsAll(configTitleCards.titlecards.get(i).getTriggers()) && configTitleCards.titlecards.get(i).getTriggers().containsAll(MusicPicker.titleCardEvents)) pass=true;
            else if(configTitleCards.titlecards.get(i).getVague() && MusicPicker.playableList.containsAll(configTitleCards.titlecards.get(i).getTriggers())) pass=true;
            if (pass && mc.player != null) {
                MusicTriggers.logger.info("displaying title card "+i);
                if(!configTitleCards.titlecards.get(i).getTitles().isEmpty()) mc.ingameGUI.displayTitle(TextFormatting.getValueByName(configTitleCards.titlecards.get(i).getTitlecolor()).toString()+configTitleCards.titlecards.get(i).getTitles().get(ThreadLocalRandom.current().nextInt(0, configTitleCards.titlecards.get(i).getTitles().size())), null, 5, 20, 20);
                if(!configTitleCards.titlecards.get(i).getSubTitles().isEmpty()) mc.ingameGUI.displayTitle(null, TextFormatting.getValueByName(configTitleCards.titlecards.get(i).getSubtitlecolor()).toString()+configTitleCards.titlecards.get(i).getSubTitles().get(ThreadLocalRandom.current().nextInt(0, configTitleCards.titlecards.get(i).getSubTitles().size())), 5, 20, 20);
                if(configTitleCards.titlecards.get(i).getPlayonce()) markForDeletion = i;
                break;
            }
        }
        if(markForDeletion!=-1) {
            configTitleCards.titlecards.remove(markForDeletion);
            markForDeletion = -1;
        }
        for (int i : configTitleCards.imagecards.keySet()) {
            boolean pass = false;
            if(MusicPicker.titleCardEvents.containsAll(configTitleCards.imagecards.get(i).getTriggers()) && configTitleCards.imagecards.get(i).getTriggers().containsAll(MusicPicker.titleCardEvents)) pass=true;
            else if(configTitleCards.imagecards.get(i).getVague() && MusicPicker.playableList.containsAll(configTitleCards.imagecards.get(i).getTriggers())) pass=true;
            if (pass && mc.player != null) {
                if(configTitleCards.imagecards.get(i).getName()!=null) {
                    MusicTriggers.logger.info("displaying image card " + configTitleCards.imagecards.get(i).getName());
                    if (!configTitleCards.ismoving.get(i)) {
                        MusicTriggers.logger.info("image card is static");
                        eventsClient.IMAGE_CARD = new ResourceLocation(MusicTriggers.MODID, "textures/" + configTitleCards.imagecards.get(i).getName() + ".png");
                    } else {
                        MusicTriggers.logger.info("image card is moving");
                        eventsClient.pngs = new ArrayList<>();
                        eventsClient.ismoving = true;
                        eventsClient.movingcounter = 0;
                        File folder = new File("." + "/config/MusicTriggers/songs/assets/musictriggers/textures/" + configTitleCards.imagecards.get(i).getName());
                        File[] listOfPNG = folder.listFiles();
                        assert listOfPNG != null;
                        List<String> temp = new ArrayList<>();
                        for (File f : listOfPNG) {
                            temp.add(f.getName().replaceAll(".png", ""));
                        }
                        temp.sort(new Comparator<String>() {
                            public int compare(String o1, String o2) {
                                return extractInt(o1) - extractInt(o2);
                            }

                            int extractInt(String s) {
                                String num = s.replaceAll("\\D", "");
                                return num.isEmpty() ? 0 : Integer.parseInt(num);
                            }
                        });
                        for (int index = 0; index < temp.size(); index++) {
                            eventsClient.pngs.add(index, new ResourceLocation(MusicTriggers.MODID, "textures/" + configTitleCards.imagecards.get(i).getName() + "/" + temp.get(index) + ".png"));
                        }
                        eventsClient.timer = Minecraft.getSystemTime();
                    }
                    eventsClient.curImageIndex = i;
                    eventsClient.activated = true;

                    if (configTitleCards.imagecards.get(i).getPlayonce()) markForDeletion = i;
                    break;
                }
            }
        }
        if(markForDeletion!=-1) configTitleCards.imagecards.get(markForDeletion).setName(null);
        cards = false;
    }

    public static boolean theDecidingFactor(List<String> all, List<String> titlecard, String[] comparison) {
        List<String> updatedComparison = new ArrayList<>();
        boolean cont = false;
        for(String el : comparison) {
            if(titlecard.contains(el)) {
                updatedComparison = Arrays.stream(comparison)
                        .filter(element -> !element.matches(el))
                        .collect(Collectors.toList());
                if(updatedComparison.size()<=0) {
                    return true;
                }
                cont = true;
                break;
            }
        }
        if(cont) return all.containsAll(updatedComparison);
        return false;
    }

    public static String formatSongTime() {
        String ret = "No song playing";
        if(curMusic!=null && ((SoundSystem) sh.sndManager.sndSystem).playing(sh.sndManager.invPlayingSounds.get(curMusic))) {
            try {
                float milliseconds = SoundManipulator.getMillisecondTimeForSource(sh.sndManager.sndSystem, sh.sndManager.invPlayingSounds.get(curMusic));
                if(milliseconds!=Integer.MAX_VALUE) {
                    if (milliseconds == -1) milliseconds = 0;
                    float seconds = milliseconds / 1000f;
                    if (seconds % 60 < 10)
                        ret = (int) (seconds / 60) + ":0" + (int) (seconds % 60) + formatMilliseconds(milliseconds);
                    else ret = (int) (seconds / 60) + ":" + (int) (seconds % 60) + formatMilliseconds(milliseconds);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private static String formatMilliseconds(float milliseconds) {
        if(milliseconds%1000<10) return ":00"+(int)(milliseconds%1000);
        else if(milliseconds%1000<100) return ":0"+(int)(milliseconds%1000);
        else return ":"+(int)(milliseconds%1000);
    }

    private static void removeTrack(String track, int index, List<String> events, ISound playing) {
        if(track!=null) {
            mc.getSoundHandler().stopSound(playing);
            curTrackList = ArrayUtils.remove(curTrackList, index);
            for (String ev : events) {
                String trigger = StringUtils.substringBefore(ev, "-");
                SoundHandler.TriggerSongMap.get(trigger).remove(track);
                if(SoundHandler.TriggerSongMap.get(trigger).isEmpty()) SoundHandler.TriggerSongMap.remove(trigger);
            }
            trackToDelete=null;
            playedEvents = new ArrayList<>();
        }
    }

    private static void changeTrack() {
        eventsClient.GuiCounter = 1;
        String songNum = null;
        for (Map.Entry<String, setVolumeSound> stringListEntry : musicLinker.entrySet()) {
            String checkThis = ((Map.Entry) stringListEntry).getKey().toString();
            if (triggerLinker.get(checkThis) != null) {
                if (theDecidingFactor(MusicPicker.playableList, MusicPicker.titleCardEvents, triggerLinker.get(checkThis)) && mc.player != null) {
                    songNum = checkThis;
                    break;
                }
            }
        }
        if (songNum == null) {
            oncePerTrigger = new ArrayList<>();
            onceUntilEmpty = new ArrayList<>();
            triggerLinker = new HashMap<>();
            musicLinker = new HashMap<>();
            loopLinker = new HashMap<>();
            loopLinkerCounter = new HashMap<>();
            if (MusicPicker.curFade == 0) {
                curTrackList = null;
                if(curMusic!=null) sh.stopSound(curMusic);
                eventsClient.IMAGE_CARD = null;
                eventsClient.fadeCount = 1000;
                eventsClient.timer = 0;
                eventsClient.activated = false;
                eventsClient.ismoving = false;
                cards = true;
            } else {
                fading = true;
                tempFade = MusicPicker.curFade;
                saveVol = mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
            }
        } else {
            curTrackList = null;
            eventsClient.IMAGE_CARD = null;
            eventsClient.fadeCount = 1000;
            eventsClient.timer = 0;
            eventsClient.activated = false;
            eventsClient.ismoving = false;
            cards = true;
            for (Map.Entry<String, setVolumeSound> stringListEntry : musicLinker.entrySet()) {
                String checkThis = ((Map.Entry) stringListEntry).getKey().toString();
                if(loopLinkerCounter.get(checkThis)!=null) {
                    for (int l : loopLinkerCounter.get(checkThis).keySet()) {
                        loopLinkerCounter.get(checkThis).put(l, 0);
                    }
                }
                String temp = sh.sndManager.playingSounds.entrySet().stream().filter(entry -> entry.getValue() == musicLinker.get(checkThis)).map(Map.Entry::getKey).findFirst().orElse(null);
                if (checkThis.matches(songNum)) {
                    musicLinker.get(checkThis).setVolume(volumeLinker.get(songNum));
                    ((SoundSystem)sh.sndManager.sndSystem).setVolume(temp, volumeLinker.get(songNum));
                    curMusic = musicLinker.get(checkThis);
                    curTrackHolder = musicLinker.get(checkThis).getSoundLocation().toString().replaceAll("music.", "").replaceAll("riggers:", "");
                    if (configRegistry.registerDiscs && MusicPicker.player != null) {
                        RegistryHandler.network.sendToServer(new packetCurSong.packetCurSongMessage(curTrack, MusicPicker.player.getUniqueID()));
                    }
                } else {
                    musicLinker.get(checkThis).setVolume(Float.MIN_VALUE);
                    ((SoundSystem)sh.sndManager.sndSystem).setVolume(temp, Float.MIN_VALUE);
                }
            }
        }
        MusicPicker.shouldChange = false;
    }
}