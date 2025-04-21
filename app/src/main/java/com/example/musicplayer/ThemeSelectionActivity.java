package com.example.musicplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ThemeSelectionActivity extends AppCompatActivity {

    private GridView gridView;
    private ThemeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_selection);

        // 初始化返回按钮
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackButtonClick(v));

        gridView = findViewById(R.id.grid_themes);
        // 主题列表，支持 Default 和 Aurora
        String[] themes = new String[]{"Default", "Aurora"};
        adapter = new ThemeAdapter(this, themes);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedTheme = adapter.getItem(position);
                // 切换壁纸逻辑
                if ("Aurora".equalsIgnoreCase(selectedTheme)) {
                    setBackgroundImage("aurora");
                } else if ("Default".equalsIgnoreCase(selectedTheme)) {
                    setBackgroundImage("sky");
                }
                // 显示切换成功并返回主界面
                Toast.makeText(ThemeSelectionActivity.this, "主题切换成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private String[] getThemeList() {
        // TODO: 返回主题列表标识，如主题名或颜色值等
        return new String[]{"Light", "Dark", "Blue", "Green", "Red", "Yellow", "Purple", "Cyan", "Orange"};
    }

    // 返回按钮点击处理
    public void onBackButtonClick(View view) {
        finish();
    }

    // 设置主界面壁纸的方法
    private void setBackgroundImage(String imageName) {
        // 假设主界面背景是 MainActivity.layout
        // 这里通过广播或直接调用 MainActivity 的静态方法实现
        // 简单实现：用 SharedPreferences 保存主题名，MainActivity onResume 时读取并切换背景
        getSharedPreferences("app_theme", MODE_PRIVATE)
                .edit()
                .putString("theme_bg", imageName)
                .apply();
    }
}
