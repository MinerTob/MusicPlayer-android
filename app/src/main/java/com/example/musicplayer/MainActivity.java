package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.exoplayer2.Player;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
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
    ListView listView;
    ArrayAdapter<String> adpter;
    List<String> music_list = new ArrayList<>();
    ConstraintLayout layout;
    MusicService musicService;
    private boolean isServiceBound = false;
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

    // 更新播放进度和歌曲总时长
    private class ProgressUpdate extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> {
                if (musicService != null && musicService.isPlaying()) {
                    long position = musicService.getContentPosition();
                    long duration = musicService.getDuration();

                    // 更新当前播放进度和总时长
                    tv_seekBarHint.setText(format(position));
                    tv_duration.setText(format(duration));
                    seekBar.setMax((int) duration);
                    seekBar.setProgress((int) position);

                    // 在即将结束时处理下一首歌曲
                    musicService.handleNearCompletion();

                    // 获取当前播放歌曲
                    String currentSong = musicService.getCurrentSongName();

                    // 更新歌名显示
                    updateSongName(currentSong);

                    // 获取并更新歌手信息
                    String singer = LyricManager.getSinger(currentSong, MainActivity.this);
                    tv_singer.setText(singer);

                    // 获取当前歌词数据
                    LyricManager.Lyric lyric = LyricManager.getLyric(currentSong, MainActivity.this);
                    if (lyric != null && lyric.lyrics != null) {
                        int index = lyric.findLyricIndex(position);
                        if (index >= 0 && index < lyric.lyrics.size()) {
                            tv_lyrics.setText(lyric.lyrics.get(index));
                        } else {
                            tv_lyrics.setText("");
                        }
                    } else {
                        tv_lyrics.setText("");
                    }
                }
            });
        }
    }

    public String format(long position) {
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss"); // "分:秒"格式
        String timeStr = sdf.format(position); // 会自动将时长(毫秒数)转换为分秒格式
        return timeStr;
    }

    // 绑定 MusicService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            currentPlayMode = musicService.getPlayMode() == MusicService.MODE_LOOP_ONE ? 1 : 0;
            updatePlayModeIcon();
            isServiceBound = true;

            // 设置歌曲切换监听器
            musicService.setOnSongChangeListener(new MusicService.OnSongChangeListener() {
                @Override
                public void onSongChanged() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 更新收藏图标状态
                            updateFavoriteIcon();
                        }
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isServiceBound = false;
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
            String[] fNames = getAssets().list("music");
            for (String fn : fNames) {
                // 生成友好显示名称：去掉文件名后缀，并将下划线替换为空格
                String displayName = fn.substring(0, fn.lastIndexOf(".")).replace("_", " ");
                mList.add(displayName);
                // 记录显示名称和原始文件名的对应关系
                displayToOriginalMapping.put(displayName, fn);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return mList;
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

    public void playMusic(String musicName) {
        updateSongName(musicName);
        if (isServiceBound && musicService != null) {
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 1000);
            musicService.playMusic(musicName);
            btn_play.setImageResource(R.drawable.pause);
            btn_like.setEnabled(true);
            updateFavoriteIcon();
        }
    }
    // ListView 显示音乐列表 end

    public void playOrPauseMusic() {
        if (musicService.isPlaying()) {
            btn_play.setImageResource(R.drawable.play);
            musicService.pause();
        } else {
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 1000);
            btn_play.setImageResource(R.drawable.pause);
            musicService.play();
            String songName = musicService.getCurrentSongName();
            updateSongName(songName);
            btn_like.setEnabled(true);
            updateFavoriteIcon();
        }
    }

    // 播放上一首音乐
    public void playPreviousTrack() {
        if (isServiceBound && musicService != null) {
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 1000);
            btn_play.setImageResource(R.drawable.pause);
            musicService.playPreviousTrack();
            String songName = musicService.getCurrentSongName();
            updateSongName(songName);
            btn_like.setEnabled(true);
            updateFavoriteIcon();
        }
    }

    // 播放下一首音乐
    public void playNextTrack() {
        if (isServiceBound && musicService != null) {
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 1000);
            btn_play.setImageResource(R.drawable.pause);
            musicService.playNextTrack();
            String songName = musicService.getCurrentSongName();
            updateSongName(songName);
            btn_like.setEnabled(true);
            updateFavoriteIcon();
        }
    }

    public void updateSongName(String songName) {
        // 去掉文件名后缀，并将下划线替换为空格
        songName = songName.substring(0, songName.lastIndexOf(".")).replace("_", " ");
        tv_songName.setText(songName);
    }

    // 添加模式切换方法
    private void updatePlayMode() {
        currentPlayMode = (currentPlayMode + 1) % modeIcons.length;

        if (currentPlayMode == MODE_FAVORITE) {
            // 检查是否有收藏的歌曲
            if (FavoriteManager.getFavorites().isEmpty()) {
                Toast.makeText(this, "没有收藏的歌曲，已切换回普通模式", Toast.LENGTH_SHORT).show();
                currentPlayMode = MODE_NORMAL;
            } else {
                musicService.updateFavoritePlaylist();
            }
        }

        musicService.setPlayMode(currentPlayMode);
        updatePlayModeIcon();
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

    // 更新收藏状态图标，根据当前播放歌曲是否已收藏进行设置
    public void updateFavoriteIcon() {
        if (musicService != null) {
            String currentSong = musicService.getCurrentSongName();
            if (currentSong != null && !currentSong.trim().isEmpty() && FavoriteManager.isFavorite(currentSong)) {
                btn_like.setImageResource(R.drawable.like_red);
            } else {
                btn_like.setImageResource(R.drawable.like);
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

        btn_play.setOnClickListener(listener1);
        btn_pre.setOnClickListener(listener1);
        btn_next.setOnClickListener(listener1);
        btn_playList.setOnClickListener(listener1);
        btn_playWay.setOnClickListener(listener1);

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
                    FavoriteManager.removeFavorite(currentSong, MainActivity.this);
                    btn_like.setImageResource(R.drawable.like);
                } else {
                    FavoriteManager.addFavorite(currentSong, MainActivity.this);
                    btn_like.setImageResource(R.drawable.like_red);
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
                updateSongName(musicService.getCurrentSongName());
                musicService.play();
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除直接调用service.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection);
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // 在收藏/取消收藏时检查当前模式
    private void checkFavoriteMode() {
        if (currentPlayMode == MODE_FAVORITE) {
            Toast.makeText(this, "收藏列表已更改，已切换回普通模式", Toast.LENGTH_SHORT).show();
            currentPlayMode = MODE_NORMAL;
            musicService.setPlayMode(MODE_NORMAL);
            updatePlayModeIcon();
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
                        FavoriteManager.removeFavorite(originalName, getContext());
                        btnListLike.setImageResource(R.drawable.like);
                    } else {
                        FavoriteManager.addFavorite(originalName, getContext());
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
}