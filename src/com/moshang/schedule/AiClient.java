package com.moshang.schedule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/** 最小 OpenAI 兼容客户端（纯 HttpURLConnection，无第三方库） */
public class AiClient {

    public static class Msg {
        public final String role;
        public final String content;

        public Msg(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /** 同步调用，必须在子线程执行 */
    public static String chat(AiConfig cfg, String system, List<Msg> history) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", cfg.model);
        body.put("temperature", 0.2);
        body.put("stream", false);

        JSONArray msgs = new JSONArray();
        if (system != null && system.length() > 0) {
            msgs.put(new JSONObject().put("role", "system").put("content", system));
        }
        for (Msg m : history) {
            msgs.put(new JSONObject().put("role", m.role).put("content", m.content));
        }
        body.put("messages", msgs);

        byte[] payload = body.toString().getBytes("UTF-8");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(cfg.endpoint()).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(90000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + cfg.key.trim());

            OutputStream os = conn.getOutputStream();
            os.write(payload);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String text = readAll(is);
            if (code >= 400) {
                throw new Exception("HTTP " + code + "：" + brief(text));
            }
            JSONObject o = new JSONObject(text);
            JSONArray choices = o.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                throw new Exception("返回里没有 choices：" + brief(text));
            }
            JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
            if (msg == null) throw new Exception("返回结构不认识：" + brief(text));
            String content = msg.optString("content", "");
            if (content.length() == 0) {
                // 个别推理模型把结果放在 reasoning_content
                content = msg.optString("reasoning_content", "");
            }
            if (content.length() == 0) throw new Exception("模型返回了空内容");
            return content;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

    private static String brief(String s) {
        if (s == null) return "";
        s = s.replace("\n", " ").trim();
        return s.length() > 220 ? s.substring(0, 220) + "…" : s;
    }
}
