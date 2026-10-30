package com.moshang.schedule;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** AI 接口设置：任何 OpenAI 兼容服务都能接 */
public class AiSettingsActivity extends Activity {

    private AiConfig cfg;
    private float density;
    private TextView providerVal;
    private EditText baseField, keyField, modelField;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        cfg = AiConfig.load(this);
        density = getResources().getDisplayMetrics().density;
        setContentView(build());
    }

    private int dp(float v) { return Math.round(v * density); }

    private int c(int id) { return getResources().getColor(id); }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(c(R.color.window_bg));

        // 顶栏
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(16), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        TextView title = new TextView(this);
        title.setText("AI 设置");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(c(R.color.text_primary));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(6);
        title.setLayoutParams(tlp);

        bar.addView(back);
        bar.addView(title);
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(4), dp(18), dp(28));

        TextView tip = new TextView(this);
        tip.setText("填任何 OpenAI 兼容接口（/chat/completions）即可。密钥只存在本机，不会上传到别处。");
        tip.setTextSize(12);
        tip.setTextColor(c(R.color.text_secondary));
        tip.setLineSpacing(dp(3), 1f);
        tip.setPadding(0, 0, 0, dp(14));
        body.addView(tip);

        // 服务商
        body.addView(label("服务商"));
        LinearLayout pRow = row();
        providerVal = new TextView(this);
        providerVal.setText(cfg.provider);
        providerVal.setTextSize(14);
        providerVal.setTextColor(c(R.color.text_primary));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        providerVal.setLayoutParams(plp);
        ImageView care = new ImageView(this);
        care.setImageResource(R.drawable.ic_caret_down);
        care.setLayoutParams(new LinearLayout.LayoutParams(dp(14), dp(14)));
        pRow.addView(providerVal);
        pRow.addView(care);
        pRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickProvider(); }
        });
        Press.attach(pRow);
        body.addView(pRow);

        // Base URL
        body.addView(label("接口地址（Base URL）"));
        baseField = field(cfg.base, InputType.TYPE_TEXT_VARIATION_URI, "https://api.deepseek.com/v1");
        body.addView(baseField);

        // API Key
        body.addView(label("API Key"));
        keyField = field(cfg.key, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, "sk-...");
        body.addView(keyField);

        // 模型
        body.addView(label("模型名"));
        modelField = field(cfg.model, InputType.TYPE_CLASS_TEXT, "deepseek-chat");
        body.addView(modelField);

        TextView note = new TextView(this);
        note.setText("提示：如果服务商给的地址已经带了 /chat/completions，直接整条粘进来也行。");
        note.setTextSize(11);
        note.setTextColor(c(R.color.text_tertiary));
        note.setPadding(dp(2), dp(10), dp(2), dp(18));
        body.addView(note);

        TextView save = new TextView(this);
        save.setText("保存");
        save.setTextSize(15);
        save.setTypeface(Typeface.DEFAULT_BOLD);
        save.setGravity(Gravity.CENTER);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundResource(R.drawable.bg_btn_brand);
        save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.brand(this)));
        save.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doSave(); }
        });
        Press.attach(save);
        body.addView(save);

        sv.addView(body);
        root.addView(sv);
        return root;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(c(R.color.text_secondary));
        t.setPadding(dp(2), dp(10), dp(2), dp(6));
        return t;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackgroundResource(R.drawable.bg_field);
        r.setPadding(dp(14), dp(13), dp(14), dp(13));
        r.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return r;
    }

    private EditText field(String value, int inputType, String hint) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setHint(hint);
        e.setHintTextColor(c(R.color.text_tertiary));
        e.setTextColor(c(R.color.text_primary));
        e.setTextSize(14);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setBackgroundResource(R.drawable.bg_field);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private void pickProvider() {
        int cur = 0;
        for (int i = 0; i < AiConfig.PROVIDERS.length; i++) {
            if (AiConfig.PROVIDERS[i].equals(cfg.provider)) cur = i;
        }
        final String[] names = new String[AiConfig.PROVIDERS.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = i == AiConfig.PROVIDERS.length - 1 ? "自定义" : AiConfig.PROVIDERS[i];
        }
        new OptionSheet(this, "选择服务商", names, cur, new OptionSheet.OnPick() {
            public void onPick(int idx) {
                cfg.provider = AiConfig.PROVIDERS[idx];
                providerVal.setText(idx == AiConfig.PROVIDERS.length - 1 ? "自定义" : AiConfig.PROVIDERS[idx]);
                if (AiConfig.BASE[idx].length() > 0) {
                    baseField.setText(AiConfig.BASE[idx]);
                    modelField.setText(AiConfig.MODEL[idx]);
                }
            }
        }).showSheet();
    }

    private void doSave() {
        cfg.base = baseField.getText().toString().trim();
        cfg.key = keyField.getText().toString().trim();
        cfg.model = modelField.getText().toString().trim();
        if (cfg.base.length() == 0 || cfg.model.length() == 0) {
            Toast.makeText(this, "接口地址和模型名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        cfg.save(this);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}
