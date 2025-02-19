package com.example.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// MusicService.java
public class MusicService extends Service {
    ExoPlayer player;
    List<MediaItem> mediaItems = new ArrayList<>();
    int currentTrackIndex = 0;
    public static final int MODE_NORMAL = Player.REPEAT_MODE_OFF; // 顺序播放
    public static final int MODE_LOOP_ONE = Player.REPEAT_MODE_ONE; // 单曲循环
    int playMode = MODE_NORMAL; // 默认普通模式
    boolean prepared = false;

    public void init() {
        if (player == null) {
            player = new ExoPlayer.Builder(MusicService.this).build(); // 创建播放器

            // 添加播放状态监听，同时监控播放列表中文件切换的回调
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        handlePlaybackCompleted();
                    }
                }

                @Override
                public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                    // 每次播放器自动切换到新曲目时，更新 currentTrackIndex
                    currentTrackIndex = player.getCurrentMediaItemIndex();
                }
            });

            try {
                // 获取 assets/music 文件夹中的所有音乐文件
                String[] musicFiles = getAssets().list("music");
                for (String musicFile : musicFiles) {
                    // 构建音乐文件的 URI
                    Uri uri = Uri.parse("asset:///music/" + musicFile);
                    // 创建 MediaItem 并添加到播放列表
                    mediaItems.add(MediaItem.fromUri(uri));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 将所有音乐文件添加到播放器的播放列表中
            player.addMediaItems(mediaItems);
            player.setRepeatMode(playMode); // 顺序播放
            player.prepare();
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public void play() {
        if (player != null) {
            player.play();
        }
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void playMusic(String name) {
        if (player != null) {
            for (int i = 0; i < mediaItems.size(); i++) {
                MediaItem mediaItem = mediaItems.get(i);
                String fileName = getFileNameFromMediaItem(mediaItem);
                if (fileName.equals(name)) {
                    currentTrackIndex = i; // 更新当前播放索引
                    player.seekTo(i, 0);
                    player.play();
                    break;
                }
            }
        }
    }

    public String getFileNameFromMediaItem(MediaItem mediaItem) {
        Uri uri = mediaItem.playbackProperties.uri;
        return uri.getLastPathSegment();
    }

    public void playNextTrack() {
        if (playMode == MODE_LOOP_ONE) {
            player.seekTo(0); // 单曲循环时重置进度
            player.play();
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % mediaItems.size();
            player.seekTo(currentTrackIndex, 0);
            player.play();
        }
    }

    public void playPreviousTrack() {
        if (playMode == MODE_LOOP_ONE) {
            player.seekTo(0); // 单曲循环时重置进度
            player.play();
        } else {
            currentTrackIndex = (currentTrackIndex - 1 + mediaItems.size()) % mediaItems.size();
            player.seekTo(currentTrackIndex, 0);
            player.play();
        }
    }

    public void seekTo(long position) {
        if (player != null) {
            player.seekTo(position);
        }
    }

    // 获取当前播放进度
    public long getContentPosition() {
        if (player != null) {
            return player.getCurrentPosition();
        }
        return 0;
    }

    // 获取歌曲总时长
    public long getDuration() {
        if (player != null) {
            return player.getDuration();
        }
        return 0;
    }

    // 获取当前正在播放的歌曲名
    public String getCurrentSongName() {
        if (currentTrackIndex >= 0 && currentTrackIndex < mediaItems.size()) {
            MediaItem mediaItem = mediaItems.get(currentTrackIndex);
            return getFileNameFromMediaItem(mediaItem);
        }
        return "";
    }

    // 获取当前播放模式
    public int getPlayMode() {
        return player.getRepeatMode();
    }

    // 设置播放模式
    public void setPlayMode(int mode) {
        playMode = mode;
        player.setRepeatMode(playMode);
        // 当设置为单曲循环时强制更新索引
        if (playMode == MODE_LOOP_ONE) {
            currentTrackIndex = player.getCurrentMediaItemIndex();
        }
    }

    // 新增播放完成处理
    private void handlePlaybackCompleted() {
        if (playMode == MODE_LOOP_ONE) {
            // 单曲循环模式下，重置当前歌曲的播放进度
            player.seekTo(0);
            player.play();
        } else {
            // 列表循环模式：自动切换到下一首
            // 如果当前已经是最后一首，则 (currentTrackIndex + 1) % mediaItems.size() 会等于 0，回到第一首
            currentTrackIndex = (currentTrackIndex + 1) % mediaItems.size();
            player.seekTo(currentTrackIndex, 0);
            player.play();
        }
    }

    // 绑定 MusicService
    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.stop();
            // 添加释放前检查
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
            player = null;
        }
    }
}
