# 行程提醒 · Schedule Reminder

一款**纯 Java 原生**的 Android 日程 / 课表应用。没有 Gradle、没有第三方依赖、没有一行 Kotlin —— 直接用 `aapt2` + `javac` + `d8` + `apksigner` 手工构建，APK 约 **159 KB**。

| | |
|---|---|
| 版本 | 0.0.1（versionCode 1） |
| 包名 | `com.moshang.schedule` |
| 最低系统 | Android 7.0（API 24） |
| 目标系统 | Android 14（API 34） |
| 许可证 | GNU GPL-3.0 |

## 功能

### 五种视图

全部用 `Canvas` 自绘，没有用任何系统控件：

- **年**：12 个迷你月历（3 列 × 4 行），有日程的日期带标记，点击进入月视图
- **月**：周日起始的月历网格，每格带农历与节气小字、事件圆点；左右跟手翻月，松手吸附或回弹；下方是可上滑的日程面板
- **周**：时间轴，左侧小时刻度 + 7 天列，事件按起止时间定位，今日有一条红线，进入时自动滚到当前小时
- **日**：24 小时时间轴 + 事件块
- **日程**：跨日期的议程列表，按日期分组

### 日程

- 重复规则：不重复 / 每天 / 每周 / 每月；另支持「指定多个日期」（导入时生成，不在表单选项里）
- 重复可设截止日期
- 全天事件、结束时间、地点、备注
- 提醒：不提醒 / 准时 / 提前 5 分钟 / 15 分钟 / 30 分钟 / 1 小时
- 分类：新改名 / 换色 / 删除都支持，按住手柄可拖动排序

### 其他

- **农历与节气**：1900–2100 农历引擎（`Lunar.java`），含二十四节气与常见节日；月视图每格带农历小字，面板顶部有干支信息条
- **9 套主题色**：校园蓝 / 松柏绿 / 暮云蓝 / 青瓷 / 鼠尾草 / 雾紫 / 石墨 / 玫瑰 / 麦田，深色模式下自动提亮
- **深浅色**：跟随系统，两套完整色板
- **搜索**：跨标题、地点、备注检索
- **导入**：JSON 全量导入，或粘贴课表文本按正则解析（支持一行多条的粘连文本、多日期自动展开）
- **导出**：JSON 全量导出到文件
- **AI 添加日程**：接入任意 OpenAI 兼容接口，说人话自动解析成日程，先预览再写入
- **桌面小组件**：显示今日日期与当天前 4 条日程

### 提醒链路

- `AlarmManager.setExactAndAllowWhileIdle`（API 23+，低版本退化为 `setExact`），按事件 id 独立 PendingIntent
- `BootReceiver` 开机后重排全部闹钟
- 通知点击进入应用，无内联操作按钮（Android 11+ 的无声音横幅需要额外挂静音音频，本版未做）

## 月页上滑面板

月页只有一个可动层，靠两段位移完成「盖住月历」和「滚完日程」两件事：

```
SwipePanel（FrameLayout）
├── 月历 MonthView        固定占位，内部按行裁切
└── 面板 panel            高度 = 容器高，整体上移
    ├── head  农历条       钉在面板顶部
    └── body  日程卡片     第二段单独上移
```

| 段 | 范围 | 行为 |
|---|---|---|
| 第一段 | 0 ~ (月历高 − 一行高) | 面板整体上移，月历按比例逐行裁切 |
| 第二段 | 超出部分 | 面板顶钉住不动，只让 body 内容继续上移 |

月历收起量按进度等比映射：`calShift = 选中行顶部 × (面板位移 / 总行程)`。等比的用意是任何一行都走到行程尽头才恰好贴顶——如果按差值算，选中第一行时手指滑 84dp 就已经收敛到上限，之后整段滑动那一行都固定不动。

阻尼按手指累计位移的 √ 型曲线，与时间无关：

```
增益 = 0.06 + 0.87 × √(位移 / 110dp)     位置按位移积分
```

距端点 90dp 以内改用三次方衰减，剩余 60dp 时推 200 只走 58，剩余 30dp 时只走 7，最后 10dp 几乎不动。前半程轻快、末段像被吸住。

松手判定：上滑看面板位移是否过半，下滑看月历展开是否过半；速度超过 550 / 600 dp/s 时按方向直接翻。归位动画 150–280ms，缓出曲线。

实现上重写了 `dispatchTouchEvent` 而不是 `onInterceptTouchEvent`——子 View 一旦调用 `requestDisallowInterceptTouchEvent`，父容器就收不到 MOVE，阻尼会静默失效，而这个坑在真机上表现为「完全没有阻尼」，很难从代码上看出原因。

## 界面规则

| 项 | 取值 |
|---|---|
| 主色 | `#172E61`（只用于选中 / 今天 / 主按钮） |
| 画布 / 卡面 | `#F5F6F8` / `#FCFDFE`（深色 `#15191F` / `#202630`） |
| 圆角 | 卡片 13dp、弹层 16dp、FAB 20dp |
| 网格 | 4dp 基线：月格 56dp、视图栏 44dp、卡间距 8dp |
| 今日 vs 选中 | 今日是描边环、选中是实心块 —— 不靠颜色也能分辨 |
| 数字 | 全部等宽数字（`tnum`），翻页时时间不跳字 |
| 触控 | 图标 40dp、主按钮 48dp |
| 分类色板 | 低饱和 9 色，与删除红（`#C0453F`）、周末色分开 |

所有弹层（选项、确认、日期、时间滚轮、开关）都是自绘的，没有用 `AlertDialog` / `DatePickerDialog` / `Spinner` 这类系统控件。

## 构建

需要三样东西：`build-tools`（aapt2 / zipalign / apksigner）、`android.jar`、`r8.jar`。

```bash
# 1) 工具链（腾讯云镜像）
curl -O https://mirrors.cloud.tencent.com/AndroidSDK/build-tools_r34-linux.zip
curl -O https://mirrors.cloud.tencent.com/AndroidSDK/platform-35_r01.zip
curl -O https://mirrors.cloud.tencent.com/nexus/repository/maven-public/com/android/tools/r8/9.5.23/r8-9.5.23.jar

# 2) 环境变量
export ANDROID_TOOLS=/path/to/tools   # 内含 android-14/（build-tools）与 android-35/android.jar
export KS_PASS=<你的签名口令>

# 3) 构建
bash build.sh        # → build/app-signed.apk
```

流水线：`aapt2 compile → aapt2 link → javac --release 8 → d8 → zip → zipalign -f -p 4 → apksigner sign`。

两个必踩的坑：

1. **`javac` 必须加 `--release 8`**。用新版 JDK 生成的字节码，旧版 `d8` 处理不了实例方法里的匿名内部类，会抛 `NullPointerException` 然后静默失败。
2. **`apksigner` 在网络文件系统上会卡死**（报 `Failed to read chunk` 后挂起）。先 `cp` 到 `/dev/shm` 或 `/tmp` 再签，一秒完成。`build.sh` 已内置这一步。

> 签名密钥 `schedule.keystore` 不入库（已在 `.gitignore`）。口令通过环境变量 `KS_PASS` 传入，脚本里不保存任何凭据。密钥丢失会导致新包无法覆盖安装旧版本，务必单独备份。

## 目录结构

```
├── AndroidManifest.xml
├── build.sh                    # 一键构建
├── build_obf.sh                # R8 混淆构建
├── proguard-rules.pro
├── res/                        # 颜色 / 尺寸 / drawable / 动画 / 小组件布局
├── src/com/moshang/schedule/   # 39 个 Java 类
│   ├── MainActivity.java       # 主界面、视图切换、菜单
│   ├── SwipePanel.java         # 月页上滑面板：手势、阻尼、吸附
│   ├── MonthView / WeekTimelineView / DayView / YearView
│   ├── EventEditActivity / ImportActivity / SearchActivity
│   ├── CategoryActivity / CategoryOrderActivity / CategorySheet
│   ├── AiChatActivity / AiSettingsActivity / AiClient / AiParser / AiConfig
│   ├── Store.java              # SharedPreferences 持久化 + 重复规则 + 闹钟排程
│   ├── Event.java / Category.java
│   ├── Lunar.java              # 农历引擎（1900–2100）
│   ├── Theme.java              # 9 套主题色
│   ├── ReminderReceiver / BootReceiver / ScheduleWidget
│   └── Sheet / OptionSheet / ConfirmSheet / DateSheet / TimeSheet
│       / WheelView / ToggleView / SegmentBar / TitleBar / Press
└── skin/                       # 设计与构建辅助脚本（纯 Python）
    ├── ams_skin.py             # 设计令牌（色板 / 圆角 / 描边）
    ├── ams_skin_java.py        # 自绘组件外观
    ├── skill_rules.py          # 日历交互规范修订
    └── make_icon.py            # 启动图标生成
```

## AI 配置

主界面 `⋮` → **AI 添加日程** → 右上「设置」：

1. 选服务商：DeepSeek / 智谱 GLM / 通义千问 / Moonshot Kimi / OpenAI / 自定义，会自动填好接口地址与默认模型名
2. 填 API Key → 保存

请求走标准 `POST {base}/chat/completions`，`Authorization: Bearer <key>`。

## 数据与隐私

- 日程数据以 JSON 存在本机 SharedPreferences（`schedule_store`），不联网、不上传
- AI 功能的 API Key 只存在本机（`schedule_ai`），不入库、不上传、不写日志；聊天内容直接发送给你自己配置的服务商
- 权限只有 6 项：`INTERNET`（仅 AI 使用）、`POST_NOTIFICATIONS`、`SCHEDULE_EXACT_ALARM`、`RECEIVE_BOOT_COMPLETED`、`VIBRATE`、`WAKE_LOCK`；不申请存储、定位、通讯录、相机、麦克风
- 仓库已通过凭据扫描：无硬编码密钥、无邮箱、无服务器地址

## 版本

- **0.0.1** — 首个公开版本。纯 Java 原生重写；月页改为上滑面板交互；包名由 `com.moshang.trip` 迁移至 `com.moshang.schedule`（属于新应用，旧版数据需导出再导入）

## 许可证

[GNU General Public License v3.0](LICENSE) © 2026 BEAVER-awa · [github.com/BEAVER-awa](https://github.com/BEAVER-awa)

本程序是自由软件：你可以依据自由软件基金会发布的 GNU 通用公共许可证第 3 版（或你选择的任何更新版本）的条款重新发布和/或修改它。

本程序基于「有用」的希望分发，但不提供任何担保，甚至不提供适销性或特定用途适用性的默示担保。详见 [LICENSE](LICENSE)。

> 分发编译产物（APK）时，请同时提供对应版本的完整源代码，这是 GPL-3.0 的要求。
