package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件同步管理器 - 负责从HTTP服务器下载音乐和歌词文件到本地存储
 */
public class FileSyncManager {
    private static final String TAG = "FileSyncManager";
    
    // 音频文件扩展名
    public static final String[] MUSIC_EXTENSIONS = { ".mp3", ".flac", ".m4a", ".wav", ".ogg" };
    // 歌词文件扩展名
    public static final String[] LYRIC_EXTENSIONS = { ".lrc", ".txt" };
    // 可用的服务器URL列表
    private static final String[] SERVER_URLS = {
        "http://minertob.s.odn.cc",      // 本地服务器
    };
    
    // 首选URL存储键
    private static final String PREF_LAST_SERVER = "last_successful_server";
    
    // 存储最后一次成功连接的服务器URL
    private static String lastSuccessfulServerUrl = null;
    
    // 状态监听器接口
    public interface SyncStatusListener {
        void onSyncStarted();
        void onSyncProgress(int current, int total, String filename);
        void onSyncProgress(String fileName, int progress); // 新增单个文件进度监听
        void onSyncCompleted(int total);
        void onSyncError(String errorMessage);
    }
    
    private Context context;
    private SyncStatusListener syncStatusListener;
    private ExecutorService executor;
    
    // 默认超时时间（毫秒）
    private static final int DEFAULT_TIMEOUT = 30000; // 30秒
    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;
    
    private boolean isCancelled = false;
    private boolean forceDownload = false;
    
    public FileSyncManager(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public void setSyncStatusListener(SyncStatusListener listener) {
        this.syncStatusListener = listener;
    }
    
    /**
     * 获取当前设置的同步状态监听器
     * @return 当前的同步状态监听器，如果未设置则返回null
     */
    public SyncStatusListener getSyncStatusListener() {
        return this.syncStatusListener;
    }
    
    // 执行同步
    public void startSync() {
        new Thread(() -> {
            if (syncStatusListener != null) {
                syncStatusListener.onSyncStarted();
            }
            
            try {
                String serverUrl = detectAvailableServer();
                if (serverUrl == null) {
                    if (syncStatusListener != null) {
                        syncStatusListener.onSyncError("无法连接到服务器");
                    }
                    return;
                }
                
                // 记录最后成功的服务器URL
                saveLastSuccessfulServer(serverUrl);
                
                // 确保下载目录存在
                ensureDownloadDirExists();
                
                // 同步音乐文件
                String musicPath = "/assets/music/";
                Log.d(TAG, "正在请求音乐文件列表: " + serverUrl + musicPath);
                List<String> musicFiles = getRemoteFileList(serverUrl, musicPath, MUSIC_EXTENSIONS);
                
                // 同步歌词文件
                String lyricsPath = "/assets/lyrics/";
                Log.d(TAG, "正在请求歌词文件列表: " + serverUrl + lyricsPath);
                List<String> lyricFiles = getRemoteFileList(serverUrl, lyricsPath, LYRIC_EXTENSIONS);
                
                // 合并所有要同步的文件
                List<String> allFiles = new ArrayList<>();
                List<String> allRemotePaths = new ArrayList<>();
                
                for (String file : musicFiles) {
                    allFiles.add(file);
                    allRemotePaths.add("/assets/music/");
                }
                
                for (String file : lyricFiles) {
                    allFiles.add(file);
                    allRemotePaths.add("/assets/lyrics/");
                }
                
                // 总文件数
                int totalFiles = allFiles.size();
                int syncedFiles = 0;
                
                // 记录实际同步的文件数
                int actualSyncedFiles = 0;
                
                // 开始同步
                for (int i = 0; i < totalFiles; i++) {
                    String fileName = allFiles.get(i);
                    String remotePath = allRemotePaths.get(i);
                    
                    // 更新进度 - 显示解码后的文件名
                    String displayName = decodeUrlFileName(fileName);
                    if (syncStatusListener != null) {
                        syncStatusListener.onSyncProgress(syncedFiles, totalFiles, displayName);
                    }
                    
                    // 判断本地是否已存在此文件
                    File localFile = getLocalFile(fileName, remotePath.contains("lyrics"));
                    if (!localFile.exists() || isFileOutdated(localFile)) {
                        // 下载文件
                        boolean success = downloadFile(serverUrl, remotePath, fileName, localFile, new FileSyncProgress() {
                            @Override
                            public void onProgress(int progress) {
                                if (syncStatusListener != null) {
                                    syncStatusListener.onSyncProgress(fileName, progress);
                                }
                            }
                        });
                        if (success) {
                            actualSyncedFiles++;
                        }
                    }
                    
                    syncedFiles++;
                }
                
                // 同步完成
                if (syncStatusListener != null) {
                    // 添加日志确认同步完成
                    Log.d(TAG, "文件同步完成，开始通知监听器，同步文件数: " + actualSyncedFiles);
                    
                    // 确保在主线程上调用监听器
                    final int finalSyncedFiles = actualSyncedFiles;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        syncStatusListener.onSyncCompleted(finalSyncedFiles);
                        Log.d(TAG, "已通知监听器同步完成");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "同步过程中发生错误", e);
                if (syncStatusListener != null) {
                    // 确保在主线程上调用监听器
                    final String errorMsg = e.getMessage();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        syncStatusListener.onSyncError("同步过程中发生错误: " + errorMsg);
                    });
                }
            }
        }).start();
    }
    
    /**
     * 同步文件前确保下载目录存在
     */
    private void ensureDownloadDirExists() {
        File dir = new File(context.getFilesDir() + File.separator + "download");
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                Log.e(TAG, "创建下载目录失败: " + dir.getAbsolutePath());
            } else {
                Log.d(TAG, "成功创建下载目录: " + dir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 获取本地文件对象
     * @param fileName 文件名
     * @param isLyric 是否为歌词文件
     * @return 本地文件对象
     */
    private File getLocalFile(String fileName, boolean isLyric) {
        File dir;
        if (isLyric) {
            dir = new File(context.getFilesDir(), "lyrics");
        } else {
            dir = new File(context.getFilesDir(), "music");
        }
        
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 解码URL编码的文件名
        String decodedFileName = decodeUrlFileName(fileName);
        
        return new File(dir, decodedFileName);
    }
    
    /**
     * 获取本地歌词文件目录
     * @param context 上下文对象
     * @return 歌词文件目录
     */
    public static File getLocalLyricsDir(Context context) {
        File dir = new File(context.getFilesDir(), "lyrics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /**
     * 获取本地音乐文件目录
     * @param context 上下文对象
     * @return 音乐文件目录
     */
    public static File getLocalMusicDir(Context context) {
        File dir = new File(context.getFilesDir(), "music");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    /**
     * 判断文件是否需要更新
     * @param file 本地文件
     * @return 是否需要更新
     */
    private boolean isFileOutdated(File file) {
        // 这里可以实现更复杂的逻辑，例如基于文件修改时间或哈希值
        // 目前简单实现：如果文件大小为0，认为需要更新
        return file.length() == 0;
    }
    
    /**
     * 检测可用的服务器URL
     */
    private String detectAvailableServer() {
        // 先尝试加载上次成功的服务器地址
        String lastServer = getLastSuccessfulServer();
        if (lastServer != null) {
            Log.d(TAG, "检测上次成功的服务器地址: " + lastServer);
            if (isServerAvailable(lastServer)) {
                Log.d(TAG, "上次成功的服务器地址可用: " + lastServer);
                return lastServer;
            } else {
                Log.d(TAG, "上次成功的服务器地址不可用: " + lastServer);
            }
        }

        Log.d(TAG, "开始检测可用的服务器...");
        
        // 检查是否为模拟器环境
        boolean isEmulator = isEmulator();
        Log.d(TAG, "当前环境是否为模拟器: " + isEmulator);
        
        // 根据环境选择合适的URL优先级
        List<String> prioritizedUrls = new ArrayList<>();
        
        if (isEmulator) {
            // 模拟器优先使用10.0.2.2
            prioritizedUrls.add("http://minertob.s.odn.cc");
            prioritizedUrls.add("http://minertob.s.odn.cc");
        } else {
            // 真机优先使用127.0.0.1和局域网地址
            prioritizedUrls.add("http://minertob.s.odn.cc");
            prioritizedUrls.add("http://minertob.s.odn.cc");
            prioritizedUrls.add("http://minertob.s.odn.cc");
        }
        
        // 添加其他未包含的SERVER_URLS中的地址
        for (String url : SERVER_URLS) {
            if (!prioritizedUrls.contains(url)) {
                prioritizedUrls.add(url);
            }
        }
        
        // 尝试所有可能的服务器URL
        for (String url : prioritizedUrls) {
            Log.d(TAG, "正在测试服务器: " + url);
            if (isServerAvailable(url)) {
                Log.d(TAG, "找到可用的服务器: " + url);
                return url;
            }
        }
        
        Log.e(TAG, "无法连接到任何服务器");
        return null;
    }

    /**
     * 判断当前是否在模拟器环境中运行
     */
    private boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
    
    /**
     * 获取上次成功连接的服务器URL
     */
    private String getLastSuccessfulServer() {
        SharedPreferences prefs = context.getSharedPreferences("FileSyncPrefs", Context.MODE_PRIVATE);
        return prefs.getString(PREF_LAST_SERVER, null);
    }
    
    /**
     * 保存成功连接的服务器URL
     */
    private void saveLastSuccessfulServer(String serverUrl) {
        SharedPreferences prefs = context.getSharedPreferences("FileSyncPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_LAST_SERVER, serverUrl).apply();
        Log.d(TAG, "已保存成功的服务器URL: " + serverUrl);
    }
    
    /**
     * 检测服务器是否可用
     * @param serverUrl 服务器URL
     * @return 服务器是否可用
     */
    private boolean isServerAvailable(String serverUrl) {
        try {
            // 尝试访问一个已知路径 - assets/music目录
            URL url = new URL(serverUrl + "/assets/music/");
            Log.d(TAG, "尝试连接: " + url.toString());
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            connection.setRequestMethod("GET");  // 改用GET请求而不是HEAD
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");  // 设置UA
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "服务器响应状态码: " + responseCode);
            
            return (responseCode >= 200 && responseCode < 400);
        } catch (Exception e) {
            Log.d(TAG, "服务器连接失败: " + serverUrl + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 从服务器获取文件列表
     * 
     * @param serverUrl 服务器URL
     * @param remotePath 远程路径
     * @param extensions 文件扩展名列表
     * @return 文件名列表
     */
    private List<String> getRemoteFileList(String serverUrl, String remotePath, String[] extensions) {
        List<String> fileList = new ArrayList<>();
        
        // 确保路径格式正确
        if (!remotePath.startsWith("/")) {
            remotePath = "/" + remotePath;
        }
        if (!remotePath.endsWith("/")) {
            remotePath = remotePath + "/";
        }
        
        String url = serverUrl + remotePath;
        Log.d(TAG, "完整URL: " + url);
        
        try {
            // 尝试获取目录列表
            String htmlContent = downloadContent(url);
            
            if (htmlContent != null && !htmlContent.isEmpty()) {
                // 记录调试信息
                Log.d(TAG, "服务器响应内容长度: " + htmlContent.length() + " 字节");
                Log.d(TAG, "HTML内容前100字符: " + htmlContent.substring(0, Math.min(100, htmlContent.length())));
                
                // 使用正则表达式解析目录列表
                parseFilesFromHtml(htmlContent, joinExtensions(extensions), fileList);
            } else {
                Log.d(TAG, "服务器未返回内容或内容为空");
            }
            
            // 如果目录浏览被禁用或返回空目录，直接尝试获取已知文件名
            if (fileList.isEmpty()) {
                // 尝试常见文件名模式
                tryCommonFileNames(serverUrl, remotePath, extensions, fileList);
            }
        } catch (IOException e) {
            Log.e(TAG, "获取远程文件列表失败", e);
        }
        
        return fileList;
    }
    
    /**
     * 从HTML文本中解析文件列表
     */
    private void parseFilesFromHtml(String html, String extensions, List<String> fileList) {
        // 定义文件链接的正则表达式模式
        // 这个模式匹配href属性中的链接，并捕获文件名
        String linkPattern = "href=[\"']([^\"']*\\.(" + extensions + "))[\"']";
        Pattern pattern = Pattern.compile(linkPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        
        int filesFound = 0;
        
        // 查找所有匹配项
        while (matcher.find()) {
            String fileName = matcher.group(1);
            
            // 尝试对URL编码的文件名进行解码
            try {
                String decodedFileName = java.net.URLDecoder.decode(fileName, "UTF-8");
                Log.d(TAG, "解析到文件（已解码）: " + decodedFileName);
                fileName = decodedFileName;
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "解码文件名出错: " + e.getMessage());
                // 如果解码失败，使用原始文件名
                Log.d(TAG, "解析到文件（原始）: " + fileName);
            }
            
            // 如果文件名包含路径分隔符，只取最后的文件名部分
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            }
            
            // 添加到文件列表中
            if (!fileList.contains(fileName)) {
                fileList.add(fileName);
                filesFound++;
            }
        }
        
        if (filesFound > 0) {
            Log.d(TAG, "成功从HTML解析出 " + filesFound + " 个文件");
        } else {
            Log.d(TAG, "未能从HTML中解析出文件列表");
            // 输出HTML的前200个字符用于调试
            String htmlPreview = html.length() > 200 ? html.substring(0, 200) : html;
            Log.d(TAG, "HTML前200字符: " + htmlPreview);
        }
    }
    
    /**
     * 拼接扩展名为正则表达式形式
     */
    private String joinExtensions(String[] extensions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extensions.length; i++) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(extensions[i].replace(".", ""));
        }
        return sb.toString();
    }
    
    /**
     * 当服务器不支持目录浏览时，尝试直接访问常见文件名
     */
    private void tryCommonFileNames(String serverUrl, String remotePath, String[] extensions, List<String> fileList) throws IOException {
        // 尝试常见的音乐文件名
        String[] commonFileNames = {
            "song", "music", "track", "audio", "花海", "菊花台", "青花瓷",
            "01", "02", "03", "track01", "track02",
            "一千年以后", "十年", "爱你", "遇见", "蒲公英的约定", "稻香"
        };
        
        // 确保路径格式正确
        if (!remotePath.startsWith("/")) {
            remotePath = "/" + remotePath;
        }
        if (!remotePath.endsWith("/")) {
            remotePath = remotePath + "/";
        }
        
        // 改进URL构建
        String baseUrl = serverUrl + remotePath;
        Log.d(TAG, "尝试访问路径基础URL: " + baseUrl);
        
        for (String name : commonFileNames) {
            for (String ext : extensions) {
                String fileName = name + ext;
                String fileUrl = baseUrl + fileName;
                
                Log.d(TAG, "尝试访问文件: " + fileUrl);
                if (isFileExistOnServer(fileUrl)) {
                    Log.d(TAG, "文件存在: " + fileName);
                    fileList.add(fileName);
                }
            }
        }
    }
    
    /**
     * 检查文件是否存在于服务器
     * @param fileUrl 文件URL
     * @return 文件是否存在
     */
    private boolean isFileExistOnServer(String fileUrl) {
        HttpURLConnection connection = null;
        try {
            // 确保URL格式正确
            if (fileUrl.contains(" ")) {
                fileUrl = fileUrl.replace(" ", "%20");
                Log.d(TAG, "URL包含空格，已转义: " + fileUrl);
            }
            
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET"); // 改用GET代替HEAD，有些服务器可能不支持HEAD
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "检查文件 " + fileUrl + " 状态码: " + responseCode);
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            Log.e(TAG, "检查文件存在失败: " + fileUrl + ", 错误: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 下载文件到本地
     * @param serverUrl 服务器URL
     * @param remotePath 远程路径
     * @param fileName 文件名
     * @param localFile 本地文件
     * @param progress 进度监听器
     * @return 是否下载成功
     */
    private boolean downloadFile(String serverUrl, String remotePath, String fileName, File localFile, FileSyncProgress progress) {
        // 确保下载目录存在
        ensureDownloadDirExists();
        
        InputStream input = null;
        FileOutputStream output = null;
        HttpURLConnection connection = null;
        boolean success = false;
        int retryCount = 0;
        
        // 记录原始文件名，用于最后保存时使用
        String originalFileName = fileName;
        
        while (!success && retryCount < MAX_RETRY_COUNT) {
            try {
                // 对文件名进行URL编码
                String encodedFileName = URLEncoder.encode(fileName, "UTF-8")
                        .replace("+", "%20"); // 确保空格正确编码
                
                // 记录编码前后的文件名，便于调试
                Log.d(TAG, "文件名编码: 原始=" + fileName + ", 编码后=" + encodedFileName);
                
                URL url = new URL(serverUrl + remotePath + encodedFileName);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(DEFAULT_TIMEOUT);
                connection.setReadTimeout(DEFAULT_TIMEOUT);
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "下载文件 " + fileName + " 失败，响应码: " + responseCode);
                    retryCount++;
                    continue;
                }
                
                // 创建目录（如果不存在）
                if (!localFile.getParentFile().exists()) {
                    localFile.getParentFile().mkdirs();
                }
                
                // 下载文件
                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream(localFile);
                
                byte[] data = new byte[4096];
                long total = 0;
                int count;
                
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                    if (progress != null) {
                        progress.onProgress((int) (total * 100 / fileLength));
                    }
                }
                
                Log.d(TAG, "文件 " + originalFileName + " 下载完成，大小: " + total + " 字节");
                success = true;
                
            } catch (Exception e) {
                Log.e(TAG, "下载文件 " + fileName + " 时出错: " + e.getMessage(), e);
                retryCount++;
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭流时出错", e);
                }
                if (connection != null) connection.disconnect();
            }
        }
        
        return success;
    }
    
    /**
     * 解码URL编码的文件名
     * @param fileName URL编码的文件名
     * @return 解码后的文件名
     */
    private String decodeUrlFileName(String fileName) {
        try {
            // 如果是URL编码形式（包含%），就进行解码
            if (fileName.contains("%")) {
                return java.net.URLDecoder.decode(fileName, "UTF-8");
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "解码文件名失败: " + fileName, e);
            return fileName;
        }
    }
    
    /**
     * 下载内容
     */
    private String downloadContent(String url) throws IOException {
        HttpURLConnection connection = null;
        InputStream input = null;
        StringBuilder content = new StringBuilder();
        
        try {
            Log.d(TAG, "开始下载内容: " + url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取HTML响应
                input = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String line;
                
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();
                Log.d(TAG, "内容下载成功，长度: " + content.length());
            } else {
                Log.e(TAG, "下载内容失败，状态码: " + responseCode);
            }
        } finally {
            if (input != null) {
                input.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        
        return content.toString();
    }
    
    /**
     * 下载指定文件列表
     */
    private void downloadFileList(List<String> fileList, String serverUrl, String remotePath, File localDir, FileSyncProgress progress) {
        if (fileList.isEmpty()) {
            Log.d(TAG, "文件列表为空，无需下载");
            return;
        }
        
        Log.d(TAG, "开始下载文件列表，共 " + fileList.size() + " 个文件");
        int totalFiles = fileList.size();
        int downloadedFiles = 0;
        
        for (String fileName : fileList) {
            if (isCancelled) {
                Log.d(TAG, "同步已被取消，停止下载");
                break;
            }
            
            // 确保文件名不含有路径分隔符
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            }
            
            // 显示正在下载的文件信息
            Log.d(TAG, "准备下载文件: " + fileName);
            
            File localFile = new File(localDir, fileName);
            
            // 如果文件已存在且不需要重新下载，则跳过
            if (localFile.exists() && !forceDownload) {
                Log.d(TAG, "文件已存在，跳过: " + fileName);
                downloadedFiles++;
                if (progress != null) {
                    progress.onProgress(downloadedFiles * 100 / totalFiles);
                }
                continue;
            }
            
            // 下载文件
            boolean success = downloadFile(serverUrl, remotePath, fileName, localFile, progress);
            
            // 记录下载结果
            if (success) {
                downloadedFiles++;
                Log.d(TAG, "文件下载成功: " + fileName + " [" + downloadedFiles + "/" + totalFiles + "]");
                
                // 验证文件是否正确下载
                if (localFile.exists() && localFile.length() > 0) {
                    Log.d(TAG, "文件验证成功，大小: " + localFile.length() + " 字节");
                } else {
                    Log.e(TAG, "文件验证失败: " + (localFile.exists() ? "文件大小为0" : "文件不存在"));
                }
            } else {
                Log.e(TAG, "文件下载失败: " + fileName);
            }
            
            if (progress != null) {
                progress.onProgress(downloadedFiles * 100 / totalFiles);
            }
        }
        
        Log.d(TAG, "文件下载完成，成功下载 " + downloadedFiles + " / " + totalFiles + " 个文件");
    }
    
    public interface FileSyncProgress {
        void onProgress(int progress);
    }
}
