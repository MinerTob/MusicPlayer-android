package com.example.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// MusicService.java
public class MusicService extends Service {
    private static final String TAG = "MusicService";
    
    // 广播相关常量
    public static final String ACTION_PLAYING_UPDATE = "com.example.musicplayer.PLAYING_UPDATE";
    public static final String EXTRA_PLAYING_POSITION = "playing_position";
    public static final String EXTRA_PLAYING_DURATION = "playing_duration";
    
    // UI更新相关
    private Handler uiHandler;
    private Runnable uiUpdateRunnable;
    private static final long UI_UPDATE_INTERVAL = 1000; // 1秒更新一次UI
    
    ExoPlayer player;
    List<MediaItem> mediaItems = new ArrayList<>();
    int currentTrackIndex = 0;
    private String currentSongName = ""; // 当前播放歌曲名称
    public static final int MODE_NORMAL = Player.REPEAT_MODE_OFF; // 顺序播放
    public static final int MODE_LOOP_ONE = Player.REPEAT_MODE_ONE; // 单曲循环
    public static final int MODE_FAVORITE = 2; // 收藏循环模式
    int playMode = MODE_NORMAL; // 默认普通模式
    boolean prepared = false;
    private List<MediaItem> favoriteMediaItems = new ArrayList<>(); // 收藏歌曲列表
    private FileSyncManager fileSyncManager;
    private boolean isInitializing = false;
    private LyricManager.Lyric currentLyric; // 添加缺少的currentLyric变量
    private MusicUiController uiController;
    private List<String> playList;

    public interface OnSongChangeListener {
        void onSongChanged();
    }

    private OnSongChangeListener songChangeListener;

    public void setOnSongChangeListener(OnSongChangeListener listener) {
        this.songChangeListener = listener;
    }
    
    // UI控制器接口，用于更新界面
    public interface MusicUiController {
        // 更新播放状态
        default void updatePlayState(boolean isPlaying) {}
        
        // 更新当前歌曲信息
        default void updateSongInfo(String title, String singer) {}
        
        // 更新进度条
        default void updateProgress(long currentPosition, long duration) {}
        
        // 更新歌词
        default void updateLyric(String currentLyric, String nextLyric) {}
    }
    
    public void setUiController(MusicUiController controller) {
        this.uiController = controller;
    }

    private void init() {
        if (player == null && !isInitializing) {
            isInitializing = true;
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
                    if (mediaItem != null) {
                        currentTrackIndex = player.getCurrentMediaItemIndex();
                        currentSongName = getFileNameFromMediaItem(mediaItem);
                        Log.d(TAG, "媒体项切换: 索引=" + currentTrackIndex + ", 歌曲=" + currentSongName);
                        if (songChangeListener != null) {
                            songChangeListener.onSongChanged();
                        }
                        currentLyric = null; // 切换歌曲时清空歌词缓存
                    }
                }
            });

            // 设置文件同步状态监听器
            fileSyncManager.setSyncStatusListener(new FileSyncManager.SyncStatusListener() {
                @Override
                public void onSyncStarted() {
                    Log.d(TAG, "开始同步文件");
                }

                @Override
                public void onSyncProgress(int current, int total, String filename) {
                    Log.d(TAG, "同步进度: " + current + "/" + total + " - " + filename);
                }

                @Override
                public void onSyncProgress(String fileName, int progress) {
                    Log.d(TAG, "文件 " + fileName + " 下载进度: " + progress + "%");
                }

                @Override
                public void onSyncCompleted(int total) {
                    Log.d(TAG, "文件同步完成，共同步 " + total + " 个文件");
                    // 同步完成后刷新音乐列表
                    refreshMusicList();
                }

                @Override
                public void onSyncError(String errorMessage) {
                    Log.e(TAG, "文件同步错误: " + errorMessage);
                    // 同步出错也尝试刷新音乐列表
                    refreshMusicList();
                }
            });
            
            // 开始同步文件
            fileSyncManager.startSync();
            
            loadPlayList();
            
            // 设置播放模式 (修改这里，确保正确设置重复模式)
            player.setRepeatMode(playMode == MODE_LOOP_ONE ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
            prepared = true;
            player.prepare();
            isInitializing = false;
        }
    }

    /**
     * 加载音乐文件，优先使用已下载的文件，如果没有则使用assets
     */
    private void loadPlayList() {
        try {
            // 获取assets音乐目录下的文件列表
            String[] musicFiles = getAssets().list("music");
            if (musicFiles != null) {
                Log.d(TAG, "Assets中找到音乐文件: " + musicFiles.length + "个");
                for (String name : musicFiles) {
                    Log.d(TAG, "Assets中的文件: " + name);
                }
            } else {
                Log.e(TAG, "Assets中未找到音乐文件");
            }
            
            if (musicFiles.length > 0) {
                // 清空当前列表
                mediaItems.clear();
                
                // 添加音乐文件到播放列表
                for (String fileName : musicFiles) {
                    // 只添加音乐文件
                    if (fileName.endsWith(".mp3") || fileName.endsWith(".flac") || 
                        fileName.endsWith(".m4a") || fileName.endsWith(".wav") || 
                        fileName.endsWith(".ogg")) {
                        // 使用ContentUri而不是asset:///格式，这样更可靠
                        Uri uri = Uri.parse("asset:///music/" + fileName);
                        MediaItem mediaItem = MediaItem.fromUri(uri);
                        Log.d(TAG, "添加资源文件到播放列表: " + fileName + ", URI: " + uri);
                        mediaItems.add(mediaItem);
                    }
                }
                
                // 也加载下载目录中的音乐文件
                File localMusicDir = new File(getFilesDir(), "music");
                Log.d(TAG, "检查本地音乐目录: " + localMusicDir.getAbsolutePath());
                if (localMusicDir.exists() && localMusicDir.isDirectory()) {
                    File[] localFiles = localMusicDir.listFiles();
                    if (localFiles != null) {
                        Log.d(TAG, "本地音乐目录中找到文件: " + localFiles.length + "个");
                        for (File file : localFiles) {
                            Log.d(TAG, "本地文件: " + file.getName() + ", 大小: " + file.length() + "字节");
                            if (file.isFile() && (file.getName().endsWith(".mp3") || 
                                file.getName().endsWith(".flac") || 
                                file.getName().endsWith(".m4a") || 
                                file.getName().endsWith(".wav") || 
                                file.getName().endsWith(".ogg"))) {
                                Uri uri = Uri.fromFile(file);
                                MediaItem mediaItem = MediaItem.fromUri(uri);
                                Log.d(TAG, "添加本地文件到播放列表: " + file.getName() + ", URI: " + uri);
                                // 避免重复添加同名文件
                                boolean isDuplicate = false;
                                for (MediaItem item : mediaItems) {
                                    if (getFileNameFromMediaItem(item).equals(file.getName())) {
                                        isDuplicate = true;
                                        break;
                                    }
                                }
                                if (!isDuplicate) {
                                    mediaItems.add(mediaItem);
                                }
                            }
                        }
                    }
                }
                
                Log.d(TAG, "加载了 " + mediaItems.size() + " 个音乐文件");
                
                // 更新收藏歌曲列表
                updateFavoritePlaylist();
            }
        } catch (IOException e) {
            Log.e(TAG, "加载播放列表失败", e);
        }
    }

    private void loadMusicFromFiles() {
        try {
            File musicDir = new File(getFilesDir(), "music");
            if (musicDir.exists()) {
                File[] files = musicDir.listFiles();
                if (files != null) {
                    Log.d(TAG, "本地目录中找到音乐文件: " + files.length + "个");
                    for (File file : files) {
                        String fileName = file.getName();
                        String filePath = file.getAbsolutePath();
                        
                        // 记录日志，特别是中文文件名
                        Log.d(TAG, "本地文件: 名称=" + fileName + 
                               ", 路径=" + filePath + 
                               ", 字节大小=" + file.length());
                        
                        String extension = getFileExtension(fileName).toLowerCase();
                        if (isSupportedAudioFile(extension)) {
                            MediaItem mediaItem = buildMediaItem(filePath, fileName);
                            mediaItems.add(mediaItem);
                        }
                    }
                } else {
                    Log.e(TAG, "本地音乐目录存在但无法列出文件");
                }
            } else {
                Log.d(TAG, "本地音乐目录不存在，将创建该目录");
                boolean created = musicDir.mkdirs();
                Log.d(TAG, "创建本地音乐目录" + (created ? "成功" : "失败"));
            }
        } catch (Exception e) {
            Log.e(TAG, "加载本地音乐文件时出错: " + e.getMessage(), e);
        }
    }

    private MediaItem buildMediaItem(String filePath, String displayName) {
        Uri uri = Uri.parse(filePath);
        if (!filePath.startsWith("http")) {
            uri = Uri.fromFile(new File(filePath));
        }
        
        // 添加日志记录文件路径和URI，便于调试
        Log.d(TAG, "构建MediaItem: 路径=" + filePath + ", URI=" + uri + ", 显示名=" + displayName);
        
        // 检查文件是否存在（对于本地文件）
        if (!filePath.startsWith("http") && !new File(filePath).exists()) {
            Log.e(TAG, "警告: 文件不存在: " + filePath);
        }
        
        return new MediaItem.Builder()
                .setUri(uri)
                .setMediaId(filePath)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(displayName)
                        .build())
                .build();
    }

    /**
     * 刷新音乐列表，重新加载并更新播放器的媒体项列表
     * 在新音乐文件下载完成后调用，确保播放器能立即访问新下载的文件
     */
    public void refreshMusicList() {
        Log.d(TAG, "刷新音乐列表");
        
        if (player == null) {
            Log.d(TAG, "播放器未初始化，无法刷新列表");
            return;
        }
        
        // 确保在主线程中执行ExoPlayer相关操作
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.d(TAG, "refreshMusicList被非主线程调用，转到主线程执行");
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(this::refreshMusicList);
            return;
        }
        
        // 保存当前播放状态
        boolean wasPlaying = player.isPlaying();
        String currentSongName = getCurrentSongName();
        long currentPosition = player.getCurrentPosition();
        
        // 保存当前的播放列表
        List<MediaItem> oldMediaItems = new ArrayList<>(mediaItems);
        
        try {
            // 清空当前列表并重新加载
            mediaItems.clear();
            
            // 重新加载音乐文件列表
            String[] musicFiles = getAssets().list("music");
            if (musicFiles != null) {
                Log.d(TAG, "Assets中找到音乐文件: " + musicFiles.length + "个");
                for (String name : musicFiles) {
                    Log.d(TAG, "Assets中的文件: " + name);
                }
            } else {
                Log.e(TAG, "Assets中未找到音乐文件");
            }
            
            if (musicFiles.length > 0) {
                // 添加assets中的音乐文件到播放列表
                for (String fileName : musicFiles) {
                    // 只添加音乐文件
                    if (fileName.endsWith(".mp3") || fileName.endsWith(".flac") || 
                        fileName.endsWith(".m4a") || fileName.endsWith(".wav") || 
                        fileName.endsWith(".ogg")) {
                        // 使用ContentUri而不是asset:///格式，这样更可靠
                        Uri uri = Uri.parse("asset:///music/" + fileName);
                        MediaItem mediaItem = MediaItem.fromUri(uri);
                        Log.d(TAG, "添加资源文件到播放列表: " + fileName + ", URI: " + uri);
                        mediaItems.add(mediaItem);
                    }
                }
            }
            
            // 加载下载目录中的音乐文件
            File localMusicDir = new File(getFilesDir(), "music");
            Log.d(TAG, "检查本地音乐目录: " + localMusicDir.getAbsolutePath());
            if (localMusicDir.exists() && localMusicDir.isDirectory()) {
                File[] localFiles = localMusicDir.listFiles();
                if (localFiles != null) {
                    Log.d(TAG, "本地音乐目录中找到文件: " + localFiles.length + "个");
                    for (File file : localFiles) {
                        Log.d(TAG, "本地文件: " + file.getName() + ", 大小: " + file.length() + "字节");
                        if (file.isFile() && (file.getName().endsWith(".mp3") || 
                            file.getName().endsWith(".flac") || 
                            file.getName().endsWith(".m4a") || 
                            file.getName().endsWith(".wav") || 
                            file.getName().endsWith(".ogg"))) {
                            Uri uri = Uri.fromFile(file);
                            MediaItem mediaItem = MediaItem.fromUri(uri);
                            Log.d(TAG, "添加本地文件到播放列表: " + file.getName() + ", URI: " + uri);
                            // 避免重复添加同名文件
                            boolean isDuplicate = false;
                            for (MediaItem item : mediaItems) {
                                if (getFileNameFromMediaItem(item).equals(file.getName())) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            if (!isDuplicate) {
                                mediaItems.add(mediaItem);
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "刷新后的音乐列表包含 " + mediaItems.size() + " 个文件");
            
            // 如果列表为空，直接返回
            if (mediaItems.isEmpty()) {
                Log.w(TAG, "刷新后的音乐列表为空");
                return;
            }
            
            // 停止播放并清空播放器
            player.stop();
            player.clearMediaItems();
            
            // 向播放器添加新的媒体项
            player.addMediaItems(mediaItems);
            
            // 准备播放器
            player.prepare();
            
            // 尝试恢复之前的播放状态
            if (!currentSongName.isEmpty() && !currentSongName.equals("unknown.mp3")) {
                boolean found = false;
                for (int i = 0; i < mediaItems.size(); i++) {
                    if (getFileNameFromMediaItem(mediaItems.get(i)).equals(currentSongName)) {
                        currentTrackIndex = i;
                        player.seekTo(i, currentPosition);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    // 如果之前的歌曲不在新列表中，从头开始播放
                    currentTrackIndex = 0;
                    player.seekTo(0, 0);
                }
            } else {
                // 如果之前没有播放歌曲，设置为第一首
                currentTrackIndex = 0;
                player.seekTo(0, 0);
            }
            
            // 恢复播放状态
            if (wasPlaying) {
                player.play();
            }
            
            // 更新收藏歌曲列表
            updateFavoritePlaylist();
            
            // 通知UI更新
            if (uiController != null) {
                uiController.updateSongInfo(getCurrentSongName(), "");
                uiController.updatePlayState(player.isPlaying());
                uiController.updateProgress(player.getCurrentPosition(), player.getDuration());
            }
            
            // 通知歌曲变化
            if (songChangeListener != null) {
                songChangeListener.onSongChanged();
            }
            
            Log.d(TAG, "音乐列表刷新完成");
        } catch (IOException e) {
            Log.e(TAG, "刷新播放列表失败", e);
            
            // 发生错误时尝试恢复旧列表
            if (!oldMediaItems.isEmpty()) {
                mediaItems.clear();
                mediaItems.addAll(oldMediaItems);
                player.clearMediaItems();
                player.addMediaItems(mediaItems);
                player.prepare();
                
                // 尝试恢复播放位置
                for (int i = 0; i < mediaItems.size(); i++) {
                    if (getFileNameFromMediaItem(mediaItems.get(i)).equals(currentSongName)) {
                        currentTrackIndex = i;
                        player.seekTo(i, currentPosition);
                        break;
                    }
                }
                
                if (wasPlaying) {
                    player.play();
                }
            }
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public void play() {
        if (player != null && prepared) {
            player.play();
            if (uiController != null) {
                uiController.updatePlayState(true);
            }
        }
    }

    public void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
            if (uiController != null) {
                uiController.updatePlayState(false);
            }
        }
    }

    public void playMusic(String name) {
        Log.d(TAG, "尝试播放音乐: " + name);
        
        // 检查媒体列表
        if (mediaItems.isEmpty()) {
            Log.e(TAG, "媒体列表为空，尝试重新加载后播放");
            
            try {
                refreshMusicList();
            } catch (Exception e) {
                Log.e(TAG, "刷新媒体列表失败: " + e.getMessage(), e);
            }
            
            // 检查重新加载后的状态
            if (mediaItems.isEmpty()) {
                Log.e(TAG, "重新加载后媒体列表仍为空，无法播放");
                
                // 文件系统检查
                File musicDir = new File(getFilesDir(), "music");
                if (musicDir.exists()) {
                    File[] files = musicDir.listFiles();
                    if (files != null) {
                        Log.d(TAG, "文件系统检查: 音乐目录存在，包含 " + files.length + " 个文件");
                        for (File file : files) {
                            Log.d(TAG, "文件: " + file.getName() + ", 大小: " + file.length());
                        }
                    } else {
                        Log.e(TAG, "文件系统检查: 音乐目录存在但为空或无法访问");
                    }
                } else {
                    Log.e(TAG, "文件系统检查: 音乐目录不存在");
                }
                return;
            }
        }
        
        // 记录当前的歌曲名，即使未找到也更新这个值
        currentSongName = name;
        boolean found = false;
        
        // 打印所有的MediaItem进行调试
        Log.d(TAG, "当前媒体列表中的所有音频文件:");
        for (int i = 0; i < mediaItems.size(); i++) {
            String fileName = getFileNameFromMediaItem(mediaItems.get(i));
            Log.d(TAG, i + ": " + fileName);
        }
        
        // 尝试查找匹配的音乐文件
        for (int i = 0; i < mediaItems.size(); i++) {
            MediaItem mediaItem = mediaItems.get(i);
            String fileName = getFileNameFromMediaItem(mediaItem);
            
            // 比较文件名（不区分大小写）
            if (fileName.equalsIgnoreCase(name)) {
                Log.d(TAG, "找到匹配的文件: " + name + " 位于索引 " + i);
                currentTrackIndex = i; // 更新当前播放索引
                player.seekTo(i, 0);
                player.prepare();
                player.play();
                found = true;
                break;
            }
        }
        
        if (!found) {
            Log.e(TAG, "在媒体列表中未找到: " + name);
            // 如果没找到，重新加载列表再尝试一次
            refreshMusicList();
            
            // 再次打印所有的MediaItem进行调试
            Log.d(TAG, "刷新后媒体列表中的所有音频文件:");
            for (int i = 0; i < mediaItems.size(); i++) {
                String fileName = getFileNameFromMediaItem(mediaItems.get(i));
                Log.d(TAG, i + ": " + fileName);
            }
            
            for (int i = 0; i < mediaItems.size(); i++) {
                MediaItem mediaItem = mediaItems.get(i);
                String fileName = getFileNameFromMediaItem(mediaItem);
                if (fileName.equalsIgnoreCase(name)) {
                    Log.d(TAG, "刷新后找到匹配的文件: " + name);
                    currentTrackIndex = i;
                    player.seekTo(i, 0);
                    player.prepare();
                    player.play();
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                Log.e(TAG, "即使刷新后也未找到文件: " + name);
                // 尝试播放第一首歌曲
                if (!mediaItems.isEmpty()) {
                    Log.d(TAG, "未找到指定歌曲，默认播放第一首");
                    player.seekTo(0, 0);
                    player.prepare();
                    player.play();
                    currentTrackIndex = 0;
                }
            }
        }
        
        // 通知UI更新
        updateUiState();
    }

    public void playNextTrack() {
        // 确保在主线程中执行ExoPlayer相关操作
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.d(TAG, "playNextTrack被非主线程调用，转到主线程执行");
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(this::playNextTrack);
            return;
        }
        
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
            // 检查播放列表是否为空
            if (mediaItems == null || mediaItems.isEmpty()) {
                Log.w(TAG, "playNextTrack: 播放列表为空，无法播放下一首");
                return;
            }
            
            currentTrackIndex = (currentTrackIndex + 1) % mediaItems.size();
            player.seekTo(currentTrackIndex, 0);
            player.play();
        }
    }

    public void playPreviousTrack() {
        // 确保在主线程中执行ExoPlayer相关操作
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.d(TAG, "playPreviousTrack被非主线程调用，转到主线程执行");
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(this::playPreviousTrack);
            return;
        }
        
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
            // 检查播放列表是否为空
            if (mediaItems == null || mediaItems.isEmpty()) {
                Log.w(TAG, "playPreviousTrack: 播放列表为空，无法播放上一首");
                return;
            }
            
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

    // 获取当前播放进度 (兼容性方法，与getContentPosition功能相同)
    public long getCurrentPosition() {
        return getContentPosition();
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
        try {
            if (player != null && currentTrackIndex >= 0 && currentTrackIndex < mediaItems.size()) {
                // 确保在主线程中访问ExoPlayer
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    Log.w(TAG, "getCurrentSongName在非主线程调用，当前线程: " + Thread.currentThread().getName());
                    // 非阻塞情况下，返回当前缓存的歌名
                    if (currentSongName != null && !currentSongName.isEmpty() && !currentSongName.equals("unknown.mp3")) {
                        return currentSongName;
                    }
                }
                
                MediaItem mediaItem = mediaItems.get(currentTrackIndex);
                String fileName = getFileNameFromMediaItem(mediaItem);
                
                // 确保文件名不为空
                if (fileName != null && !fileName.isEmpty() && !fileName.equals("unknown.mp3")) {
                    // 更新当前歌曲名缓存
                    currentSongName = fileName;
                    return fileName;
                }
                
                // 如果获取到的文件名为unknown.mp3，记录详细的调试信息
                if (fileName.equals("unknown.mp3")) {
                    Log.w(TAG, "获取到未知文件名，MediaItem详情: " + mediaItem);
                }
            }
            
            // 如果获取不到有效的歌曲名，记录错误并返回一个可识别的默认值
            Log.e(TAG, "无法获取当前歌曲名称，播放列表状态：size=" + 
                  (mediaItems != null ? mediaItems.size() : 0) + 
                  ", currentTrackIndex=" + currentTrackIndex + 
                  ", 当前线程: " + Thread.currentThread().getName());
            
            // 如果此前已有缓存的歌曲名，优先返回缓存值
            if (currentSongName != null && !currentSongName.isEmpty() && !currentSongName.equals("unknown.mp3")) {
                return currentSongName;
            }
            
            // 最后的备选，返回默认值
            return "unknown.flac"; // 更改默认扩展名为.flac
        } catch (Exception e) {
            Log.e(TAG, "getCurrentSongName出现异常", e);
            return "unknown.flac"; // 更改默认扩展名为.flac
        }
    }

    // 获取当前播放模式
    public int getPlayMode() {
        return player.getRepeatMode();
    }

    // 设置播放模式
    public void setPlayMode(int mode) {
        Log.d(TAG, "切换播放模式: " + playMode + " -> " + mode);
        
        // 保存旧模式用于比较
        int oldMode = playMode;
        playMode = mode;
        
        // 为播放器设置合适的重复模式
        if (playMode == MODE_LOOP_ONE) {
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
            Log.d(TAG, "设置ExoPlayer为单曲循环模式");
        } else {
            // 收藏模式和普通模式都使用REPEAT_MODE_OFF，因为我们会在handlePlaybackCompleted中自己处理
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
            Log.d(TAG, "设置ExoPlayer为关闭循环模式");
        }

        if (playMode == MODE_FAVORITE) {
            // 更新收藏列表
            updateFavoritePlaylist();
            
            // 检查收藏列表是否为空
            if (favoriteMediaItems.isEmpty()) {
                Log.w(TAG, "收藏列表为空，切换回普通模式");
                playMode = MODE_NORMAL;
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                if (songChangeListener != null) {
                    songChangeListener.onSongChanged();
                }
                return;
            }

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
                Log.d(TAG, "当前歌曲不在收藏列表中，跳转到最近的收藏歌曲");
                currentTrackIndex = nearestFavoriteIndex;
                player.seekTo(nearestFavoriteIndex, 0);
                player.prepare();
                if (isPlaying()) {
                    player.play();
                }
                if (songChangeListener != null) {
                    songChangeListener.onSongChanged();
                }
            }
        }
        
        // 更新UI状态
        updateUiState();
        
        Log.d(TAG, "播放模式切换完成: " + mode + ", ExoPlayer RepeatMode: " + player.getRepeatMode());
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
            // 检查播放列表是否为空
            if (mediaItems == null || mediaItems.isEmpty()) {
                Log.w(TAG, "handlePlaybackCompleted: 播放列表为空，无法播放下一首");
                return;
            }
            
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
            playMode = MODE_NORMAL;
            player.setRepeatMode(playMode);
            if (songChangeListener != null) {
                songChangeListener.onSongChanged();
            }
            return;
        }

        for (MediaItem item : mediaItems) {
            String fileName = getFileNameFromMediaItem(item);
            if (favorites.contains(fileName)) {
                favoriteMediaItems.add(item);
            }
        }

        // 通知 UI 刷新更新后的收藏列表
        if (songChangeListener != null) {
            songChangeListener.onSongChanged();
        }
    }

    // 在 MusicService 中添加一个方法来判断并处理即将结束的歌曲
    public void handleNearCompletion() {
        if (player == null)
            return;

        // 确保在主线程中执行ExoPlayer相关操作
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.d(TAG, "handleNearCompletion被非主线程调用，转到主线程执行");
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(this::handleNearCompletion);
            return;
        }
        
        long currentPosition = player.getCurrentPosition();
        long duration = player.getDuration();

        // 检查播放列表是否为空
        if (mediaItems == null || mediaItems.isEmpty()) {
            Log.w(TAG, "handleNearCompletion: 播放列表为空，无法播放下一首");
            return;
        }

        // 当距离结束还有1秒时
        if (duration > 0 && duration - currentPosition <= 1000) {
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
    
    // 发送当前播放位置的广播
    private void sendPlayingUpdateBroadcast() {
        Intent intent = new Intent(ACTION_PLAYING_UPDATE);
        intent.putExtra(EXTRA_PLAYING_POSITION, player.getCurrentPosition());
        intent.putExtra(EXTRA_PLAYING_DURATION, player.getDuration());
        sendBroadcast(intent);
    }
    
    // 为兼容性添加的方法，主要由MainActivity调用
    public void updateSongInfo() {
        if (uiController != null && currentTrackIndex >= 0 && currentTrackIndex < mediaItems.size()) {
            String songName = getFileNameFromMediaItem(mediaItems.get(currentTrackIndex));
            // 尝试从歌词中获取歌手信息
            String singer = LyricManager.getSinger(songName, this);
            
            // 去掉扩展名显示
            int dotIndex = songName.lastIndexOf('.');
            String title = dotIndex > 0 ? songName.substring(0, dotIndex) : songName;
            
            // 将下划线替换为空格，确保和MainActivity中的处理一致
            title = title.replace("_", " ");
            
            uiController.updateSongInfo(title, singer);
        }
    }
    
    // 为兼容性添加的方法，主要由MainActivity调用
    public void updateProgress() {
        if (uiController != null && player != null) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            
            uiController.updateProgress(currentPosition, duration);
            
            // 更新歌词显示
            updateLyric(currentPosition);
        }
    }
    
    /**
     * 刷新音乐列表，加载新下载的文件
     */
    // 移除重复的refreshMusicList()方法
    
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
        playList = new ArrayList<>();

        // 初始化文件同步管理器，但不立即启动同步（会在init方法中启动）
        fileSyncManager = new FileSyncManager(this);
        
        // 启动UI更新定时器
        startUiUpdateTimer();
        
        // 初始化播放器
        init();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MusicService.onStartCommand");
        
        // 检查Intent是否包含上次播放信息
        if (intent != null) {
            String lastPlayedSong = intent.getStringExtra("last_played_song");
            long lastPosition = intent.getLongExtra("last_position", 0);
            boolean autoPlay = intent.getBooleanExtra("auto_play", false);
            
            Log.d(TAG, "接收到上次播放信息: " + 
                  "歌曲=" + (lastPlayedSong != null ? lastPlayedSong : "null") + 
                  ", 位置=" + lastPosition + 
                  ", 自动播放=" + autoPlay);
            
            if (lastPlayedSong != null && !lastPlayedSong.isEmpty()) {
                // 尝试播放上次的歌曲，但延迟一点执行，确保播放器已完成初始化
                uiHandler.postDelayed(() -> {
                    try {
                        // 播放上次的歌曲
                        playMusic(lastPlayedSong);
                        
                        // 设置进度
                        if (lastPosition > 0) {
                            seekTo(lastPosition);
                        }
                        
                        // 如果设置自动播放，则开始播放
                        if (autoPlay) {
                            play();
                        } else {
                            // 否则暂停在上次位置
                            pause();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "恢复播放状态失败: " + e.getMessage(), e);
                    }
                }, 500); // 延迟500毫秒，确保播放器已完成初始化
            }
        }
        
        // 确保服务不会被系统自动停止
        return START_STICKY;
    }
    
    /**
     * 启动UI更新定时器
     */
    private void startUiUpdateTimer() {
        uiHandler = new Handler(Looper.getMainLooper());
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUiState();
                uiHandler.postDelayed(this, UI_UPDATE_INTERVAL);
            }
        };
        uiHandler.post(uiUpdateRunnable);
    }
    
    /**
     * 停止UI更新定时器
     */
    private void stopUiUpdateTimer() {
        if (uiHandler != null && uiUpdateRunnable != null) {
            uiHandler.removeCallbacks(uiUpdateRunnable);
        }
    }
    
    /**
     * 更新UI状态
     */
    private void updateUiState() {
        try {
            if (player == null) {
                Log.e(TAG, "updateUiState: 播放器为空");
                return;
            }
            
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            boolean isPlaying = player.isPlaying();
            
            Log.d(TAG, "更新UI状态: 位置=" + currentPosition + ", 时长=" + duration + ", 播放状态=" + isPlaying);
            
            // 更新UI控制器状态
            if (uiController != null) {
                uiController.updateProgress(currentPosition, duration);
                uiController.updatePlayState(isPlaying);
                
                // 只有当我们有当前歌曲和歌曲索引有效时才更新歌曲信息
                if (currentTrackIndex >= 0 && currentTrackIndex < mediaItems.size()) {
                    String songName = getFileNameFromMediaItem(mediaItems.get(currentTrackIndex));
                    // 尝试从歌词中获取歌手信息
                    String singer = LyricManager.getSinger(songName, this);
                    
                    // 去掉扩展名显示
                    int dotIndex = songName.lastIndexOf('.');
                    String title = dotIndex > 0 ? songName.substring(0, dotIndex) : songName;
                    
                    // 将下划线替换为空格，确保和MainActivity中的处理一致
                    title = title.replace("_", " ");
                    
                    uiController.updateSongInfo(title, singer);
                }
                
                // 更新歌词
                updateLyric(currentPosition);
            }
            
            // 发送广播更新
            sendPlayingUpdateBroadcast();
        } catch (Exception e) {
            Log.e(TAG, "UI更新失败", e);
        }
    }
    
    // 修正歌词管理相关代码
    private void updateLyric(long currentPosition) {
        if (uiController != null && currentTrackIndex >= 0 && currentTrackIndex < mediaItems.size()) {
            String currentSong = getCurrentSongName();
            
            if (currentLyric == null) {
                // 加载歌词
                currentLyric = LyricManager.getLyric(currentSong, this);
            }
            
            // 查找当前时间点对应的歌词
            if (currentLyric != null && currentLyric.lyrics != null && !currentLyric.lyrics.isEmpty()) {
                int index = currentLyric.findLyricIndex(currentPosition);
                if (index >= 0 && index < currentLyric.lyrics.size()) {
                    String currentLyricText = currentLyric.lyrics.get(index);
                    
                    // 获取下一句歌词
                    String nextLyricText = "";
                    if (index + 1 < currentLyric.lyrics.size()) {
                        nextLyricText = currentLyric.lyrics.get(index + 1);
                    }
                    
                    uiController.updateLyric(currentLyricText, nextLyricText);
                }
            } else {
                // 无歌词时显示空白
                uiController.updateLyric("", "");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止UI更新定时器
        stopUiUpdateTimer();
        
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
    
    /**
     * 获取文件同步管理器
     * @return 文件同步管理器实例
     */
    public FileSyncManager getFileSyncManager() {
        return fileSyncManager;
    }

    private String getFileNameFromMediaItem(MediaItem mediaItem) {
        // 处理空媒体项
        if (mediaItem == null) {
            Log.e(TAG, "getFileNameFromMediaItem: 媒体项为空");
            return "unknown.flac";
        }
        
        try {
            Uri uri = null;
            
            // 先尝试从 localConfiguration 获取 uri (ExoPlayer 2.19.1 中的方式)
            if (mediaItem.localConfiguration != null) {
                uri = mediaItem.localConfiguration.uri;
            } 
            // 再尝试从 playbackProperties 获取 uri (早期 ExoPlayer 版本的方式)
            else if (mediaItem.playbackProperties != null) {
                uri = mediaItem.playbackProperties.uri;
            }
            // 最后尝试从 mediaId 获取
            else if (mediaItem.mediaId != null && !mediaItem.mediaId.isEmpty()) {
                try {
                    uri = Uri.parse(mediaItem.mediaId);
                } catch (Exception e) {
                    Log.e(TAG, "解析mediaId为URI时出错: " + mediaItem.mediaId, e);
                }
            }
            
            if (uri != null) {
                // 处理资源文件 (asset:///)
                if (uri.toString().startsWith("asset:///")) {
                    String assetPath = uri.toString().substring("asset:///".length());
                    if (assetPath.startsWith("music/")) {
                        String fileName = assetPath.substring("music/".length());
                        return fileName;
                    }
                }
                
                // 处理文件URI (file:///)
                String path = uri.getPath();
                if (path != null) {
                    int lastSlashIndex = path.lastIndexOf('/');
                    if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
                        String fileName = path.substring(lastSlashIndex + 1);
                        return fileName;
                    }
                }
                
                // 尝试获取最后一个路径段
                String lastPathSegment = uri.getLastPathSegment();
                if (lastPathSegment != null) {
                    return lastPathSegment;
                }
                
                // 如果还是获取不到，使用URI的字符串表示
                String uriString = uri.toString();
                
                // 如果URI字符串包含文件名部分，提取出来
                int fileNameStart = uriString.lastIndexOf('/');
                if (fileNameStart >= 0 && fileNameStart < uriString.length() - 1) {
                    String fileName = uriString.substring(fileNameStart + 1);
                    // 确保文件名有扩展名
                    if (!fileName.contains(".")) {
                        fileName += ".flac";  // 添加默认扩展名
                    }
                    return fileName;
                }
            }
            
            // 只在无法获取文件名时记录媒体项的字符串表示
            String mediaItemString = mediaItem.toString();
            
            // 查找可能的文件名模式，例如[song.flac]或"file.mp3"等
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\w\\-]+\\.(flac|mp3|m4a|wav|ogg))");
            java.util.regex.Matcher matcher = pattern.matcher(mediaItemString);
            if (matcher.find()) {
                String fileName = matcher.group(1);
                return fileName;
            }
        } catch (Exception e) {
            Log.e(TAG, "getFileNameFromMediaItem异常", e);
        }
        
        // 只在返回默认值时记录错误
        Log.e(TAG, "无法从媒体项获取文件名，返回默认值");
        return "unknown.flac";
    }

    /**
     * 判断文件扩展名是否为支持的音频格式
     */
    private boolean isSupportedAudioFile(String extension) {
        if (extension == null) return false;
        extension = extension.toLowerCase();
        return extension.equals("mp3") || extension.equals("flac") || 
               extension.equals("m4a") || extension.equals("wav") || 
               extension.equals("ogg");
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
