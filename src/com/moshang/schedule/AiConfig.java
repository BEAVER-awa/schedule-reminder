package com.moshang.schedule;

import android.content.Context;
import android.content.SharedPreferences;

/** AI 接口配置：任何 OpenAI 兼容的 chat/completions 服务都能接 */
public class AiConfig {

    public static final String[] PROVIDERS = {
            "DeepSeek", "智谱 GLM", "通义千问", "Moonshot Kimi", "OpenAI", "自定义"
    };

    public static final String[] BASE = {
            "https://api.deepseek.com/v1",
            "https://open.bigmodel.cn/api/paas/v4",
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "https://api.moonshot.cn/v1",
            "https://api.openai.com/v1",
            ""
    };

    public static final String[] MODEL = {
            "deepseek-chat",
            "glm-4-flash",
            "qwen-plus",
            "moonshot-v1-8k",
            "gpt-4o-mini",
            ""
    };

    public String provider = PROVIDERS[0];
    public String base = BASE[0];
    public String key = "";
    public String model = MODEL[0];

    private static final String PREF = "schedule_ai";

    public static AiConfig load(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        AiConfig cfg = new AiConfig();
        cfg.provider = sp.getString("provider", cfg.provider);
        cfg.base = sp.getString("base", cfg.base);
        cfg.key = sp.getString("key", "");
        cfg.model = sp.getString("model", cfg.model);
        return cfg;
    }

    public void save(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("provider", provider)
                .putString("base", base)
                .putString("key", key)
                .putString("model", model)
                .apply();
    }

    public boolean ready() {
        return key != null && key.trim().length() > 0
                && base != null && base.trim().length() > 0
                && model != null && model.trim().length() > 0;
    }

    /** 兼容用户直接粘贴到 /chat/completions 的完整地址 */
    public String endpoint() {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith("/chat/completions")) return b;
        return b + "/chat/completions";
    }
}
