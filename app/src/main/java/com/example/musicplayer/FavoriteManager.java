package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoriteManager {
    // 使用 HashSet 保存已收藏的歌曲文件名（mp3格式）
    private static Set<String> favoriteSet = new HashSet<>();

    // 初始化时加载 SharedPreferences 中的收藏数据
    public static void initFavorites(Context context) {
        SharedPreferences sp = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE);
        Set<String> savedFavorites = sp.getStringSet("favorites", new HashSet<>());
        favoriteSet = new HashSet<>(savedFavorites);
    }

    // 保存收藏数据到 SharedPreferences
    private static void saveFavorites(Context context) {
        SharedPreferences sp = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE);
        sp.edit().putStringSet("favorites", favoriteSet).apply();
    }

    // 判断当前歌曲是否已收藏
    public static boolean isFavorite(String songName) {
        return favoriteSet.contains(songName);
    }

    // 收藏歌曲（同时更新持久化数据）
    public static void addFavorite(String songName, Context context) {
        favoriteSet.add(songName);
        saveFavorites(context);
    }

    // 取消收藏（同时更新持久化数据）
    public static void removeFavorite(String songName, Context context) {
        favoriteSet.remove(songName);
        saveFavorites(context);
    }

    // 获取所有收藏的歌曲（如有需要）
    public static Set<String> getFavorites() {
        return favoriteSet;
    }
}