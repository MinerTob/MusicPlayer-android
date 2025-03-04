package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存管理器，处理缓存文件的读取和清理
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    
    // 获取已缓存的文件列表
    public static List<File> getCachedFiles(Context context) {
        List<File> result = new ArrayList<>();
        Set<String> processedNames = new HashSet<>();
        
        // 获取缓存目录
        File musicCacheDir = new File(context.getFilesDir(), "music");
        
        // 只添加音乐文件
        if (musicCacheDir.exists() && musicCacheDir.isDirectory()) {
            File[] files = musicCacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isAudioFile(file.getName())) {
                        String baseName = getBaseName(file.getName());
                        if (!processedNames.contains(baseName)) {
                            result.add(file);
                            processedNames.add(baseName);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    // 获取已缓存的歌曲组（每组包含歌曲文件和对应的歌词文件）
    public static List<SongFileGroup> getCachedSongGroups(Context context) {
        List<SongFileGroup> result = new ArrayList<>();
        Set<String> processedNames = new HashSet<>();
        
        // 获取音乐和歌词缓存目录
        File musicCacheDir = new File(context.getFilesDir(), "music");
        File lyricsCacheDir = new File(context.getFilesDir(), "lyrics");
        
        if (!musicCacheDir.exists() || !musicCacheDir.isDirectory()) {
            return result;
        }
        
        // 获取所有文件
        File[] files = musicCacheDir.listFiles();
        if (files == null) {
            return result;
        }
        
        // 先处理所有的音乐文件
        for (File file : files) {
            String fileName = file.getName();
            if (isAudioFile(fileName)) {
                // 获取不带后缀的文件名
                String baseName = getBaseName(fileName);
                
                if (!processedNames.contains(baseName)) {
                    // 查找对应的歌词文件（在lyrics目录下）
                    File lyricFile = new File(lyricsCacheDir, baseName + ".lrc");
                    
                    // 创建歌曲组
                    SongFileGroup group = new SongFileGroup(
                            formatDisplayName(baseName),
                            file,
                            lyricFile.exists() ? lyricFile : null
                    );
                    
                    result.add(group);
                    processedNames.add(baseName);
                }
            }
        }
        
        return result;
    }
    
    // 从目录中添加文件到列表
    private static void addFilesFromDirectory(File directory, List<File> fileList) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileList.add(file);
                    }
                }
            }
        }
    }
    
    // 计算文件总大小
    public static long calculateTotalSize(List<File> files) {
        long total = 0;
        for (File file : files) {
            total += file.length();
        }
        return total;
    }
    
    // 删除文件
    public static int deleteFiles(List<File> files) {
        int successCount = 0;
        for (File file : files) {
            // 删除音乐文件
            if (file.delete()) {
                successCount++;
            }
            
            // 如果是音频文件，尝试删除对应的歌词文件
            if (isAudioFile(file.getName())) {
                String baseName = getBaseName(file.getName());
                File lyricFile = new File(new File(file.getParentFile().getParentFile(), "lyrics"), baseName + ".lrc");
                if (lyricFile.exists() && lyricFile.delete()) {
                    successCount++;
                }
            }
        }
        return successCount;
    }
    
    // 删除歌曲组（包括歌曲文件和歌词文件）
    public static int deleteSongGroups(List<SongFileGroup> groups) {
        int successCount = 0;
        
        for (SongFileGroup group : groups) {
            // 删除音乐文件
            if (group.getMusicFile() != null && group.getMusicFile().delete()) {
                successCount++;
            }
            
            // 删除歌词文件
            if (group.getLyricFile() != null && group.getLyricFile().delete()) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    // 格式化文件大小
    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    // 获取文件基础名（不含扩展名）
    public static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
    
    // 格式化显示名称（去掉下划线）
    public static String formatDisplayName(String baseName) {
        return baseName.replace("_", " ");
    }
    
    // 判断是否为音频文件
    public static boolean isAudioFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".flac") || 
               lowerCaseName.endsWith(".mp3") || 
               lowerCaseName.endsWith(".wav") || 
               lowerCaseName.endsWith(".ogg") || 
               lowerCaseName.endsWith(".m4a");
    }
    
    // 判断是否为歌词文件
    public static boolean isLyricFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".lrc");
    }
    
    /**
     * 歌曲文件组，包含一个歌曲文件和对应的歌词文件
     */
    public static class SongFileGroup {
        private final String displayName;
        private final File musicFile;
        private final File lyricFile;
        
        public SongFileGroup(String displayName, File musicFile, File lyricFile) {
            this.displayName = displayName;
            this.musicFile = musicFile;
            this.lyricFile = lyricFile;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public File getMusicFile() {
            return musicFile;
        }
        
        public File getLyricFile() {
            return lyricFile;
        }
        
        public long getTotalSize() {
            long size = 0;
            if (musicFile != null) {
                size += musicFile.length();
            }
            if (lyricFile != null) {
                size += lyricFile.length();
            }
            return size;
        }
    }
    
    /**
     * 缓存文件组适配器，用于在列表中显示歌曲组
     */
    public static class SongGroupAdapter extends BaseAdapter {
        private final Context context;
        private final List<SongFileGroup> songGroups;
        private final Map<String, Boolean> selectedGroups;
        
        public SongGroupAdapter(Context context, List<SongFileGroup> songGroups) {
            this.context = context;
            this.songGroups = songGroups;
            this.selectedGroups = new HashMap<>();
            
            // 初始化所有组为未选中状态
            for (SongFileGroup group : songGroups) {
                selectedGroups.put(group.getDisplayName(), false);
            }
        }
        
        @Override
        public int getCount() {
            return songGroups.size();
        }
        
        @Override
        public Object getItem(int position) {
            return songGroups.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_cached_file, parent, false);
                
                holder = new ViewHolder();
                holder.checkBox = convertView.findViewById(R.id.checkBoxFile);
                holder.fileIcon = convertView.findViewById(R.id.textFileIcon);
                holder.fileName = convertView.findViewById(R.id.textFileName);
                holder.fileSize = convertView.findViewById(R.id.textFileSize);
                
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            SongFileGroup group = songGroups.get(position);
            
            // 设置显示名称
            holder.fileName.setText(group.getDisplayName());
            
            // 设置文件大小
            String sizeInfo = formatFileSize(group.getTotalSize());
            if (group.getLyricFile() != null) {
                sizeInfo += " (音乐+歌词)";
            } else {
                sizeInfo += " (仅音乐)";
            }
            holder.fileSize.setText(sizeInfo);
            
            // 设置文件图标
            holder.fileIcon.setText("🎵");
            
            // 设置复选框状态，不触发监听器
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedGroups.get(group.getDisplayName()));
            
            // 设置复选框监听器
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedGroups.put(group.getDisplayName(), isChecked);
            });
            
            // 整个条目点击事件
            convertView.setOnClickListener(v -> {
                boolean newState = !holder.checkBox.isChecked();
                holder.checkBox.setChecked(newState);
                selectedGroups.put(group.getDisplayName(), newState);
            });
            
            return convertView;
        }
        
        // 获取所有选中的歌曲组
        public List<SongFileGroup> getSelectedGroups() {
            List<SongFileGroup> selected = new ArrayList<>();
            for (SongFileGroup group : songGroups) {
                if (Boolean.TRUE.equals(selectedGroups.get(group.getDisplayName()))) {
                    selected.add(group);
                }
            }
            return selected;
        }
        
        // 全选
        public void selectAll() {
            for (SongFileGroup group : songGroups) {
                selectedGroups.put(group.getDisplayName(), true);
            }
            notifyDataSetChanged();
        }
        
        // 取消全选
        public void deselectAll() {
            for (SongFileGroup group : songGroups) {
                selectedGroups.put(group.getDisplayName(), false);
            }
            notifyDataSetChanged();
        }
        
        // ViewHolder模式
        private static class ViewHolder {
            CheckBox checkBox;
            TextView fileIcon;
            TextView fileName;
            TextView fileSize;
        }
    }
    
    /**
     * 缓存文件适配器，用于在列表中显示文件项
     */
    public static class CachedFileAdapter extends BaseAdapter {
        private final Context context;
        private final List<File> files;
        private final Map<String, Boolean> selectedFiles;
        
        public CachedFileAdapter(Context context, List<File> files) {
            this.context = context;
            this.files = files;
            this.selectedFiles = new HashMap<>();
            
            // 初始化所有文件为未选中状态
            for (File file : files) {
                selectedFiles.put(file.getAbsolutePath(), false);
            }
        }
        
        @Override
        public int getCount() {
            return files.size();
        }
        
        @Override
        public Object getItem(int position) {
            return files.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_cached_file, parent, false);
                
                holder = new ViewHolder();
                holder.checkBox = convertView.findViewById(R.id.checkBoxFile);
                holder.fileIcon = convertView.findViewById(R.id.textFileIcon);
                holder.fileName = convertView.findViewById(R.id.textFileName);
                holder.fileSize = convertView.findViewById(R.id.textFileSize);
                
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            File currentFile = files.get(position);
            
            // 设置处理后的显示名称
            String displayName = formatDisplayName(getBaseName(currentFile.getName()));
            holder.fileName.setText(displayName);
            
            // 计算文件组的总大小（包括歌词文件）
            long totalSize = currentFile.length();
            // 修正：歌词文件应该在lyrics目录下
            File lyricFile = new File(new File(currentFile.getParentFile().getParentFile(), "lyrics"), 
                    getBaseName(currentFile.getName()) + ".lrc");
            
            if (lyricFile.exists()) {
                totalSize += lyricFile.length();
            }
            
            // 设置文件大小信息
            String sizeInfo = formatFileSize(totalSize);
            if (lyricFile.exists()) {
                sizeInfo += " (音乐+歌词)";
            } else {
                sizeInfo += " (仅音乐)";
            }
            holder.fileSize.setText(sizeInfo);
            
            // 设置文件图标
            holder.fileIcon.setText("🎵");
            
            // 设置复选框状态和监听器
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedFiles.get(currentFile.getAbsolutePath()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedFiles.put(currentFile.getAbsolutePath(), isChecked);
            });
            
            // 整个条目点击事件
            convertView.setOnClickListener(v -> {
                boolean newState = !holder.checkBox.isChecked();
                holder.checkBox.setChecked(newState);
                selectedFiles.put(currentFile.getAbsolutePath(), newState);
            });
            
            return convertView;
        }
        
        // 判断是否为音频文件
        private boolean isAudioFile(String fileName) {
            String lowerCaseName = fileName.toLowerCase();
            for (String ext : FileSyncManager.MUSIC_EXTENSIONS) {
                if (lowerCaseName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
        
        // 判断是否为歌词文件
        private boolean isLyricFile(String fileName) {
            String lowerCaseName = fileName.toLowerCase();
            for (String ext : FileSyncManager.LYRIC_EXTENSIONS) {
                if (lowerCaseName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
        
        // 获取所有选中的文件
        public List<File> getSelectedFiles() {
            List<File> selected = new ArrayList<>();
            for (File file : files) {
                if (Boolean.TRUE.equals(selectedFiles.get(file.getAbsolutePath()))) {
                    selected.add(file);
                }
            }
            return selected;
        }
        
        // 全选
        public void selectAll() {
            for (File file : files) {
                selectedFiles.put(file.getAbsolutePath(), true);
            }
            notifyDataSetChanged();
        }
        
        // 取消全选
        public void deselectAll() {
            for (File file : files) {
                selectedFiles.put(file.getAbsolutePath(), false);
            }
            notifyDataSetChanged();
        }
        
        // ViewHolder模式
        private static class ViewHolder {
            CheckBox checkBox;
            TextView fileIcon;
            TextView fileName;
            TextView fileSize;
        }
    }
}
