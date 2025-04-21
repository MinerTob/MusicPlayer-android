package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ThemeAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private String[] mThemes;

    public ThemeAdapter(@NonNull Context context, @NonNull String[] themes) {
        super(context, R.layout.item_theme, themes);
        this.mContext = context;
        this.mThemes = themes;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_theme, parent, false);
        }
        // 主题预览图
        ImageView ivPreview = convertView.findViewById(R.id.iv_theme_preview);
        TextView tvName = convertView.findViewById(R.id.tv_theme_name);
        String themeName = getItem(position);
        if (themeName != null) {
            tvName.setText(themeName);
        }
        // 设置预览图或隐藏
        if (themeName != null && !themeName.isEmpty()) {
            ivPreview.setVisibility(View.VISIBLE);
            int resId = getThemePreviewResId(themeName);
            if (resId != 0) ivPreview.setImageResource(resId);
        } else {
            ivPreview.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

    /**
     * 根据主题名动态获取主题预览图资源ID
     * @param themeName 主题名称
     * @return 对应 drawable 资源ID，找不到时返回0
     */
    private int getThemePreviewResId(String themeName) {
        String resName;
        if ("Default".equalsIgnoreCase(themeName)) {
            resName = "default_theme";
        } else if ("".equals(themeName)) {
            // 预留主题，可扩展
            resName = "";
        } else {
            resName = themeName.toLowerCase() + "_theme";
        }
        if (resName.isEmpty()) return 0;
        return mContext.getResources().getIdentifier(resName, "drawable", mContext.getPackageName());
    }
}
