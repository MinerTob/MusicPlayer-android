# MusicPlayer

《MusicPlayer》是一个充满文艺气息的音乐播放器项目，旨在为用户呈现一场视听盛宴。项目不仅实现了可靠的音乐播放功能，还融入了歌词同步、播放模式切换与优雅的界面交互。每一个文件都承载着开发者对音乐的热忱和对代码美学的追求。

## 项目亮点

- **音乐播放核心**  
  基于 [ExoPlayer](https://exoplayer.dev/) 实现稳定高效的音乐播放。通过 `MusicService` 负责加载音乐资源、管理播放队列以及处理播放模式，为用户提供流畅的音乐体验。

- **歌词同步展示**  
  内置 `LyricManager` 模块，根据播放进度实时更新歌词显示。同时，通过 `Dandelion_Promise.java` 与 `Counting_Stars.java` 两个歌词类，展示了不同歌曲的歌词和对应歌手信息，让每一句歌词都显得生动而有感。

- **交互与布局**  
  主界面由 `activity_main.xml` 定义，采用 `ConstraintLayout` 布局，实现了播放、暂停、上一曲、下一曲以及列表选择等多种操作。直观友好的用户界面保证了极佳的用户体验。

- **播放模式切换**  
  支持列表循环和单曲循环两种播放模式，用户可以根据自身喜好自由切换。界面上直观的图标变化和提示信息，更增加了互动乐趣。

## 主要模块介绍

### MainActivity.java

- 应用入口，负责初始化界面及控件、绑定 `MusicService` 并与之交互。  
- 实现了音乐播放、暂停、上一曲、下一曲、播放列表展示等多种操作，同时利用定时器任务更新播放进度和歌词展示。

### MusicService.java

- 后台服务核心，基于 ExoPlayer 实现音乐播放。  
- 负责加载 assets 中的音乐文件，并管理播放队列、当前播放索引以及播放模式。  
- 同时，提供了播放、暂停、快进、快退等基本控制接口，确保音乐无缝衔接。

### LyricManager.java

- 提供歌词文件与歌曲名称间的映射功能，通过解析歌曲名返回对应的歌词类（如 `Dandelion_Promise` 或 `Counting_Stars`）。
- 便于在主界面中根据当前播放歌曲获取歌词文本及歌手信息。

### Dandelion_Promise.java & Counting_Stars.java

- 分别封装了《蒲公英的约定》和《Counting Stars》的歌词时间戳与文本数据。
- 内含歌词查找方法 `findLyricIndex`，实现了基于当前播放进度的歌词匹配功能，同时定义了对应的歌手信息。

### AndroidManifest.xml

- 声明了所有的应用组件，包括 `MainActivity` 与 `MusicService`。
- 配置了应用的主题、图标等必要属性，确保应用正确运行。

### activity_main.xml

- 定义了应用的主界面布局，采用 `ConstraintLayout` 实现响应式设计。
- 包含了 `SeekBar`、`ImageButton`、`ListView`、`TextView` 等控件，实现了直观的播放控制和信息展示。

## 项目结构


MusicPlayer/
│
├── app/
│ ├── src/
│ │ ├── main/
│ │ │ ├── java/com/example/musicplayer/
│ │ │ │ ├── MainActivity.java
│ │ │ │ ├── MusicService.java
│ │ │ │ ├── LyricManager.java
│ │ │ │ ├── Dandelion_Promise.java
│ │ │ │ └── Counting_Stars.java
│ │ │ ├── res/
│ │ │ │ ├── layout/
│ │ │ │ │ └── activity_main.xml
│ │ │ │ └── ... (其他资源文件)
│ │ │ └── AndroidManifest.xml
│ └── ... (其他项目配置)
└── README.md

## 使用说明

1. **开发环境准备**  
   - 使用 Android Studio 导入本项目，确保 Gradle 及依赖项配置正确。
   - 项目基于 ExoPlayer 实现音乐播放功能，请确认已具备相关依赖库（通常在 `build.gradle` 中配置）。

2. **运行项目**  
   - 将项目导入至 Android Studio 后，点击运行按钮即可在模拟器或真机上启动应用。
   - 应用启动后即可进入主界面，点击各功能按钮体验音乐播放、歌词同步及播放模式切换。

3. **扩展与定制**  
   - 若需增加更多歌曲和歌词，请在 `assets/music` 文件夹中加入音乐文件，并为相应歌曲新增歌词类。
   - 可以依据个人喜好修改 `activity_main.xml` 布局或各图标、色彩，以达到个性化定制效果。

## 版权与声明

- 本项目为开源项目，欢迎各位开发者贡献代码或提出改进建议。  
- 音乐文件及歌词数据仅供学习交流使用，禁止用于商业用途，请遵守相关版权法规。

## 致谢

在这个快节奏的时代，我们希望通过《MusicPlayer》唤起你对音乐的那份纯真与热爱。愿每一次音符的跳动都能触动你心灵深处最柔软的一隅，让音乐成为你生活中最美的风景。

愿你在音乐与代码的世界里，找到属于自己的快乐与自由！
