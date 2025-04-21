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
- 🧹 缓存清理系统（支持选择性和全量清理缓存文件）
  - 文件类型识别
  - 大小格式化显示
  - 滑动查看长列表
  - 快速滚动支持
  - 音乐与歌词文件关联管理
- 🎤 歌手信息智能匹配
- 🌈 主题切换：支持多种主题风格，随心切换界面背景
- 🖼️ 主题预览：主题选择界面带图片预览，所见即所得
- 📦 多格式支持：支持 .mp3、.flac、.m4a、.wav、.ogg 等多种音频格式与 .lrc、.txt 歌词格式
- 🔗 智能服务器连接：自动切换多服务器地址，支持真机/模拟器环境自动适配
- 🚦 同步状态UI：同步进度条与状态文本动态显示，支持错误提示与恢复
- 🛡️ 错误恢复机制：服务器/下载失败自动切换备选地址，断点续传与错误提示

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
- 缓存管理支持音乐与歌词文件关联清理
- 进入主题设置，体验主题切换与界面自定义
- 使用缓存清理功能，管理本地存储空间
- 支持本地与网络曲库切换，随时畅听

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
      "https://minertob.onrender.com",  // 主服务器
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
  
  // CacheManager.java
  public static class SongFileGroup {
      private final String displayName;
      private final File musicFile;
      private final File lyricFile;
    
      // 获取总大小（包含歌词文件）
      public long getTotalSize() {
          long size = 0;
          if (musicFile != null) {
             size += musicFile.length();
          }
          if (lyricFile != null) {
             size += lyricFile.length();
          }
          return size;
      }
  }

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

## 📚 代码结构与函数说明

本项目所有主要函数的用途与开发细节，详见 `project_analysis.md`。

- 如果你需要查找某个功能的实现位置或函数说明，请优先查阅 `project_analysis.md`，该文档会持续维护和补充。

### 主题系统
- 主界面与主题选择界面高度解耦，便于扩展新主题

### 文件同步
- 多种HTTP目录格式自动识别，支持断点续传与本地新鲜度检查

### 收藏与同步
- 本地/网络曲库收藏状态自动同步，切换曲库收藏无缝体验

### UI体验
- 主题、收藏、同步、歌词等交互均有实时反馈与动画优化

## 项目结构 📂

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
> > > > > ThemeSelectionActivity.java      # 主题选择界面
> > > > > ThemeAdapter.java               # 主题适配器与预览
> > > > > CacheManager.java               # 缓存管理与清理逻辑
> > > > > project_analysis.md             # 所有函数用途与结构说明
> > > > res/
> > > > > layout/
> > > > > > activity_main.xml        # 主界面布局
> > > > > > list_item_song.xml       # 歌曲列表项布局  
> > > > > anim/

## 更新日志 📝

### v1.5.0 (2025-04-21)
- 🌈 新增主题切换与主题选择界面，支持多主题背景与预览
- 🖼️ 主题图片预览与即时切换体验
- 🛡️ 文件同步与错误恢复机制增强，支持多服务器自动切换
- 🧹 缓存清理功能完善，界面与交互优化
- 📦 支持更多音频与歌词格式，兼容性提升
- 🛠️ 代码结构优化，所有函数用途已整理至 project_analysis.md
  
### v1.4.0 (2025-03-02)
- ✨ 新增HTTP服务器文件同步系统
- 🔄 优化服务器地址自动切换机制
- 🛠️ 修复歌曲名称显示不稳定问题
- 🔧 优化播放模式和收藏系统逻辑
- 📱 改进UI响应性和歌词同步
- 🧹 新增缓存管理功能，支持选择性清理和全量清理缓存文件
- 📂 实现音乐与歌词文件关联管理，删除音乐时自动删除对应歌词

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

MinerTob - [在GitHub上查看更多项目](https://github.com/MinerTob)

## 许可证 📄

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 开源协议 📜
本项目采用 MIT 协议，音乐文件仅供学习交流，商业使用请遵守版权法规。

## 致谢

在这个快节奏的时代，我们希望通过《音径播放器》唤起你对音乐的那份纯真与热爱。愿每一次音符的跳动都能触动你心灵深处最柔软的一隅，让音乐成为你生活中最美的风景。

愿你在音乐与代码的世界里，找到属于自己的快乐与自由！

下个版本逻辑要大改实现这些功能

1.保留清理缓存的功能

2.在playlist播放列表中添加本地曲库和网络曲库按钮，用于切换曲库

3.还是每次启动或点击现有的刷新图表根据服务端实时更新歌曲但不要下载下来

4.在歌曲列表内部的最顶上添加关键词收索功能，找到网络曲库的对应歌曲

5.网络曲库的播放和本地曲库一致，并且两个曲库要同步收藏歌曲，但是本地曲库只显示本地有的收藏歌曲

6.当用户点击download.png时询问用户下载当前正在播放的歌曲到本地，然后同时下载歌曲和歌词

7.现在的所有函数和功能不准改，写函数时要严格参考project_analysis.md文件
