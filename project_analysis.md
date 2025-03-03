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
