# MelodyFlow 音径播放器 🎵

[![GitHub license](https://img.shields.io/github/license/MinerTob/android-MusicPlayer)](https://github.com/MinerTob/android-MusicPlayer/blob/main/LICENSE)

沉浸式音乐体验解决方案，集高效播放、精准歌词同步与优雅界面于一身。

## 核心功能 🎧

- 🎶 ExoPlayer 驱动的专业级音频解码
- 📜 实时歌词同步（支持中英文双语）
- 🔄 智能播放模式（列表循环/单曲循环/收藏循环）
- ❤️ 收藏管理系统（支持动态同步）
- 🎨 Material Design 交互界面
- 📂 原生音乐文件管理（assets 集成）
- 🎤 歌手信息智能匹配

## 快速入门 🚀

1. 将音乐文件放入 `app/src/main/assets/music/`
2. 将歌词文件放入 `app/src/main/assets/lyrics/`
3. 构建并运行应用
4. 点击播放列表选择歌曲
5. 使用控制面板管理播放

**特色功能**：
- 点击列表项自动跳转对应歌曲
- 收藏循环模式下自动过滤非收藏歌曲
- 播放中途修改收藏列表自动切换模式

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

## 项目结构 📂
MelodyFlow/
> app/
> > src/
> > > main/
> > > > java/com/example/musicplayer/
> > > > > FavoriteManager.java       # 收藏管理核心
> > > > > > isFavorite()             # 收藏状态判断
> > > > > > add/removeFavorite()      # 收藏操作
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
> > > > 
> > > > > MusicService.java          # 后台服务
> > > > > > ExoPlayer 集成           # 播放器核心
> > > > > > 播放模式管理             # 单曲/列表循环
> > > > > > getCurrentSongName()     # 获取当前曲目
> > > > 
> > > > res/
> > > > > layout/
> > > > > > activity_main.xml        # 主界面布局
> > > > > > list_item_song.xml        # 歌曲列表项布局  
> > > > > anim/
> > > > > > slide_out_top.xml         # 列表关闭动画
> > > > > drawable/                  # 图标资源
> > > > > > like.png                 # 未收藏图标
> > > > > > like_red.png             # 已收藏图标
> > > > > values/                   # 样式/字符串
> > > > 
> > > > assets/
> > > > > music/                   # 音频资源(.mp3)
> > > > > lyrics/                  # 歌词文件(.lrc)
> > > > 
> > > > AndroidManifest.xml        # 应用配置
> > > > > 服务声明                 # MusicService
> > > > > 存储权限                 # READ_EXTERNAL_STORAGE
> > > 
> > > build.gradle.kts             # 模块配置
> > > > 依赖声明                  # ExoPlayer/约束布局

> gradle/                       # 工程级配置
> build.gradle.kts              
> settings.gradle.kts

## 安装部署 📲
### 从源码运行
1. 克隆仓库
   ```bash
   git clone https://github.com/MinerTob/android-MusicPlayer
   ```
2. 使用 Android Studio 打开项目
3. 添加音乐文件至 `app/src/main/assets/music/`
4. 运行 `app` 模块

### 扩展开发
- 新增播放模式需修改 `MusicService.java`
- 修改收藏逻辑请参考 `FavoriteManager.java`
- 更新界面动画请调整 `res/anim/` 资源

## 开源协议 📜
本项目采用 MIT 协议，音乐文件仅供学习交流，商业使用请遵守版权法规。

## 致谢

在这个快节奏的时代，我们希望通过《MusicPlayer》唤起你对音乐的那份纯真与热爱。愿每一次音符的跳动都能触动你心灵深处最柔软的一隅，让音乐成为你生活中最美的风景。

愿你在音乐与代码的世界里，找到属于自己的快乐与自由！
