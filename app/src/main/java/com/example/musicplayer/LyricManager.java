package com.example.musicplayer;

public class LyricManager {
    // 直接根据歌曲名称返回对应歌词类
    public static Class<?> getLyricClass(String songName) {
        String cleanName = songName.replace(".mp3", "")
                                  .replace(" ", "_")
                                  .replace("-", "_")
                                  .trim();
        
        switch(cleanName) {
            case "Counting_Stars":
                return Counting_Stars.class;
            case "蒲公英的约定":
                return Dandelion_Promise.class;
            default:
                return Counting_Stars.class;
        }
    }

    public static String getSinger(String songName) {
        Class<?> lyricClass = getLyricClass(songName);
        try {
            return (String) lyricClass.getField("SINGER").get(null);
        } catch (Exception e) {
            return "未知歌手";
        }
    }
}
