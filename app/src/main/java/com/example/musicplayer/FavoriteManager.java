package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class FavoriteManager {
    private static final String TAG = "FavoriteManager";
    private static final String PREF_NAME = "favorite_prefs";
    private static final String KEY_FAVORITES = "favorite_songs";
    
    private static SharedPreferences preferences;
    private static Set<String> favorites = new HashSet<>();
    private static boolean isInitialized = false;

    // 初始化收藏数据
    public static void initFavorites(Context context) {
        if (isInitialized) {
            Log.d(TAG, "收藏管理器已初始化");
            return;
        }
        
        if (context == null) {
            Log.e(TAG, "初始化收藏管理器失败：上下文为空");
            return;
        }
        
        try {
            preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            favorites = new HashSet<>(preferences.getStringSet(KEY_FAVORITES, new HashSet<>()));
            isInitialized = true;
            Log.d(TAG, "收藏管理器初始化成功，共加载 " + favorites.size() + " 首收藏歌曲");
        } catch (Exception e) {
            Log.e(TAG, "初始化收藏管理器时发生错误", e);
            // 确保favorites不为null
            favorites = new HashSet<>();
        }
    }

    // 添加收藏
    public static boolean addFavorite(Context context, String songName) {
        if (songName == null || songName.trim().isEmpty()) {
            Log.e(TAG, "无法添加收藏：歌曲名为空");
            return false;
        }
        
        // 确保初始化
        if (!isInitialized) {
            initFavorites(context);
        }
        
        // 检查是否已经是收藏
        if (favorites.contains(songName)) {
            Log.d(TAG, "歌曲已在收藏列表中: " + songName);
            return true;
        }
        
        try {
            // 添加到内存集合
            favorites.add(songName);
            
            // 保存到SharedPreferences
            if (preferences != null) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(KEY_FAVORITES, favorites);
                editor.apply();
                Log.d(TAG, "添加收藏成功: " + songName);
                return true;
            } else {
                Log.e(TAG, "添加收藏失败：SharedPreferences未初始化");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "添加收藏时发生错误", e);
            return false;
        }
    }

    // 移除收藏
    public static boolean removeFavorite(Context context, String songName) {
        if (songName == null || songName.trim().isEmpty()) {
            Log.e(TAG, "无法移除收藏：歌曲名为空");
            return false;
        }
        
        // 确保初始化
        if (!isInitialized) {
            initFavorites(context);
        }
        
        // 检查是否在收藏列表中
        if (!favorites.contains(songName)) {
            Log.d(TAG, "歌曲不在收藏列表中: " + songName);
            return true;
        }
        
        try {
            // 从内存集合中移除
            favorites.remove(songName);
            
            // 保存到SharedPreferences
            if (preferences != null) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(KEY_FAVORITES, favorites);
                editor.apply();
                Log.d(TAG, "移除收藏成功: " + songName);
                return true;
            } else {
                Log.e(TAG, "移除收藏失败：SharedPreferences未初始化");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "移除收藏时发生错误", e);
            return false;
        }
    }

    // 检查歌曲是否收藏
    public static boolean isFavorite(String songName) {
        if (songName == null || songName.trim().isEmpty()) {
            return false;
        }
        return favorites.contains(songName);
    }

    // 获取所有收藏歌曲
    public static Set<String> getFavorites() {
        return new HashSet<>(favorites);
    }
    
    // 获取收藏数量
    public static int getFavoritesCount() {
        return favorites.size();
    }
    
    // 清空所有收藏
    public static void clearFavorites(Context context) {
        try {
            favorites.clear();
            
            if (preferences != null) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(KEY_FAVORITES, favorites);
                editor.apply();
                Log.d(TAG, "清空所有收藏成功");
            } else if (context != null) {
                preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(KEY_FAVORITES, favorites);
                editor.apply();
                Log.d(TAG, "清空所有收藏成功");
            } else {
                Log.e(TAG, "清空收藏失败：无法访问SharedPreferences");
            }
        } catch (Exception e) {
            Log.e(TAG, "清空收藏时发生错误", e);
        }
    }
}