# MelodyFlow 音径播放器 🎵

[![GitHub license](https://img.shields.io/github/license/MinerTob/android-MusicPlayer)](https://github.com/MinerTob/android-MusicPlayer/blob/main/LICENSE)

沉浸式音乐体验解决方案，集高效播放、精准歌词同步与优雅界面于一身。

## 核心功能 🎧

- 🎶 ExoPlayer 驱动的专业级音频解码
- 📜 实时歌词同步（支持中英文双语）
- 🔄 智能播放模式（列表循环/单曲循环/收藏循环）
- ❤️ 收藏管理系统（支持动态同步）
- 🎨 Material Design 交互界面
- 🌐 HTTP服务器音乐文件同步（自动检测最佳服务器）
- 📂 本地音乐文件缓存（优先播放下载文件，无下载自动回退至assets）
- 🎤 歌手信息智能匹配

## 快速入门 🚀

1. 将音乐文件(.flac, .mp3等格式)放入服务器对应目录
2. 将歌词文件(.lrc格式)放入服务器对应目录
3. 构建并运行应用，应用会自动同步音乐和歌词文件
4. 点击播放列表选择歌曲
5. 使用控制面板管理播放

**特色功能**：
- 点击列表项自动跳转对应歌曲
- 收藏循环模式下自动过滤非收藏歌曲
- 播放中途修改收藏列表自动切换模式
- 智能服务器地址检测（支持模拟器和真机运行环境）

## 技术亮点 ✨

### 播放核心
- ExoPlayer 深度集成
- Service 后台播放
- 播放状态持久化：
  ```java
  // MusicService.java
  player.addListener(new Player.Listener() {
      @Override
      public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
          // 处理歌曲切换事件
      }
  });
  ```

### 收藏系统
- 智能模式切换：
  ```java
  // MusicService.java
  public void setPlayMode(int mode) {
      if (mode == MODE_FAVORITE) {
          // 自动跳转到最近的收藏歌曲
          if (!isCurrentSongFavorite) {
              currentTrackIndex = nearestFavoriteIndex;
          }
      }
  }
  ```

### 歌词引擎
- 毫秒级时间轴匹配：
  ```java
  // LyricManager.java
  public static int findLyricIndex(long position) {
      for (int i = LYRIC_TIMES.length - 1; i >= 0; i--) {
          if (position >= LYRIC_TIMES[i][0]) return i;
      }
      return 0;
  }
  ```

### 文件同步系统
- 多服务器地址智能切换：
  ```java
  // FileSyncManager.java
  private static final String[] SERVER_URLS = {
      "http://minertob.s.odn.cc",  // 主服务器
      "http://10.0.2.2:3000"       // 模拟器环境备选
  };
  ```
  你可以选择自己制作歌曲部署局域网或公网服务器，只要你的歌曲歌词文件满足assets/music/和assets/lyrics/格式
  
- HTTP目录列表解析：
  ```java
  // FileSyncManager.java
  // 三种模式匹配不同服务器的目录列表格式
  Pattern pattern1 = Pattern.compile("<a href=\"([^\"]+\\.(mp3|flac|m4a|wav|ogg|lrc|txt))\">");
  Pattern pattern2 = Pattern.compile("href=\"([^\"]+\\.(mp3|flac|m4a|wav|ogg|lrc|txt))\"");
  Pattern pattern3 = Pattern.compile("<a href=\"([^\"]+)\"");
  ```

### UI优化
- 歌曲名称智能处理：
  ```java
  // MainActivity.java
  public void updateSongName(String songName) {
      // 检查文件名是否包含扩展名
      int dotIndex = songName.lastIndexOf(".");
      if (dotIndex > 0) {
          // 去掉文件名后缀，并将下划线替换为空格
          songName = songName.substring(0, dotIndex).replace("_", " ");
      }
      tv_songName.setText(songName);
  }
  ```

## 项目结构 📂
MelodyFlow/
> app/
> > src/
> > > main/
> > > > java/com/example/musicplayer/
> > > > > FavoriteManager.java       # 收藏管理核心
> > > > > > isFavorite()             # 收藏状态判断
> > > > > > add/removeFavorite()     # 收藏操作
> > > > 
> > > > > FileSyncManager.java       # 文件同步核心
> > > > > > syncFiles()              # 文件同步入口
> > > > > > downloadFile()           # 文件下载功能
> > > > > > findBestServerUrl()      # 服务器选择策略
> > > > 
> > > > > LyricManager.java          # 歌词管理核心
> > > > > > Lyric 内部类             # 歌词数据容器
> > > > > > getLyric()               # 动态解析LRC文件
> > > > > > getSinger()              # 歌手信息提取
> > > > 
> > > > > MainActivity.java          # 主界面控制器
> > > > > > 播放控制逻辑             # 播放/暂停/切歌
> > > > > > 收藏同步机制             # updateFavoriteIcon()
> > > > > > 播放列表管理             # SongAdapter
> > > > > > 文件同步UI               # textSyncStatus, progressSync
> > > > 
> > > > > MusicService.java          # 后台服务
> > > > > > ExoPlayer 集成           # 播放器核心
> > > > > > 播放模式管理             # 单曲/列表循环
> > > > > > getCurrentSongName()     # 获取当前曲目
> > > > > > updateSongInfo()         # 歌曲信息更新
> > > > 
> > > > res/
> > > > > layout/
> > > > > > activity_main.xml        # 主界面布局
> > > > > > list_item_song.xml       # 歌曲列表项布局  
> > > > > anim/

## 更新日志 📝

### v1.4.0 (2025-03-02)
- ✨ 新增HTTP服务器文件同步系统
- 🔄 优化服务器地址自动切换机制
- 🛠️ 修复歌曲名称显示不稳定问题
- 🔧 优化播放模式和收藏系统逻辑
- 📱 改进UI响应性和歌词同步

### v1.3.0
- 添加收藏系统
- 增强播放模式选择
- 优化UI响应速度

### v1.2.0
- 添加歌词显示功能
- 改进播放控制逻辑
- 优化内存使用

### v1.0.0
- 初始版本发布
- 基本音乐播放功能
- 播放列表管理

## 开发者 👨‍💻

MinerTob Team - [在GitHub上查看更多项目](https://github.com/MinerTob)

## 许可证 📄

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件
