package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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
            R.drawable.single_cycle // 单曲循环
    };

    // 修改播放模式定义与Service保持一致
    private static final int MODE_NORMAL = Player.REPEAT_MODE_OFF;
    private static final int MODE_LOOP_ONE = Player.REPEAT_MODE_ONE;

    // 在 MainActivity 类的字段区增加映射，保存显示名称与原始文件名的对应关系
    private Map<String, String> displayToOriginalMapping = new HashMap<>();

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

                    // 获取当前播放歌曲（确保文件名与 LRC 文件名一致，例如 "蒲公英的约定.mp3"）
                    String currentSong = musicService.getCurrentSongName();

                    // 更新歌名显示（可在此处对文件名进行去除 .mp3 后缀等处理）
                    updateSongName(currentSong);

                    // 获取并更新歌手信息（不再调用 Dandelion_Promise 或 Counting_Stars，而是通过 LyricManager 动态解析 LRC
                    // 文件）
                    String singer = LyricManager.getSinger(currentSong, MainActivity.this);
                    tv_singer.setText(singer);

                    // 获取当前歌词数据，清空对旧歌词类的调用
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
            musicService = ((MusicService.LocalBinder) service).getService();
            currentPlayMode = musicService.getPlayMode() == MusicService.MODE_LOOP_ONE ? 1 : 0;
            updatePlayModeIcon();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 添加保护逻辑
            runOnUiThread(() -> {
                musicService = null;
                isServiceBound = false;
                btn_play.setImageResource(R.drawable.play);
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            });
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
                    cyclePlayMode();
                    break;
            }
        }
    };

    // ListView 显示音乐列表 start
    public void showListView() {
        music_list = getMusic(); // 获取音乐列表
        adpter = new ArrayAdapter<String>( // 创建适配器
                MainActivity.this,
                android.R.layout.simple_list_item_single_choice,
                music_list);
        listView.setAdapter(adpter); // 设置适配器
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE); // 设置选择模式

        listView.setOnItemClickListener(listener2); // 设置监听器
        listView.setVisibility(View.VISIBLE); // 设置可见
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
        mList.add("返回");
        return mList;
    }

    // 修改 ListView 的点击事件监听器，使其根据显示名称获取正确的原始名称再播放
    AdapterView.OnItemClickListener listener2 = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListView lv = (ListView) parent;
            lv.setSelector(R.color.purple_200); // 设置高亮背景色
            String selectedDisplayName = parent.getItemAtPosition(position).toString(); // 获取显示名称
            if (selectedDisplayName.equals("返回")) {
                listView.setVisibility(View.GONE);
            } else {
                // 根据映射表获得原始文件名，保证播放正确的音乐文件
                String originalName = displayToOriginalMapping.get(selectedDisplayName);
                playMusic(originalName);
            }
        }
    };

    public void playMusic(String musicName) {
        updateSongName(musicName);
        if (isServiceBound && musicService != null) {
            timer = new Timer();
            timer.schedule(new ProgressUpdate(), 0, 1000);
            musicService.playMusic(musicName);
            btn_play.setImageResource(R.drawable.pause);
        }
    }
    // ListView 显示音乐列表 end

    public void playOrPauseMusic() {
        if (musicService.isPlaying()) {
            btn_play.setImageResource(R.drawable.play);
            musicService.pause();
        } else {
            // 创建一个 Timer 对象，用于定时更新进度条和文本
            timer = new Timer();
            // 将 ProgressUpdate 任务调度到 Timer 中，每隔一定时间执行一次
            timer.schedule(new ProgressUpdate(), 0, 1000);
            btn_play.setImageResource(R.drawable.pause);

            musicService.play();

            String songName = musicService.getCurrentSongName();
            updateSongName(songName);
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
        }
    }

    public void updateSongName(String songName) {
        // 去掉文件名后缀，并将下划线替换为空格
        songName = songName.substring(0, songName.lastIndexOf(".")).replace("_", " ");
        tv_songName.setText(songName);
    }

    // 添加模式切换方法
    private void cyclePlayMode() {
        currentPlayMode = (currentPlayMode + 1) % 2; // 只在两种模式间切换
        musicService.setPlayMode(currentPlayMode);
        updatePlayModeIcon();
    }

    // 更新模式图标
    private void updatePlayModeIcon() {
        btn_playWay.setImageResource(modeIcons[currentPlayMode]);
        // 添加模式提示Toast
        String[] modeNames = {
                "列表循环",
                "单曲循环"
        };
        Toast.makeText(this, modeNames[currentPlayMode], Toast.LENGTH_SHORT).show();
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

        btn_play.setOnClickListener(listener1);
        btn_pre.setOnClickListener(listener1);
        btn_next.setOnClickListener(listener1);
        btn_playList.setOnClickListener(listener1);
        btn_playWay.setOnClickListener(listener1);

        listView = (ListView) findViewById(R.id.lv_music);

        layout = (ConstraintLayout) findViewById(R.id.constrainLayout);

        layout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                // 获取 ListView 的位置信息
                int[] listViewLocation = new int[2];
                listView.getLocationOnScreen(listViewLocation);

                // 判断触摸事件的坐标是否在 ListView 区域内
                if (event.getRawX() < listViewLocation[0] ||
                        event.getRawX() > listViewLocation[0] + listView.getWidth() ||
                        event.getRawY() < listViewLocation[1] ||
                        event.getRawY() > listViewLocation[1] + listView.getHeight()) {
                    // 如果触摸事件不在 ListView 区域内，则隐藏 ListView
                    listView.setVisibility(View.GONE);
                    return true; // 表示触摸事件已经被处理
                }
                return false; // 表示触摸事件未被处理
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
}