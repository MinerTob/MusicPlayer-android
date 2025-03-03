package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.exoplayer2.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements MusicService.MusicUiController {
    ActionBar actionBar;
    TextView tv_songName;
    TextView tv_seekBarHint;
    TextView tv_duration;
    TextView tv_lyrics;
    TextView tv_singer;
    SeekBar seekBar;
    ImageButton btn_play;
    ImageButton btn_pre;
    ImageButton btn_next;
    ImageButton btn_playList;
    ImageButton btn_playWay;
    ImageButton btn_like;
    ImageButton btn_sync;  // 添加同步按钮
    ListView listView;
    ArrayAdapter<String> adpter;
    List<String> music_list = new ArrayList<>();
    ConstraintLayout layout;
    MusicService musicService;
    private boolean serviceBound = false;
    private Timer timer; // 定时器
    private int currentPlayMode = 0; // 0:普通模式 1:单曲循环
    private int[] modeIcons = {
            R.drawable.play_in_order, // 普通模式
            R.drawable.single_cycle, // 单曲循环
            R.drawable.love_play // 收藏循环
    };

    // 修改播放模式定义与Service保持一致
    private static final int MODE_NORMAL = Player.REPEAT_MODE_OFF;
    private static final int MODE_LOOP_ONE = Player.REPEAT_MODE_ONE;
    private static final int MODE_FAVORITE = 2; // 收藏循环模式

    // 在 MainActivity 类的字段区增加映射，保存显示名称与原始文件名的对应关系
    private Map<String, String> displayToOriginalMapping = new HashMap<>();

    // 在类的成员变量区域添加
    private boolean isListViewVisible = false;

    // 添加同步状态UI组件
    private TextView textSyncStatus;
    private ProgressBar progressSync;

    // 添加SharedPreferences对象
    private SharedPreferences appPreferences;

    // 更新播放进度和歌曲总时长
    private class ProgressUpdate extends TimerTask {
        // 上次更新的歌曲名，用于检测是否切换了歌曲
        private String lastSongName = null;
        // 上次更新的位置，用于减少不必要的UI更新
        private long lastPosition = -1;

        @Override
        public void run() {
            runOnUiThread(() -> {
                if (musicService == null) {
                    return;
                }

                try {
                    boolean isPlaying = musicService.isPlaying();
                    String currentSong = musicService.getCurrentSongName();

                    // 如果播放器不在播放状态且不是刚切换歌曲，就不更新UI，减少资源占用
                    boolean songChanged = (lastSongName == null || !lastSongName.equals(currentSong));
                    if (!isPlaying && !songChanged && lastPosition >= 0) {
                        return;
                    }

                    // 获取当前播放位置和总时长
                    long position = musicService.getContentPosition();
                    long duration = musicService.getDuration();

                    // 只有当位置真正变化时才更新UI
                    boolean positionChanged = Math.abs(position - lastPosition) >= 500; // 允许500毫秒的误差

                    if (positionChanged || songChanged) {
                        // 更新进度信息
                        tv_seekBarHint.setText(format(position));
                        tv_duration.setText(format(duration));

                        // 更新进度条
                        if (duration > 0) {
                            seekBar.setMax((int) duration);
                            seekBar.setProgress((int) position);
                        }

                        // 保存当前位置用于下次比较
                        lastPosition = position;
                    }

                    // 处理歌曲即将结束的逻辑
                    if (isPlaying) {
                        musicService.handleNearCompletion();
                    }

                    // 只有在歌曲切换时更新歌曲信息
                    if (songChanged) {
                        // 获取并更新歌手信息
                        String singer = LyricManager.getSinger(currentSong, MainActivity.this);
                        tv_singer.setText(singer);

                        // 保存当前歌曲名，用于下次比较
                        lastSongName = currentSong;

                        // 更新收藏状态
                        updateFavoriteIcon();
                    }

                    // 更新歌词 - 歌词需要实时更新，但不更新UI中的歌曲名
                    updateLyricDisplay(lastSongName, position);

                } catch (Exception e) {
                    // 捕获所有异常，防止任务因异常而中断
                    Log.e("MainActivity", "进度更新出错: " + e.getMessage(), e);
                }
            });
        }

        // 更新歌词显示
        private void updateLyricDisplay(String songName, long position) {
            if (songName == null) {
                tv_lyrics.setText("");
                return;
            }

            try {
                // 获取歌词数据
                LyricManager.Lyric lyric = LyricManager.getLyric(songName, MainActivity.this);

                if (lyric != null && lyric.lyrics != null && !lyric.lyrics.isEmpty()) {
                    int index = lyric.findLyricIndex(position);

                    if (index >= 0 && index < lyric.lyrics.size()) {
                        String currentLyric = lyric.lyrics.get(index);
                        tv_lyrics.setText(currentLyric);

                        // 设置歌词文本动画效果
                        if (!currentLyric.isEmpty()) {
                            tv_lyrics.setAlpha(1.0f);
                        }
                    } else {
                        // 如果没有找到对应时间点的歌词，显示空白
                        tv_lyrics.setText("");
                    }
                } else {
                    tv_lyrics.setText("");
                }
            } catch (Exception e) {
                Log.e("MainActivity", "更新歌词出错: " + e.getMessage(), e);
                tv_lyrics.setText("");
            }
        }
    }

    /**
     * 格式化时间，将毫秒转换为mm:ss格式
     * @param position 毫秒表示的时间位置
     * @return 格式化的时间字符串
     */
    public String format(long position) {
        if (position < 0) {
            return "00:00";
        }

        // 直接计算分钟和秒钟，避免使用SimpleDateFormat可能带来的时区问题
        long totalSeconds = position / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    // 绑定 MusicService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            currentPlayMode = musicService.getPlayMode() == MusicService.MODE_LOOP_ONE ? 1 : 0;
            updatePlayModeIcon();
            serviceBound = true;

            // 设置歌曲切换监听器
            musicService.setOnSongChangeListener(new MusicService.OnSongChangeListener() {
                @Override
                public void onSongChanged() {
                    runOnUiThread(() -> {
                        updateFavoriteIcon();
                    });
                }
            });

            // 设置UI控制器
            musicService.setUiController(MainActivity.this);

            // 获取并设置文件同步管理器的监听器
            FileSyncManager syncManager = musicService.getFileSyncManager();
            if (syncManager != null) {
                syncManager.setSyncStatusListener(new FileSyncManager.SyncStatusListener() {
                    @Override
                    public void onSyncStarted() {
                        runOnUiThread(() -> {
                            textSyncStatus.setVisibility(View.VISIBLE);
                            progressSync.setVisibility(View.VISIBLE);
                            textSyncStatus.setText("同步中...");
                            progressSync.setIndeterminate(true);
                        });
                    }

                    @Override
                    public void onSyncProgress(int current, int total, String filename) {
                        runOnUiThread(() -> {
                            textSyncStatus.setVisibility(View.VISIBLE);
                            progressSync.setVisibility(View.VISIBLE);
                            textSyncStatus.setText(String.format("同步中: %s (%d/%d)", filename, current, total));
                            if (total > 0) {
                                progressSync.setIndeterminate(false);
                                progressSync.setMax(total);
                                progressSync.setProgress(current);
                            }
                        });
                    }

                    @Override
                    public void onSyncProgress(String fileName, int progress) {
                        runOnUiThread(() -> {
                            textSyncStatus.setVisibility(View.VISIBLE);
                            progressSync.setVisibility(View.VISIBLE);
                            textSyncStatus.setText(String.format("下载中: %s (%d%%)", fileName, progress));
                            progressSync.setIndeterminate(false);
                            progressSync.setMax(100);
                            progressSync.setProgress(progress);
                        });
                    }

                    @Override
                    public void onSyncCompleted(int total) {
                        runOnUiThread(() -> {
                            if (total > 0) {
                                textSyncStatus.setText(String.format("同步完成，共%d个文件", total));
                                // 3秒后隐藏
                                new Handler().postDelayed(() -> {
                                    textSyncStatus.setVisibility(View.GONE);
                                    progressSync.setVisibility(View.GONE);
                                }, 3000);
                            } else {
                                textSyncStatus.setText("无需同步");
                                // 1秒后隐藏
                                new Handler().postDelayed(() -> {
                                    textSyncStatus.setVisibility(View.GONE);
                                    progressSync.setVisibility(View.GONE);
                                }, 1000);
                            }
                        });
                    }

                    @Override
                    public void onSyncError(String errorMessage) {
                        runOnUiThread(() -> {
                            textSyncStatus.setVisibility(View.VISIBLE);
                            progressSync.setVisibility(View.GONE);
                            textSyncStatus.setText("同步失败: " + errorMessage);
                            // 5秒后隐藏
                            new Handler().postDelayed(() -> {
                                textSyncStatus.setVisibility(View.GONE);
                            }, 5000);
                        });
                    }
                });
            }

            // 更新初始UI状态
            updateUI();
            
            // 在服务连接后专门检查收藏状态并更新收藏图标
            runOnUiThread(() -> {
                // 获取当前歌曲名
                String currentSong = musicService.getCurrentSongName();
                
                // 如果当前歌曲名不为空，则检查是否为收藏歌曲并更新图标
                if (currentSong != null && !currentSong.isEmpty() && !tv_songName.getText().toString().equals("Song name")) {
                    Log.d("MainActivity", "检查收藏状态: " + currentSong);
                    if (FavoriteManager.isFavorite(currentSong)) {
                        btn_like.setImageResource(R.drawable.like_red);
                    } else {
                        btn_like.setImageResource(R.drawable.like);
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            serviceBound = false;
            btn_play.setImageResource(R.drawable.play);
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    };
    View.OnClickListener listener1 = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, MusicService.class);
            switch (v.getId()) {
                case R.id.btn_playList:
                    showListView();
                    break;
                case R.id.btn_play:
                    playOrPauseMusic();
                    break;
                case R.id.btn_pre:
                    playPreviousTrack();
                    break;
                case R.id.btn_next:
                    playNextTrack();
                    break;
                case R.id.btn_playWay:
                    updatePlayMode();
                    break;
                case R.id.btn_sync:
                    refreshMusicList();
                    break;
            }
        }
    };

    // ListView 显示音乐列表 start
    public void showListView() {
        music_list = getMusic();
        SongAdapter adapter = new SongAdapter(MainActivity.this, music_list);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(listener2);

        Animation slideIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_bottom);
        listView.startAnimation(slideIn);
        listView.setVisibility(View.VISIBLE);
        isListViewVisible = true;
    }

    // 修改 getMusic() 方法，构建显示列表和映射关系
    List<String> getMusic() {
        List<String> mList = new ArrayList<>();
        displayToOriginalMapping.clear(); // 每次获取列表前清空映射
        try {
            // 1. 获取assets目录中的音乐文件
            String[] fNames = getAssets().list("music");
            for (String fn : fNames) {
                // 只添加音频文件
                if (fn.endsWith(".mp3") || fn.endsWith(".flac") || 
                    fn.endsWith(".m4a") || fn.endsWith(".wav") || 
                    fn.endsWith(".ogg")) {
                    // 生成友好显示名称：去掉文件名后缀，并将下划线替换为空格
                    String displayName = fn.substring(0, fn.lastIndexOf(".")).replace("_", " ");
                    mList.add(displayName);
                    // 记录显示名称和原始文件名的对应关系
                    displayToOriginalMapping.put(displayName, fn);
                }
            }
            
            // 2. 获取下载目录中的音乐文件
            File localMusicDir = FileSyncManager.getLocalMusicDir(this);
            if (localMusicDir.exists() && localMusicDir.isDirectory()) {
                File[] localFiles = localMusicDir.listFiles();
                if (localFiles != null) {
                    for (File file : localFiles) {
                        if (file.isFile() && (file.getName().endsWith(".mp3") || 
                            file.getName().endsWith(".flac") || 
                            file.getName().endsWith(".m4a") || 
                            file.getName().endsWith(".wav") || 
                            file.getName().endsWith(".ogg"))) {
                            String fileName = file.getName();
                            // 生成友好显示名称：去掉文件名后缀，并将下划线替换为空格
                            String displayName = fileName.substring(0, fileName.lastIndexOf(".")).replace("_", " ");
                            
                            // 避免重复添加（如果assets中已有同名文件）
                            if (!displayToOriginalMapping.containsKey(displayName)) {
                                mList.add(displayName);
                                displayToOriginalMapping.put(displayName, fileName);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return mList;
    }
    
    // 判断文件是否为音频文件
    private boolean isAudioFile(String fileName) {
        return fileName.endsWith(".mp3") || fileName.endsWith(".flac") || 
               fileName.endsWith(".m4a") || fileName.endsWith(".wav") || 
               fileName.endsWith(".ogg");
    }

    // 恢复列表项点击选歌功能：点击列表项会播放对应歌曲，并执行退出动画
    private AdapterView.OnItemClickListener listener2 = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String selectedDisplayName = (String) parent.getItemAtPosition(position);
            String originalName = displayToOriginalMapping.get(selectedDisplayName);
            if (originalName == null) {
                originalName = selectedDisplayName;
            }

            // 在收藏模式下点击非收藏歌曲时，切换回普通模式
            if (currentPlayMode == MODE_FAVORITE && !FavoriteManager.isFavorite(originalName)) {
                Toast.makeText(MainActivity.this, "已切换回普通模式", Toast.LENGTH_SHORT).show();
                currentPlayMode = MODE_NORMAL;
                musicService.setPlayMode(MODE_NORMAL);
                updatePlayModeIcon();
            }

            // 播放选中的歌曲
            playMusic(originalName);

            // 使用慢速动画关闭列表
            Animation slideOut = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_top);
            slideOut.setDuration(600);
            slideOut.setFillAfter(true);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    listView.setVisibility(View.GONE);
                    listView.clearAnimation();
                    isListViewVisible = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            listView.startAnimation(slideOut);
        }
    };

    /**
     * 播放指定的音乐
     * @param musicName 音乐文件名
     */
    public void playMusic(String musicName) {
        try {
            if (!serviceBound || musicService == null) {
                Log.e("MainActivity", "尝试播放音乐，但服务未绑定");
                Toast.makeText(this, "音乐服务未就绪", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 停止已有的计时器
            stopProgressTimer();
            
            // 启动新的计时器
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 500); // 降低更新频率至500ms以减少资源占用
            
            // 播放音乐
            musicService.playMusic(musicName);
            btn_play.setImageResource(R.drawable.pause);
            btn_like.setEnabled(true);
            updateFavoriteIcon();
            
            // 为了更好的用户体验，立即更新进度和时长
            long position = 0;
            long duration = musicService.getDuration();
            if (duration > 0) {
                tv_seekBarHint.setText(format(position));
                tv_duration.setText(format(duration));
                seekBar.setMax((int) duration);
                seekBar.setProgress(0);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "播放音乐失败: " + e.getMessage(), e);
            Toast.makeText(this, "播放失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 播放或暂停当前音乐
     */
    public void playOrPauseMusic() {
        try {
            if (!serviceBound || musicService == null) {
                Log.e("MainActivity", "尝试播放/暂停音乐，但服务未绑定");
                Toast.makeText(this, "音乐服务未就绪，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d("MainActivity", "播放/暂停按钮被点击");
            boolean isPlaying = musicService.isPlaying();
            Log.d("MainActivity", "当前播放状态: " + (isPlaying ? "播放中" : "已暂停"));
            
            if (isPlaying) {
                // 如果正在播放，则暂停
                Log.d("MainActivity", "暂停播放...");
                musicService.pause();
                btn_play.setImageResource(R.drawable.play);
            } else {
                // 如果已暂停，则继续播放
                Log.d("MainActivity", "开始播放...");
                
                // 如果没有计时器，创建一个新的
                if (timer == null) {
                    timer = new Timer();
                    timer.schedule(new ProgressUpdate(), 0, 500);
                }
                
                // 先检查是否有当前歌曲，如果没有则尝试播放第一首
                String currentSong = musicService.getCurrentSongName();
                if (currentSong == null || currentSong.isEmpty() || currentSong.equals("unknown.flac")) {
                    Log.d("MainActivity", "没有当前歌曲，尝试播放第一首...");
                    List<String> musicList = getMusic();
                    if (!musicList.isEmpty()) {
                        String firstSong = displayToOriginalMapping.get(musicList.get(0));
                        if (firstSong != null) {
                            Log.d("MainActivity", "播放第一首歌曲: " + firstSong);
                            playMusic(firstSong);
                            return;
                        }
                    } else {
                        Log.e("MainActivity", "音乐列表为空，无法播放");
                        Toast.makeText(this, "没有可播放的音乐", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    // 有当前歌曲，继续播放
                    Log.d("MainActivity", "继续播放当前歌曲: " + currentSong);
                    musicService.play();
                    // 在播放成功后再更新UI
                    btn_play.setImageResource(R.drawable.pause);
                }
                
                // 更新当前歌曲信息
                currentSong = musicService.getCurrentSongName();
                if (currentSong != null && !currentSong.isEmpty() && !currentSong.equals("unknown.flac")) {
                    Log.d("MainActivity", "更新歌曲信息: " + currentSong);
                    updateFavoriteIcon();
                } else {
                    Log.e("MainActivity", "无法获取当前歌曲名称");
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "播放/暂停操作失败: " + e.getMessage(), e);
            Toast.makeText(this, "操作失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    // 播放上一首音乐
    public void playPreviousTrack() {
        if (serviceBound && musicService != null) {
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 1000);
            btn_play.setImageResource(R.drawable.pause);
            musicService.playPreviousTrack();
            String songName = musicService.getCurrentSongName();
            updateFavoriteIcon();
        }
    }

    // 播放下一首音乐
    public void playNextTrack() {
        if (serviceBound && musicService != null) {
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 1000);
            btn_play.setImageResource(R.drawable.pause);
            musicService.playNextTrack();
            String songName = musicService.getCurrentSongName();
            updateFavoriteIcon();
        }
    }

    public void updateSongName(String songName) {
        // 添加空值检查
        if (songName == null || songName.isEmpty()) {
            tv_songName.setText("未知歌曲");
            return;
        }
        
        // 检查文件名是否包含扩展名
        int dotIndex = songName.lastIndexOf(".");
        if (dotIndex > 0) {
            // 去掉文件名后缀，并将下划线替换为空格
            songName = songName.substring(0, dotIndex).replace("_", " ");
        } else {
            // 如果没有扩展名，只替换下划线
            songName = songName.replace("_", " ");
        }
        
        tv_songName.setText(songName);
    }

    // 添加模式切换方法
    private void updatePlayMode() {
        int nextMode = (currentPlayMode + 1) % modeIcons.length;

        if (nextMode == MODE_FAVORITE) {
            // 获取收藏歌曲列表
            Set<String> favorites = FavoriteManager.getFavorites();
            
            // 检查是否有收藏的歌曲
            if (favorites.isEmpty()) {
                Toast.makeText(this, "没有收藏的歌曲，已切换回普通模式", Toast.LENGTH_SHORT).show();
                nextMode = MODE_NORMAL;
            } else {
                // 检查媒体库中是否有任何收藏的歌曲
                boolean hasCollectedSongs = false;
                for (String displayName : music_list) {
                    String originalName = displayToOriginalMapping.get(displayName);
                    if (originalName != null && FavoriteManager.isFavorite(originalName)) {
                        hasCollectedSongs = true;
                        break;
                    }
                }

                if (!hasCollectedSongs) {
                    Toast.makeText(this, "当前列表没有收藏的歌曲，已切换回普通模式", Toast.LENGTH_SHORT).show();
                    nextMode = MODE_NORMAL;
                }
                // MusicService会处理剩余逻辑，包括当前歌曲不在收藏列表中的情况
            }
        }

        // 设置新的播放模式
        currentPlayMode = nextMode;
        if (musicService != null) {
            musicService.setPlayMode(currentPlayMode);
            
            // 如果设置成功且是收藏模式，更新UI
            if (currentPlayMode == MODE_FAVORITE) {
                // 更新歌手信息和歌词
                String currentSong = musicService.getCurrentSongName();
                if (currentSong != null && !currentSong.isEmpty()) {
                    String singer = LyricManager.getSinger(currentSong, this);
                    tv_singer.setText(singer);
                    LyricManager.Lyric lyric = LyricManager.getLyric(currentSong, this);
                    if (lyric != null && lyric.lyrics != null && !lyric.lyrics.isEmpty()) {
                        tv_lyrics.setText(lyric.lyrics.get(0));
                    }
                }
            }
        }
        updatePlayModeIcon();

        // 刷新列表
        if (listView != null && listView.getAdapter() != null) {
            ((SongAdapter) listView.getAdapter()).notifyDataSetChanged();
        }
    }

    // 更新模式图标
    private void updatePlayModeIcon() {
        btn_playWay.setImageResource(modeIcons[currentPlayMode]);
        // 添加模式提示Toast
        String[] modeNames = {
                "列表循环",
                "单曲循环",
                "收藏循环"
        };
        Toast.makeText(this, modeNames[currentPlayMode], Toast.LENGTH_SHORT).show();
    }

    // 更新收藏图标
    public void updateFavoriteIcon() {
        if (musicService != null) {
            String currentSong = musicService.getCurrentSongName();
            
            // 检查tv_songName是否已显示歌曲名（不是默认值"Song name"）
            if (tv_songName != null && tv_songName.getText() != null && 
                !tv_songName.getText().toString().equals("Song name")) {
                
                // 如果当前歌曲名不为空，检查是否为收藏歌曲
                if (currentSong != null && !currentSong.trim().isEmpty()) {
                    Log.d("MainActivity", "更新收藏图标状态: " + currentSong + ", 收藏状态: " + FavoriteManager.isFavorite(currentSong));
                    
                    if (FavoriteManager.isFavorite(currentSong)) {
                        btn_like.setImageResource(R.drawable.like_red);
                    } else {
                        btn_like.setImageResource(R.drawable.like);
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void initView() {
        tv_songName = (TextView) findViewById(R.id.tv_songName);
        tv_seekBarHint = (TextView) findViewById(R.id.tv_seekBarHint);
        tv_duration = (TextView) findViewById(R.id.tv_duration);
        tv_lyrics = (TextView) findViewById(R.id.tv_lyrics);
        tv_singer = (TextView) findViewById(R.id.tv_singer);

        btn_play = (ImageButton) findViewById(R.id.btn_play);
        btn_pre = (ImageButton) findViewById(R.id.btn_pre);
        btn_next = (ImageButton) findViewById(R.id.btn_next);
        btn_playList = (ImageButton) findViewById(R.id.btn_playList);
        btn_playWay = (ImageButton) findViewById(R.id.btn_playWay);
        btn_like = (ImageButton) findViewById(R.id.btn_like);
        btn_sync = (ImageButton) findViewById(R.id.btn_sync);  // 添加同步按钮

        btn_play.setOnClickListener(listener1);
        btn_pre.setOnClickListener(listener1);
        btn_next.setOnClickListener(listener1);
        btn_playList.setOnClickListener(listener1);
        btn_playWay.setOnClickListener(listener1);
        btn_sync.setOnClickListener(listener1);  // 添加同步按钮点击事件

        btn_like.setEnabled(false); // 一开始禁用收藏按钮

        btn_like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 仅检查 musicService 是否为空或当前歌曲名称为空
                if (musicService == null ||
                        musicService.getCurrentSongName() == null ||
                        musicService.getCurrentSongName().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "请等待音乐加载...", Toast.LENGTH_SHORT).show();
                    return;
                }
                String currentSong = musicService.getCurrentSongName();
                // 切换收藏状态并更新图标
                if (FavoriteManager.isFavorite(currentSong)) {
                    FavoriteManager.removeFavorite(MainActivity.this, currentSong);
                    btn_like.setImageResource(R.drawable.like);
                } else {
                    FavoriteManager.addFavorite(MainActivity.this, currentSong);
                    btn_like.setImageResource(R.drawable.like_red);
                }

                // 更新 MusicService 内的收藏列表
                musicService.updateFavoritePlaylist();

                // 如果列表视图可见，刷新适配器
                if (isListViewVisible && listView.getAdapter() != null) {
                    ((SongAdapter) listView.getAdapter()).notifyDataSetChanged();
                }

                checkFavoriteMode();
            }
        });

        listView = (ListView) findViewById(R.id.lv_music);

        layout = (ConstraintLayout) findViewById(R.id.constrainLayout);

        layout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (isListViewVisible) { // 仅在列表显示时处理
                    int[] listViewLocation = new int[2];
                    listView.getLocationOnScreen(listViewLocation);
                    if (event.getRawX() < listViewLocation[0] ||
                            event.getRawX() > listViewLocation[0] + listView.getWidth() ||
                            event.getRawY() < listViewLocation[1] ||
                            event.getRawY() > listViewLocation[1] + listView.getHeight()) {

                        Animation slideOut = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_top);
                        slideOut.setDuration(600); // 使用慢速动画
                        slideOut.setFillAfter(true); // 保持动画结束状态
                        slideOut.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                listView.setVisibility(View.GONE);
                                listView.clearAnimation(); // 清除因动画带来的残留状态
                                isListViewVisible = false;
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                        listView.startAnimation(slideOut);
                        return true;
                    }
                }
                return false;
            }
        });

        // SeekBar
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tv_seekBarHint = (TextView) findViewById(R.id.tv_seekBarHint);
        tv_duration = (TextView) findViewById(R.id.tv_duration);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // SeekBar 的进度发生改变时触发的操作
                if (fromUser) {
                    timer = new Timer();
                    timer.schedule(new ProgressUpdate(), 0, 1000);
                    
                    // 格式化时间显示
                    tv_seekBarHint.setText(format(progress));
                    musicService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 用户开始滑动 SeekBar 时触发的操作
                btn_play.setImageResource(R.drawable.play);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 用户停止滑动 SeekBar 时触发的操作
                btn_play.setImageResource(R.drawable.pause);
                // 不再在这里更新歌曲名称，避免与定时任务中的更新冲突
                musicService.play();
            }
        });

        // 同步状态UI组件
        textSyncStatus = findViewById(R.id.textSyncStatus);
        progressSync = findViewById(R.id.progressSync);
        textSyncStatus.setVisibility(View.GONE);
        progressSync.setVisibility(View.GONE);
    }

    // 刷新音乐列表
    private void refreshMusicList() {
        // 显示加载提示
        Toast.makeText(this, "正在刷新音乐列表...", Toast.LENGTH_SHORT).show();
        
        // 显示同步状态UI
        textSyncStatus.setVisibility(View.VISIBLE);
        progressSync.setVisibility(View.VISIBLE);
        textSyncStatus.setText("准备同步...");
        progressSync.setIndeterminate(true);
        
        // 更新按钮至开始同步
        btn_sync.setEnabled(false);
        
        // 在后台线程执行
        new Thread(() -> {
            if (musicService != null) {
                // 先尝试同步文件
                FileSyncManager syncManager = musicService.getFileSyncManager();
                if (syncManager != null) {
                    syncManager.startSync();
                }
                
                // 等待短暂时间后刷新UI
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // 在UI线程更新界面
                runOnUiThread(() -> {
                    // 刷新播放列表
                    showListView();
                    
                    // 隐藏同步状态UI
                    textSyncStatus.setVisibility(View.GONE);
                    progressSync.setVisibility(View.GONE);
                    
                    // 按钮可用
                    btn_sync.setEnabled(true);
                    
                    // 显示完成提示
                    Toast.makeText(MainActivity.this, "音乐列表已刷新", Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "播放服务未初始化，无法刷新", Toast.LENGTH_SHORT).show();
                    textSyncStatus.setVisibility(View.GONE);
                    progressSync.setVisibility(View.GONE);
                    btn_sync.setEnabled(true);
                });
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化SharedPreferences
        appPreferences = getSharedPreferences("MusicPlayerPrefs", MODE_PRIVATE);
        
        // 设置全屏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 隐藏系统 ActionBar（导航栏）
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 初始化收藏数据（确保收藏列表不会因为后台而丢失）
        FavoriteManager.initFavorites(this);

        // 其他初始化代码
        initView();

        Intent intent = new Intent(MainActivity.this, MusicService.class);
        
        // 传递上次播放状态
        String lastPlayedSong = appPreferences.getString("last_played_song", null);
        long lastPosition = appPreferences.getLong("last_position", 0);
        boolean shouldAutoPlay = appPreferences.getBoolean("auto_play_on_start", true);
        
        if (lastPlayedSong != null) {
            intent.putExtra("last_played_song", lastPlayedSong);
            intent.putExtra("last_position", lastPosition);
            intent.putExtra("auto_play", shouldAutoPlay);
        }
        
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        startService(intent); // 确保服务不会因为解绑而停止
        
        // 启动UI更新任务
        uiHandler.postDelayed(updateTask, 1000);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 停止进度更新定时器
     */
    private void stopProgressTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止进度更新
        stopProgressTimer();
        
        // 移除UI更新任务
        uiHandler.removeCallbacks(updateTask);
        
        // 解绑服务
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        // 保存用户首选项
        saveUserPreferences();
        
        // 如果不需要后台播放，停止服务
        if (!appPreferences.getBoolean("play_in_background", true)) {
            Intent intent = new Intent(this, MusicService.class);
            stopService(intent);
        }
    }
    
    /**
     * 保存用户首选项
     */
    private void saveUserPreferences() {
        // 保存当前歌曲和进度
        if (serviceBound && musicService != null) {
            SharedPreferences.Editor editor = appPreferences.edit();
            editor.putString("last_played_song", musicService.getCurrentSongName());
            editor.putLong("last_position", musicService.getContentPosition());
            editor.putBoolean("auto_play_on_start", musicService.isPlaying());
            editor.apply();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂时停止进度更新
        stopProgressTimer();
        
        // 移除定时更新任务
        uiHandler.removeCallbacks(updateTask);
        
        // 如果正在播放且后台播放没有启用，暂停播放
        if (serviceBound && musicService != null && musicService.isPlaying() && !appPreferences.getBoolean("play_in_background", true)) {
            musicService.pause();
            btn_play.setImageResource(R.drawable.play);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果服务已绑定且音乐正在播放，恢复进度更新
        if (serviceBound && musicService != null) {
            // 恢复进度更新
            if (musicService.isPlaying() && timer == null) {
                timer = new Timer();
                timer.schedule(new ProgressUpdate(), 0, 500);
            }
            
            // 更新UI状态
            if (musicService.isPlaying()) {
                btn_play.setImageResource(R.drawable.pause);
            } else {
                btn_play.setImageResource(R.drawable.play);
            }
            
            // 确保收藏图标状态正确
            String currentSong = musicService.getCurrentSongName();
            if (currentSong != null && !currentSong.isEmpty() && !tv_songName.getText().toString().equals("Song name")) {
                Log.d("MainActivity", "onResume检查收藏状态: " + currentSong);
                updateFavoriteIcon();
            }
            
            // 更新进度条
            long position = musicService.getContentPosition();
            long duration = musicService.getDuration();
            if (duration > 0) {
                tv_seekBarHint.setText(format(position));
                tv_duration.setText(format(duration));
                seekBar.setMax((int) duration);
                seekBar.setProgress((int) position);
            }
            
            // 更新UI
            updateUI();
        }
        
        // 重新启动UI更新任务
        uiHandler.postDelayed(updateTask, 1000);
    }

    // 在收藏/取消收藏时检查当前模式
    private void checkFavoriteMode() {
        if (currentPlayMode == MODE_FAVORITE) {
            // 检查是否还有收藏歌曲
            Set<String> favorites = FavoriteManager.getFavorites();
            if (favorites.isEmpty()) {
                Toast.makeText(this, "已没有收藏歌曲，已切换回普通模式", Toast.LENGTH_SHORT).show();
                currentPlayMode = MODE_NORMAL;
                musicService.setPlayMode(MODE_NORMAL);
                updatePlayModeIcon();
                return;
            }
            
            // 检查当前歌曲是否是收藏歌曲
            String currentSong = musicService.getCurrentSongName();
            if (!FavoriteManager.isFavorite(currentSong)) {
                Toast.makeText(this, "当前歌曲不再是收藏歌曲，已切换回普通模式", Toast.LENGTH_SHORT).show();
                currentPlayMode = MODE_NORMAL;
                musicService.setPlayMode(MODE_NORMAL);
                updatePlayModeIcon();
            } else {
                // 仅更新收藏列表，保持收藏模式
                musicService.updateFavoritePlaylist();
                Toast.makeText(this, "收藏列表已更新", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 实现MusicUiController接口方法

    @Override
    public void updatePlayState(boolean isPlaying) {
        // 更新播放/暂停按钮状态
        btn_play.setImageResource(isPlaying ? R.drawable.pause : R.drawable.play);
    }

    @Override
    public void updateSongInfo(String title, String singer) {
        // 更新歌曲信息
        if (tv_songName != null) {
            tv_songName.setText(title);
        }
        if (tv_singer != null) {
            tv_singer.setText(singer);
        }
    }

    @Override
    public void updateProgress(long currentPosition, long duration) {
        // 更新进度条和时间显示
        if (seekBar != null) {
            seekBar.setMax((int) duration);
            seekBar.setProgress((int) currentPosition);
        }
        
        // 格式化时间显示
        tv_seekBarHint.setText(format(currentPosition));
        tv_duration.setText(format(duration));
    }

    @Override
    public void updateLyric(String currentLyric, String nextLyric) {
        // 更新歌词显示
        if (tv_lyrics != null) {
            tv_lyrics.setText(currentLyric);
        }
    }

    // 复制在 MainActivity 类内添加自定义适配器
    public class SongAdapter extends ArrayAdapter<String> {
        private List<String> songs;

        public SongAdapter(Context context, List<String> songs) {
            super(context, 0, songs);
            this.songs = songs;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.list_item_song, parent, false);
            }
            final String displayName = getItem(position);
            TextView tvSong = view.findViewById(R.id.tv_song_item);
            tvSong.setText(displayName);

            // 获取原始文件名（带.mp3后缀）
            final String originalName = displayToOriginalMapping.get(displayName);

            final ImageButton btnListLike = view.findViewById(R.id.btn_list_like);
            // 根据收藏状态显示对应图标（使用原始文件名判断）
            if (FavoriteManager.isFavorite(originalName)) {
                btnListLike.setImageResource(R.drawable.like_red);
            } else {
                btnListLike.setImageResource(R.drawable.like);
            }

            btnListLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (FavoriteManager.isFavorite(originalName)) {
                        FavoriteManager.removeFavorite(getContext(), originalName);
                        btnListLike.setImageResource(R.drawable.like);
                    } else {
                        FavoriteManager.addFavorite(getContext(), originalName);
                        btnListLike.setImageResource(R.drawable.like_red);
                    }

                    // 使用原始文件名（带.mp3后缀）进行比较
                    if (musicService != null) {
                        String currentSong = musicService.getCurrentSongName();
                        if (originalName.equals(currentSong)) {
                            updateFavoriteIcon(); // 更新主界面收藏图标
                        }
                    }

                    // 在点击事件最后添加检查收藏模式的调用
                    checkFavoriteMode();
                }
            });
            return view;
        }
    }

    private Handler uiHandler = new Handler();
    private Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            updateUI();
            uiHandler.postDelayed(this, 1000);
        }
    };

    private void updateUI() {
        if (serviceBound && musicService != null) {
            // 更新UI，让MusicService更新所有UI元素
            musicService.updateSongInfo();
            musicService.updateProgress();
        }
    }
}