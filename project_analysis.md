# 音乐播放器项目分析

## 项目概述
这是一个Android音乐播放器应用，使用ExoPlayer进行音乐播放，支持从本地assets目录加载音乐和歌词文件。现在需要修改为从HTTP服务器获取音乐和歌词文件，并下载到本地存储。

## 主要类和功能

### MainActivity.java
主要负责用户界面和交互逻辑。

#### 字段
- `ActionBar actionBar` - 应用的操作栏
- `TextView tv_songName, tv_seekBarHint, tv_duration, tv_lyrics, tv_singer` - 显示歌曲信息的文本视图
- `SeekBar seekBar` - 进度条
- `ImageButton btn_play, btn_pre, btn_next, btn_playList, btn_playWay, btn_like` - 控制按钮
- `ListView listView` - 显示歌曲列表
- `List<String> music_list` - 歌曲列表数据
- `MusicService musicService` - 音乐服务实例
- `Map<String, String> displayToOriginalMapping` - 显示名称和原始文件名的映射
- `int currentPlayMode` - 当前播放模式
- `Timer timer` - 用于更新进度的定时器
- `ProgressUpdate` - 内部类，用于定时更新UI显示

#### 方法
- `format(long position)` - 格式化时间
- `showListView()` - 显示歌曲列表
- `getMusic()` - 获取音乐列表
- `playMusic(String musicName)` - 播放指定音乐
- `playOrPauseMusic()` - 播放或暂停音乐
- `playPreviousTrack()` - 播放上一首音乐
- `playNextTrack()` - 播放下一首音乐
- `updateSongName(String songName)` - 更新歌曲名称显示，处理扩展名和下划线
- `updatePlayMode()` - 更新播放模式
- `updatePlayModeIcon()` - 更新播放模式图标
- `updateFavoriteIcon()` - 更新收藏状态图标
- `initView()` - 初始化视图
- `onCreate(Bundle savedInstanceState)` - 活动创建时执行
- `onDestroy()` - 活动销毁时执行
- `checkFavoriteMode()` - 检查收藏模式
- `updateLyricDisplay(String songName, long position)` - 更新歌词显示
- `updateUI()` - 统一更新UI界面
- `updateSongInfo(String title, String singer)` - 更新歌曲信息和歌手
- `updateProgress(long currentPosition, long duration)` - 更新进度条和时间

### MusicService.java
负责音乐播放的服务组件。

#### 字段
- `ExoPlayer player` - ExoPlayer实例
- `List<MediaItem> mediaItems` - 媒体项列表
- `int currentTrackIndex` - 当前播放的曲目索引
- `int playMode` - 播放模式
- `List<MediaItem> favoriteMediaItems` - 收藏歌曲列表
- `String currentSongName` - 缓存当前歌曲名称

#### 方法
- `init()` - 初始化播放器
- `isPlaying()` - 检查是否正在播放
- `play()` - 开始播放
- `pause()` - 暂停播放
- `playMusic(String name)` - 播放指定歌曲
- `getFileNameFromMediaItem(MediaItem mediaItem)` - 从媒体项获取文件名
- `playNextTrack()` - 播放下一首
- `playPreviousTrack()` - 播放上一首
- `seekTo(long position)` - 跳转到指定位置
- `getContentPosition()` - 获取当前播放位置
- `getDuration()` - 获取歌曲总时长
- `getCurrentSongName()` - 获取当前歌曲名称，返回原始文件名
- `getPlayMode()` - 获取播放模式
- `setPlayMode(int mode)` - 设置播放模式
- `handlePlaybackCompleted()` - 处理播放完成
- `updateFavoritePlaylist()` - 更新收藏歌曲列表
- `handleNearCompletion()` - 处理即将结束的歌曲
- `updateSongInfo()` - 更新歌曲信息显示，处理扩展名和下划线
- `updateProgress()` - 更新进度条和时间显示

### LyricManager.java
负责歌词管理。

#### 内部类
- `Lyric` - 封装歌词信息，包含时间戳和对应的歌词文本
  - `List<Long> times` - 时间戳列表
  - `List<String> lyrics` - 歌词文本列表
  - `findLyricIndex(long position)` - 根据播放位置查找歌词索引

#### 方法
- `getLyric(String songName, Context context)` - 获取歌词
- `getSinger(String songName, Context context)` - 获取歌手信息

### FavoriteManager.java
负责管理收藏的歌曲。

#### 字段
- `Set<String> favoriteSet` - 保存已收藏的歌曲文件名

#### 方法
- `initFavorites(Context context)` - 初始化收藏数据
- `saveFavorites(Context context)` - 保存收藏数据
- `isFavorite(String songName)` - 判断是否已收藏
- `addFavorite(Context context, String songName)` - 添加收藏
- `removeFavorite(Context context, String songName)` - 移除收藏
- `getFavorites()` - 获取所有收藏

### FileSyncManager.java
负责从服务器同步文件到本地存储。

#### 常量和字段
- `String[] SERVER_URLS` - 可能的服务器URL列表
- `List<String> prioritizedUrls` - 按优先级排序的服务器URL
- `String LAST_SERVER_URL_KEY` - 储存最后使用的服务器URL的键名

#### 方法
- `syncFiles(Context context, SyncCallback callback)` - 同步文件
- `getLocalMusicDir(Context context)` - 获取本地音乐目录
- `getLocalLyricsDir(Context context)` - 获取本地歌词目录
- `downloadFile(String serverUrl, String filename, File targetFile)` - 下载文件
- `findBestServerUrl(List<String> urls)` - 找到最佳服务器URL

## 缓存管理功能实现

### 功能特性
- 支持选择性清理和全量清理
- 文件类型识别（音乐/歌词）
- 文件大小格式化显示
- 滑动查看长列表
- 快速滚动支持

### 技术实现
- 使用 `getFilesDir()` 访问内部存储
- 统一缓存路径：
  - 音乐文件：`/data/data/com.example.musicplayer/files/music/`
  - 歌词文件：`/data/data/com.example.musicplayer/files/lyrics/`
- 优化列表性能：
  - 使用 ViewHolder 模式
  - 启用快速滚动
  - 优化滚动条显示

## 当前工作流程
1. MainActivity启动时绑定MusicService
2. MusicService初始化时读取assets/music目录下和本地存储中的所有音乐文件
3. 用户点击播放按钮或选择歌曲时，MusicService播放音乐
4. LyricManager从assets/lyrics目录下和本地存储读取歌词文件
5. 播放过程中更新进度条和歌词显示
6. FileSyncManager负责从服务器下载音乐和歌词文件到本地存储

## 问题修复记录

### 歌曲名称显示问题

#### 问题描述
歌曲名称在界面上显示不稳定，有时显示为格式化后的名称（如"Counting Stars"），有时显示为原始文件名（如"Counting_Stars.flac"）。

#### 根本原因分析
1. `MainActivity.updateSongName()`方法会处理文件名，去掉扩展名并将下划线替换为空格
2. 但是在多处代码中，UI更新逻辑不一致：
   - 定时任务`ProgressUpdate`中的歌词更新循环每次使用`getCurrentSongName()`直接获取原始文件名
   - UI界面各处直接调用`updateSongName()`方法处理歌曲名称
   - `MusicService.updateSongInfo()`只处理扩展名但不处理下划线

#### 解决方案
1. 修改`ProgressUpdate`类中的歌词更新逻辑，使用缓存的`lastSongName`而不是每次重新获取
2. 修改`MusicService.updateSongInfo()`方法，确保处理下划线与`MainActivity.updateSongName()`一致
3. 减少直接调用`updateSongName()`的地方，统一通过`updateUI()`方法更新UI

#### 修改的文件和方法
1. `MainActivity.java`：
   - 修改`ProgressUpdate.run()`中的歌词更新逻辑
   - 移除多余的`updateSongName()`调用
   - 在SeekBar拖动结束时不再直接更新歌曲名称
2. `MusicService.java`：
   - 更新`updateSongInfo()`方法，处理下划线与扩展名

#### 测试结果
修改后，歌曲名称显示保持稳定，无论是播放状态变化、切换歌曲还是拖动进度条，都能正确显示格式化后的歌曲名称。

### 缓存清理功能实现

#### 功能描述
点击界面上的point.png按钮，显示缓存清理对话框，用户可以选择性地清理缓存的歌曲和歌词文件，也可以一键清理所有缓存文件。

#### 实现方案
1. 创建缓存管理器处理文件列表获取和删除操作
2. 使用对话框展示缓存文件列表，允许用户选择要删除的文件
3. 提供确认机制防止误操作
4. 检测并提示当前播放歌曲是否在清理列表中

#### 新增文件和类
1. `CacheManager.java`：
   - 静态方法：`getCachedFiles()`、`calculateTotalSize()`、`deleteFiles()`、`formatFileSize()`
   - 内部类：`CachedFileAdapter` - 处理文件列表的显示和选择状态
2. 布局文件：
   - `dialog_cache_cleaner.xml` - 缓存清理对话框布局
   - `item_cached_file.xml` - 缓存文件列表项布局

#### 修改的文件和方法
1. `MainActivity.java`：
   - 新增方法：`showCacheCleanerDialog()` - 显示缓存清理对话框
   - 新增方法：`showConfirmCleanDialog()` - 显示确认清理对话框
   - 新增方法：`checkIfCurrentSongDeleted()` - 检查当前播放歌曲是否被清理
   - 修改`initView()`方法，添加btn_point按钮的点击事件处理

#### 调用方法
```java
// 在MainActivity中
// 1. 初始化缓存清理按钮并设置点击事件
ImageView btn_point = findViewById(R.id.btn_point);
btn_point.setOnClickListener(v -> showCacheCleanerDialog());

// 2. 显示缓存清理对话框
private void showCacheCleanerDialog() {
    // 创建对话框并设置布局
    final Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.dialog_cache_cleaner);
    
    // 获取缓存文件列表
    List<File> cachedFiles = CacheManager.getCachedFiles(this);
    
    // 显示文件列表并设置按钮事件
    // ...
}

// 3. 执行文件清理操作
int deletedCount = CacheManager.deleteFiles(selectedFiles);
```

#### 测试结果
功能实现后，用户可以通过点击point.png按钮查看和管理缓存文件，支持选择性清理和全量清理，并在删除当前播放的歌曲缓存时给予用户提示。

### 添加的函数
- `onStylingClick(View view)` - 处理主题按钮点击事件，启动 `ThemeSelectionActivity`
- `onBackButtonClick(View view)` - 处理主题界面返回按钮点击，结束 `ThemeSelectionActivity`

---

## 主要函数与用途总览

### MainActivity.java
- `initView()`：初始化主界面控件和布局，设置按钮监听。
- `onCreate(Bundle savedInstanceState)`：Activity创建入口，初始化界面与服务。
- `onDestroy()`：Activity销毁时资源释放。
- `updateSongName(String songName)`：格式化并显示当前歌曲名称。
- `checkFavoriteMode()`：切换收藏模式时检查收藏状态。
- `updatePlayMode()`：切换并刷新播放模式。
- `updatePlayModeIcon()`：更新播放模式图标。
- `updateFavoriteIcon()`：更新收藏按钮图标。
- `showListView()`：显示歌曲列表。
- `playMusic(String musicName)`：播放指定歌曲。
- `playOrPauseMusic()`：播放或暂停当前歌曲。
- `playNextTrack()`/`playPreviousTrack()`：播放下一首/上一首歌曲。
- `updateLyricDisplay(String, long)`：根据进度显示歌词。
- `updateUI()`：统一刷新界面。
- `showCacheCleanerDialog()`：弹出缓存清理对话框。
- `showConfirmCleanDialog()`：弹出清理确认对话框。
- `onStylingClick(View)`：主题按钮点击事件，启动主题选择界面。

### MusicService.java
- `init()`：初始化ExoPlayer和媒体列表。
- `isPlaying()`：判断是否正在播放。
- `play()`/`pause()`：控制播放与暂停。
- `playMusic(String)`：播放指定歌曲。
- `playNextTrack()`/`playPreviousTrack()`：播放下一首/上一首。
- `seekTo(long)`：跳转到指定播放位置。
- `getCurrentSongName()`：获取当前播放歌曲文件名。
- `getPlayMode()`/`setPlayMode(int)`：获取/设置播放模式。
- `updateFavoritePlaylist()`：刷新收藏歌曲列表。
- `refreshMusicList()`：重新加载本地音乐文件。
- `updateUiState()`：刷新通知栏与UI。
- `updateSongInfo(String, String)`：更新歌曲信息。

### LyricManager.java
- `getLyric(String, Context)`：加载指定歌曲的歌词。
- `getSinger(String, Context)`：根据歌词文件解析歌手名。
- `Lyric.findLyricIndex(long)`：根据播放进度查找歌词行。

### FavoriteManager.java
- `initFavorites(Context)`：初始化收藏数据。
- `addFavorite(Context, String)`/`removeFavorite(Context, String)`：添加/移除收藏。
- `getFavorites()`：获取所有收藏歌曲。
- `clearFavorites(Context)`：清空所有收藏。
- `saveFavorites(Context)`：保存收藏到本地。

### FileSyncManager.java
- `startSync()`：启动文件同步。
- `downloadFile(...)`：下载指定文件。
- `getCachedFiles(Context)`：获取本地缓存文件列表。
- `calculateTotalSize(List<File>)`：计算缓存文件总大小。

### ThemeSelectionActivity.java
- `onCreate(Bundle)`：初始化主题选择界面。
- `setBackgroundImage(String)`：保存主题背景设置。
- `onBackButtonClick(View)`：返回按钮事件。

### ThemeAdapter.java
- `getView(int, View, ViewGroup)`：渲染主题选择项。
- `getThemePreviewResId(String)`：获取主题预览图资源ID。

---

> 本节持续更新，便于开发者快速查找和理解各函数用途。
