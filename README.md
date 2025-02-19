# MelodyFlow 音乐播放器 🎵

[![GitHub license](https://img.shields.io/github/license/yourname/melodyflow)](https://github.com/yourname/melodyflow/blob/main/LICENSE)
[![Android CI](https://github.com/yourname/melodyflow/actions/workflows/android.yml/badge.svg)](https://github.com/yourname/melodyflow/actions/workflows/android.yml)

沉浸式音乐体验解决方案，集高效播放、精准歌词同步与优雅界面于一身。

![应用截图](screenshots/player_interface.png) <!-- 需要添加实际截图 -->

## 核心功能 🎧

- 🎶 ExoPlayer 驱动的专业级音频解码
- 📜 实时歌词同步（支持中英文双语）
- 🔁 智能播放模式（列表循环/单曲循环）
- 🎨 Material Design 交互界面
- 📂 原生音乐文件管理（assets 集成）
- 🎤 歌手信息智能匹配

## 快速入门 🚀

1. 将音乐文件放入 `app/src/main/assets/music/`
2. 将歌词文件放入 'app/src/main/assets/lyrics/'
3. 构建并运行应用
4. 点击播放列表选择歌曲
5. 使用控制面板管理播放

**提示**：支持手势操作 - 滑动进度条可快速定位播放位置

## 技术架构 🛠️

### 播放核心
- ExoPlayer 深度集成
- Service 后台播放
- 播放状态持久化：
  ```java
  // MusicService.java
  player.addListener(new Player.Listener() {
      @Override
      public void onPlaybackStateChanged(int state) {
          // 处理播放状态变化
      }
  });
  ```

### 歌词引擎
- 毫秒级时间轴匹配：
  ```java
  // Dandelion_Promise.java
  public static int findLyricIndex(long position) {
      for (int i = LYRIC_TIMES.length - 1; i >= 0; i--) {
          if (position >= LYRIC_TIMES[i][0]) return i;
      }
      return 0;
  }
  ```
- 动态歌词渲染（支持多行显示）

### 播放模式
- 智能状态切换逻辑：
  ```java
  // MainActivity.java
  private void cyclePlayMode() {
      currentPlayMode = (currentPlayMode + 1) % 2;
      musicService.setPlayMode(currentPlayMode);
      updatePlayModeIcon();
  }
  ```

## 项目结构 📂
+```text
+音径播放器/
+│
+├── 应用程序/
+    ├── 来源/
+    │   ├── 主打/
+    │   │   ├── java/com/example/musicplayer/
+    │   │   │   ├── MainActivity.java
+    │   │   │   ├── MusicService.java
+    │   │   │   ├── LyricManager.java
+    │   │   │   ├── Dandelion_Promise.java
+    │   │   │   └── Counting_Stars.java
+    │   ├── res/
+    │   │   ├── 版面/
+    │   │   │   └── activity_main.xml
+    │   │   └── ...(其他资源文件)
+    │   └── AndroidManifest.xml
+    └── ...(其他项目配置)
+└── README.md
+```

## 安装部署 📲
### 从Release下载
1. 访问 [GitHub Releases](https://github.com/MinerTob/MusicPlayer-android/releases/tag/release1.2.0)
2. 下载最新版APK
3. 按照系统提示安装

### 从源码运行
1. 克隆仓库
   ```bash
   git clone https://github.com/yourname/melodyflow.git
   ```
2. 使用 Android Studio 打开项目
3. 添加音乐文件至 `app/src/main/assets/music/`
4. 运行 `app` 模块

### 扩展开发
- 新增歌词类需继承基础模板
- 修改 `LyricManager.java` 注册新歌词
- 更新播放列表适配新格式

## 开源协议 📜
本项目采用 MIT 协议，音乐文件仅供学习交流，商业使用请遵守版权法规。

## 致谢

在这个快节奏的时代，我们希望通过《MusicPlayer》唤起你对音乐的那份纯真与热爱。愿每一次音符的跳动都能触动你心灵深处最柔软的一隅，让音乐成为你生活中最美的风景。

愿你在音乐与代码的世界里，找到属于自己的快乐与自由！
