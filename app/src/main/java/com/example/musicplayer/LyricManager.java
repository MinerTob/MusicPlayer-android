package com.example.musicplayer;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricManager {

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
     * 根据歌曲名称动态解析 assets/lyrics 下的 LRC 文件，并返回 Lyric 对象
     *
     * @param songName 歌曲文件名（包含 .mp3 后缀）
     * @param context  上下文对象（用于访问 assets 资源）
     * @return 解析后的 Lyric 对象，如果解析失败会返回一个空的 Lyric 对象（内含空列表）
     */
    public static Lyric getLyric(String songName, Context context) {
        // 构造 LRC 文件名，例如 "蒲公英的约定.mp3" 对应 "蒲公英的约定.lrc"
        String fileName = songName.replace(".mp3", "").trim() + ".lrc";
        AssetManager assetManager = context.getAssets();
        List<Long> times = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        try (InputStream is = assetManager.open("lyrics/" + fileName);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
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
                    times.add(timeMs);
                    texts.add(lyricText);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 如果读取或解析异常，则直接返回一个空的 Lyric 对象
        }
        // 空数组保护（重要！）：不论最终解析结果如何，都保证 Lyric 对象中的列表不为 null
        Lyric lyric = new Lyric();
        lyric.times = times;
        lyric.lyrics = texts;
        return lyric;
    }

    /**
     * 通过解析 LRC 文件获取歌手信息
     * 默认读取歌词的第一行，格式要求为 "歌名 - 歌手"，如果存在括号则取括号前部分作为歌手名
     *
     * @param songName 歌曲文件名（包含 .mp3 后缀）
     * @param context  上下文对象
     * @return 解析后的歌手名称，如果解析失败则返回 "未知歌手"
     */
    public static String getSinger(String songName, Context context) {
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
