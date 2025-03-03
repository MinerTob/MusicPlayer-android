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
import java.util.List;
import java.util.Map;

/**
 * 缓存管理器，处理缓存文件的读取和清理
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    
    // 获取已缓存的文件列表
    public static List<File> getCachedFiles(Context context) {
        List<File> result = new ArrayList<>();
        
        // 获取缓存目录
        File musicCacheDir = new File(context.getFilesDir(), "music");
        File lyricsCacheDir = new File(context.getFilesDir(), "lyrics");
        
        // 添加音乐文件
        addFilesFromDirectory(musicCacheDir, result);
        
        // 添加歌词文件
        addFilesFromDirectory(lyricsCacheDir, result);
        
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
            if (file.delete()) {
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
            
            // 设置文件名
            holder.fileName.setText(currentFile.getName());
            
            // 设置文件大小
            holder.fileSize.setText(formatFileSize(currentFile.length()));
            
            // 设置文件图标
            if (isAudioFile(currentFile.getName())) {
                holder.fileIcon.setText("🎵");
            } else if (isLyricFile(currentFile.getName())) {
                holder.fileIcon.setText("📝");
            } else {
                holder.fileIcon.setText("📄");
            }
            
            // 设置复选框状态，不触发监听器
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedFiles.get(currentFile.getAbsolutePath()));
            
            // 设置复选框监听器
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
