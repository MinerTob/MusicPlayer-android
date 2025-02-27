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
import java.util.Set;

// MusicService.java
public class MusicService extends Service {
    ExoPlayer player;
    List<MediaItem> mediaItems = new ArrayList<>();
    int currentTrackIndex = 0;
    public static final int MODE_NORMAL = Player.REPEAT_MODE_OFF; // 顺序播放
    public static final int MODE_LOOP_ONE = Player.REPEAT_MODE_ONE; // 单曲循环
    public static final int MODE_FAVORITE = 2; // 收藏循环模式
    int playMode = MODE_NORMAL; // 默认普通模式
    boolean prepared = false;
    private List<MediaItem> favoriteMediaItems = new ArrayList<>(); // 收藏歌曲列表

    public interface OnSongChangeListener {
        void onSongChanged();
    }

    private OnSongChangeListener songChangeListener;

    public void setOnSongChangeListener(OnSongChangeListener listener) {
        this.songChangeListener = listener;
    }

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
                public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                    // 每次播放器自动切换到新曲目时，更新 currentTrackIndex
                    currentTrackIndex = player.getCurrentMediaItemIndex();
                    // 当切换到新的歌曲时触发
                    if (songChangeListener != null) {
                        songChangeListener.onSongChanged();
                    }
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
            player.seekTo(0);
            player.play();
        } else if (playMode == MODE_FAVORITE) {
            if (favoriteMediaItems.isEmpty()) {
                setPlayMode(MODE_NORMAL);
                return;
            }
            // 在收藏列表中找到当前歌曲的位置
            String currentSong = getCurrentSongName();
            int currentFavoriteIndex = -1;
            for (int i = 0; i < favoriteMediaItems.size(); i++) {
                if (getFileNameFromMediaItem(favoriteMediaItems.get(i)).equals(currentSong)) {
                    currentFavoriteIndex = i;
                    break;
                }
            }
            // 获取下一首收藏歌曲
            int nextIndex = (currentFavoriteIndex + 1) % favoriteMediaItems.size();
            MediaItem nextItem = favoriteMediaItems.get(nextIndex);

            // 在原始列表中找到对应位置并播放
            for (int i = 0; i < mediaItems.size(); i++) {
                if (mediaItems.get(i).equals(nextItem)) {
                    currentTrackIndex = i;
                    player.seekTo(i, 0);
                    player.play();
                    break;
                }
            }
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % mediaItems.size();
            player.seekTo(currentTrackIndex, 0);
            player.play();
        }
    }

    public void playPreviousTrack() {
        if (playMode == MODE_LOOP_ONE) {
            player.seekTo(0);
            player.play();
        } else if (playMode == MODE_FAVORITE) {
            if (favoriteMediaItems.isEmpty()) {
                setPlayMode(MODE_NORMAL);
                return;
            }
            // 在收藏列表中找到当前歌曲的位置
            String currentSong = getCurrentSongName();
            int currentFavoriteIndex = -1;
            for (int i = 0; i < favoriteMediaItems.size(); i++) {
                if (getFileNameFromMediaItem(favoriteMediaItems.get(i)).equals(currentSong)) {
                    currentFavoriteIndex = i;
                    break;
                }
            }
            // 获取上一首收藏歌曲
            int prevIndex = (currentFavoriteIndex - 1 + favoriteMediaItems.size()) % favoriteMediaItems.size();
            MediaItem prevItem = favoriteMediaItems.get(prevIndex);

            // 在原始列表中找到对应位置并播放
            for (int i = 0; i < mediaItems.size(); i++) {
                if (mediaItems.get(i).equals(prevItem)) {
                    currentTrackIndex = i;
                    player.seekTo(i, 0);
                    player.play();
                    break;
                }
            }
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

        if (playMode == MODE_FAVORITE) {
            // 更新收藏列表
            updateFavoritePlaylist();

            // 如果当前播放的歌曲不在收藏列表中，跳转到最近的收藏歌曲
            String currentSong = getCurrentSongName();
            boolean isCurrentSongFavorite = false;
            int nearestFavoriteIndex = -1;
            int minDistance = Integer.MAX_VALUE;

            // 遍历收藏列表，找到距离当前播放歌曲最近的收藏歌曲
            for (int i = 0; i < mediaItems.size(); i++) {
                String songName = getFileNameFromMediaItem(mediaItems.get(i));
                if (FavoriteManager.isFavorite(songName)) {
                    if (songName.equals(currentSong)) {
                        isCurrentSongFavorite = true;
                        break;
                    }
                    int distance = Math.abs(i - currentTrackIndex);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestFavoriteIndex = i;
                    }
                }
            }

            // 如果当前歌曲不在收藏列表中，跳转到最近的收藏歌曲
            if (!isCurrentSongFavorite && nearestFavoriteIndex != -1) {
                currentTrackIndex = nearestFavoriteIndex;
                player.seekTo(nearestFavoriteIndex, 0);
                player.play();
                if (songChangeListener != null) {
                    songChangeListener.onSongChanged();
                }
            }
        } else if (playMode == MODE_LOOP_ONE) {
            currentTrackIndex = player.getCurrentMediaItemIndex();
        }
    }

    // 新增播放完成处理
    private void handlePlaybackCompleted() {
        if (playMode == MODE_LOOP_ONE) {
            player.seekTo(0);
            player.play();
        } else if (playMode == MODE_FAVORITE) {
            if (favoriteMediaItems.isEmpty()) {
                setPlayMode(MODE_NORMAL);
                return;
            }
            playNextTrack(); // 使用已经修改过的 playNextTrack 方法
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % mediaItems.size();
            player.seekTo(currentTrackIndex, 0);
            player.play();
            if (songChangeListener != null) {
                songChangeListener.onSongChanged();
            }
        }
    }

    // 更新收藏歌曲列表
    public void updateFavoritePlaylist() {
        favoriteMediaItems.clear();
        Set<String> favorites = FavoriteManager.getFavorites();

        if (favorites.isEmpty()) {
            // 如果没有收藏歌曲，切换回普通模式
            setPlayMode(MODE_NORMAL);
            return;
        }

        for (MediaItem item : mediaItems) {
            String fileName = getFileNameFromMediaItem(item);
            if (favorites.contains(fileName)) {
                favoriteMediaItems.add(item);
            }
        }
    }

    // 在 MusicService 中添加一个方法来判断并处理即将结束的歌曲
    public void handleNearCompletion() {
        if (player == null)
            return;

        long currentPosition = player.getCurrentPosition();
        long duration = player.getDuration();

        // 当距离结束还有1秒时
        if (duration - currentPosition <= 1000) {
            if (playMode == MODE_LOOP_ONE) {
                player.seekTo(0);
                player.play();
            } else if (playMode == MODE_FAVORITE) {
                if (favoriteMediaItems.isEmpty()) {
                    setPlayMode(MODE_NORMAL);
                    return;
                }
                playNextTrack();
            } else {
                currentTrackIndex = (currentTrackIndex + 1) % mediaItems.size();
                player.seekTo(currentTrackIndex, 0);
                player.play();
                if (songChangeListener != null) {
                    songChangeListener.onSongChanged();
                }
            }
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
