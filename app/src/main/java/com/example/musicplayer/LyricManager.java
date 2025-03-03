package com.example.musicplayer;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricManager {
    private static final String TAG = "LyricManager";

    // 用于封装歌词信息的内部类，包含所有时间戳和对应的歌词文本
    public static class Lyric {
        public List<Long> times; // 每行对应的时间戳（单位：毫秒）
        public List<String> lyrics; // 每行歌词文本

        // 根据当前播放位置查找对应歌词的索引（逆序查找）
        public int findLyricIndex(long position) {
            // 空数组保护（重要！）
            if (times == null || times.isEmpty()) {
                return 0;
            }
            for (int i = times.size() - 1; i >= 0; i--) {
                if (position >= times.get(i)) {
                    return i;
                }
            }
            return 0;
        }
    }

    /**
     * 根据歌曲名称动态解析LRC文件，优先从本地下载的文件读取，如果没有再从assets中读取
     *
     * @param songName 歌曲文件名（包含 .flac 后缀）
     * @param context  上下文对象
     * @return 解析后的 Lyric 对象，如果解析失败会返回一个空的 Lyric 对象（内含空列表）
     */
    public static Lyric getLyric(String songName, Context context) {
        // 如果是unknown.mp3，直接返回空的Lyric对象，避免尝试加载不存在的歌词文件
        if (songName == null || songName.isEmpty() || "unknown.mp3".equals(songName)) {
            Log.d(TAG, "歌曲名称无效或未知，跳过歌词加载: " + songName);
            Lyric emptyLyric = new Lyric();
            emptyLyric.times = new ArrayList<>();
            emptyLyric.lyrics = new ArrayList<>();
            return emptyLyric;
        }
        
        // 构造 LRC 文件名，例如 "蒲公英的约定.flac" 对应 "蒲公英的约定.lrc"
        String fileName = songName.replace(".flac", "").trim() + ".lrc";
        
        // 创建空的Lyric对象
        Lyric lyric = new Lyric();
        lyric.times = new ArrayList<>();
        lyric.lyrics = new ArrayList<>();
        
        // 先尝试从本地下载的文件读取
        File localLyricsDir = FileSyncManager.getLocalLyricsDir(context);
        File localLyricFile = new File(localLyricsDir, fileName);
        
        if (localLyricFile.exists() && localLyricFile.isFile()) {
            Log.d(TAG, "从本地文件读取歌词: " + localLyricFile.getAbsolutePath());
            try (InputStream is = new FileInputStream(localLyricFile);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                parseLrcContent(br, lyric);
                return lyric;
            } catch (IOException e) {
                Log.e(TAG, "读取本地歌词文件失败: " + e.getMessage());
                // 如果读取本地文件失败，继续尝试从assets读取
            }
        }
        
        // 如果本地文件不存在或读取失败，从assets读取
        try {
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open("lyrics/" + fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            parseLrcContent(br, lyric);
            br.close();
            is.close();
            return lyric;
        } catch (IOException e) {
            Log.e(TAG, "读取assets歌词文件失败: " + e.getMessage());
            // 返回空的Lyric对象
            return lyric;
        }
    }
    
    /**
     * 解析LRC文件内容
     * @param br BufferedReader对象
     * @param lyric 要填充的Lyric对象
     * @throws IOException 读取异常
     */
    private static void parseLrcContent(BufferedReader br, Lyric lyric) throws IOException {
        String line;
        // 正则表达式：匹配 [mm:ss.xxx] 格式，支持一行中有多个时间标签
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d+)](.*)");
        
        while ((line = br.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String minStr = matcher.group(1);
                String secStr = matcher.group(2);
                String fracStr = matcher.group(3);
                String lyricText = matcher.group(4).trim();
                
                // 新增双语解析逻辑：检测 '\' 符号，并将其替换为换行符
                if (lyricText.contains("\\")) {
                    lyricText = lyricText.replace("\\", "\n");
                }
                
                int minutes = Integer.parseInt(minStr);
                int seconds = Integer.parseInt(secStr);
                
                // 将小数部分转换为毫秒
                double fraction = Double.parseDouble("0." + fracStr);
                long timeMs = minutes * 60000 + seconds * 1000 + (long) (fraction * 1000);
                
                lyric.times.add(timeMs);
                lyric.lyrics.add(lyricText);
            }
        }
    }

    /**
     * 通过解析 LRC 文件获取歌手信息，优先从本地文件读取
     * 默认读取歌词的第一行，格式要求为 "歌名 - 歌手"，如果存在括号则取括号前部分作为歌手名
     *
     * @param songName 歌曲文件名（包含 .flac 后缀）
     * @param context  上下文对象
     * @return 解析后的歌手名称，如果解析失败则返回 "未知歌手"
     */
    public static String getSinger(String songName, Context context) {
        // 处理unknown.mp3的情况
        if (songName == null || songName.isEmpty() || "unknown.mp3".equals(songName)) {
            return "未知歌手";
        }
        
        // songName 现在包含 .flac 后缀
        Lyric lyric = getLyric(songName, context);
        // 空数组保护（重要！）：检查 lyrics 列表是否为空
        if (lyric != null && lyric.lyrics != null && !lyric.lyrics.isEmpty()) {
            String firstLine = lyric.lyrics.get(0);
            if (firstLine.contains("-")) {
                String[] parts = firstLine.split("-");
                if (parts.length >= 2) {
                    String singerPart = parts[1].trim();
                    // 如果有括号则只保留括号前的部分，例如 "周杰伦 (Jay Chou)" 解析为 "周杰伦"
                    if (singerPart.contains("(")) {
                        singerPart = singerPart.substring(0, singerPart.indexOf("(")).trim();
                    }
                    return singerPart;
                }
            }
        }
        return "未知歌手";
    }
}
