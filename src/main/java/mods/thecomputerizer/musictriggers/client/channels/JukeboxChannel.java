package mods.thecomputerizer.musictriggers.client.channels;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import mods.thecomputerizer.musictriggers.MusicTriggers;
import mods.thecomputerizer.musictriggers.config.ConfigDebug;
import net.minecraft.block.BlockJukebox;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.Level;

import java.util.Objects;

@SideOnly(value = Side.CLIENT)
public class JukeboxChannel implements IChannel {
    private final AudioPlayer player;
    private BlockPos pos;

    public JukeboxChannel() {
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        this.player = playerManager.createPlayer();
        this.player.setVolume(100);
        new ChannelListener(this);
        playerManager.setFrameBufferDuration(1000);
        playerManager.setPlayerCleanupThreshold(Long.MAX_VALUE);
        String resamplingQuality = ConfigDebug.RESAMPLING_QUALITY.toUpperCase();
        playerManager.getConfiguration().setResamplingQuality(EnumUtils.isValidEnum(
                AudioConfiguration.ResamplingQuality.class,resamplingQuality) ?
                AudioConfiguration.ResamplingQuality.valueOf(resamplingQuality) :
                AudioConfiguration.ResamplingQuality.HIGH);
        playerManager.getConfiguration().setOpusEncodingQuality(ConfigDebug.ENCODING_QUALITY);
        playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_PCM_S16_BE);
        MusicTriggers.logExternally(Level.INFO,"Registered jukebox channel");
    }

    @Override
    public String getChannelName() {
        return "jukebox";
    }

    @Override
    public AudioPlayer getPlayer() {
        return this.player;
    }

    public AudioTrack getCurPlaying() {
        return this.player.getPlayingTrack();
    }

    public void checkStopPlaying(boolean reloading) {
        if(isPlaying()) {
            if(isPlaying())  {
                if(reloading) stopTrack();
                else if(this.pos!=null && Minecraft.getMinecraft().world.getTileEntity(this.pos) instanceof BlockJukebox.TileEntityJukebox &&
                        Objects.requireNonNull(Minecraft.getMinecraft().world.getTileEntity(this.pos)).getBlockMetadata()==0)
                    stopTrack();
            }
        }
    }

    public boolean isPlaying() {
        return Objects.nonNull(getCurPlaying());
    }

    public void playTrack(AudioTrack track, BlockPos jukeboxPos) {
        MusicTriggers.logExternally(Level.INFO,"Playing track for jukebox");
        if(Objects.nonNull(track)) {
            track.setPosition(0);
            try {
                if (!this.getPlayer().startTrack(track, false))
                    MusicTriggers.logExternally(Level.ERROR,"Could not start track!");
                else this.pos = jukeboxPos;
            } catch (IllegalStateException e) {
                if (!this.getPlayer().startTrack(track.makeClone(), false))
                    MusicTriggers.logExternally(Level.ERROR,"Could not start track!");
            }
        } else
            MusicTriggers.logExternally(Level.ERROR,"Could not play disc!");
    }

    public void stopTrack() {
        this.getPlayer().stopTrack();
    }

    @Override
    public void onTrackStop(AudioTrackEndReason endReason) {

    }

    @Override
    public void initCache() {

    }
}
