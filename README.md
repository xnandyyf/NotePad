# NotePad

一个用 Android 原生组件写的本地记事本。数据存在 SQLite，界面用 RecyclerView + Material 3，不依赖后端服务。

这个工程在课程示例 NotePad 的基础上做了功能扩展，对应《移动软件开发》期中实验的要求：列表条目显示时间戳、支持按标题或内容检索，并补充了分类、排序、置顶、导出、深色主题和偏好设置。

## 功能

**笔记管理**

- 列表显示标题、正文摘要、分类和更新时间（`yyyy-MM-dd HH:mm`）
- 右下角按钮新建笔记，点击条目进入编辑
- 编辑页可改标题、正文和分类，右上角保存；未保存直接返回会提示
- 长按条目弹出操作：置顶 / 取消置顶、分享、删除

**查询**

- 顶部搜索框按标题或正文模糊匹配，输入即筛选，清空后恢复全部
- 分类筛选与搜索可叠加使用（例如"工作"分类里搜"会议"）

**扩展功能**

| 功能 | 说明 |
| --- | --- |
| 笔记分类 | 内置工作、生活、学习、灵感四个分类，可新增和删除；删除分类时该分类下的笔记变为未分类 |
| 笔记排序 | 按修改时间、创建时间、标题三种方式，列表菜单和设置页都能切换 |
| 置顶 | 置顶的笔记在所有排序方式下都排在最前，列表里带"置顶"标记 |
| 导出 | 把全部笔记导出为 txt，走系统文件选择器，不需要存储权限 |
| 深色主题 | 设置里切换，立即生效并在下次启动时保持 |
| 偏好设置 | 深色模式、正文字号（12–24sp）、默认排序 |

## 技术实现

### 数据层

`NoteDbHelper` 继承 `SQLiteOpenHelper`，数据库 `notepad.db`，当前版本 2。

```sql
CREATE TABLE notes (
    _id         INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT NOT NULL DEFAULT '',
    content     TEXT NOT NULL DEFAULT '',
    category_id INTEGER,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL,
    pinned      INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES categories(_id) ON DELETE SET NULL
);

CREATE TABLE categories (
    _id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL UNIQUE,
    color INTEGER NOT NULL
);
```

列表查询在一条 SQL 里完成搜索、分类筛选与排序，`notes` 左连接 `categories` 取得分类名和颜色：

- 搜索：`title LIKE '%关键字%' OR content LIKE '%关键字%'`
- 分类：`category_id = ?`，与搜索条件用 `AND` 组合
- 排序：`pinned DESC` 恒定在前，其后按 `updated_at`、`created_at` 或 `title COLLATE NOCASE`

`categories` 表在首次建库时写入四个默认分类，颜色取自 `colors.xml`。分类数量用子查询统计，不额外建索引。

时间戳用 `SimpleDateFormat("yyyy-MM-dd HH:mm")` 统一格式化，列表、编辑页和导出文件共用同一格式。

### 界面

| 界面 | 类 | 职责 |
| --- | --- | --- |
| 笔记列表 | `NoteListActivity` | 时间戳展示、搜索、分类筛选、排序、置顶、导出全部 |
| 编辑页 | `NoteEditorActivity` | 标题与正文编辑、分类选择、字数统计、保存、分享、删除 |
| 分类管理 | `CategoryActivity` | 分类增删与各分类笔记数 |
| 设置 | `SettingsActivity` | 深色模式、正文字号、默认排序 |

- 列表用 `RecyclerView` + `NoteAdapter`，开启 `setHasStableIds` 并用 `bindingAdapterPosition` 取位置，插入或删除后点击对象不会错位。
- 编辑页用 `OnBackPressedCallback` 拦截返回，通过比较当前内容与打开时的原值判断是否需要提示"尚未保存"。
- 深色模式基于 `AppCompatDelegate.setDefaultNightMode` 与 `values-night/themes.xml`，偏好值存在 `SharedPreferences`，启动时由 `NoteApp` 应用。
- 字号偏好直接作用于编辑页正文，设置页提供实时预览。
- 导出用 `ActivityResultContracts.CreateDocument` 拿到 URI 后写入，没有任何存储权限声明。

## 目录结构

```
NotePad/
├── app/src/main/java/com/example/notepad/
│   ├── NoteApp.kt                 启动时应用主题
│   ├── data/
│   │   ├── Note.kt                笔记、分类与排序枚举
│   │   ├── NoteDbHelper.kt        建表、查询、增删改
│   │   └── Prefs.kt               偏好设置读写
│   ├── ui/
│   │   ├── NoteListActivity.kt    列表、搜索、筛选、排序、导出
│   │   ├── NoteAdapter.kt         列表适配器
│   │   ├── NoteEditorActivity.kt  编辑页
│   │   ├── CategoryActivity.kt    分类管理
│   │   ├── CategoryAdapter.kt     分类适配器
│   │   └── SettingsActivity.kt    设置
│   └── util/
│       ├── TimeFormat.kt          时间戳格式
│       └── Insets.kt              系统栏内边距
├── app/src/main/res/
│   ├── layout/                    四个界面与两个列表项布局
│   ├── menu/                      列表与编辑页菜单
│   ├── drawable/                  矢量图标与形状
│   └── values/、values-night/     文案、配色、主题（含深色）
├── gradle/                        版本目录与 Gradle Wrapper
└── README.md
```

## 构建与运行

环境：

| 项目 | 要求 |
| --- | --- |
| Android Studio | 2026.1 或更高（能用 AGP 9.3 即可） |
| JDK | 17 及以上（Android Studio 自带 JBR 可直接用） |
| Android SDK | compileSdk 37，需安装 Platform 37 与 Build-Tools |
| Gradle | 9.5（工程内 Wrapper 已指定） |
| minSdk | 24 |

Android Studio 打开工程，等待同步后运行 `app`。命令行：

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

依赖仓库已配置为国内镜像，顺序是：

1. 腾讯云 `mirrors.cloud.tencent.com/nexus/repository/maven-public/`（同时代理 Maven Central 与 Google Maven）
2. 阿里云 `maven.aliyun.com/repository/google`、`maven.aliyun.com/repository/public`
3. 官方 `google()`、`mavenCentral()`，镜像缺包时使用

Gradle 发行包地址同样指向 `mirrors.cloud.tencent.com/gradle/`，因此首次同步和首次构建都不需要代理。若某个依赖在镜像里报 404，注释掉前两行的镜像、保留官方仓库再同步一次即可。

`local.properties` 里的 `sdk.dir` 是本机 SDK 路径，已被 `.gitignore` 排除，换机器时 Android Studio 会重新生成。

## 数据说明

- 笔记只保存在本机 `notepad.db`，没有联网与云同步，卸载应用数据一并清除。
- 未实现 `ContentProvider`，因为应用不向其他应用共享数据。
- 导出的 txt 由系统文件选择器决定位置，应用不申请存储权限。

## 已知限制

- SQLite 读写发生在主线程。笔记数量在几百条量级时无明显影响，要支持大量数据应改用 Room 并把查询放到协程里。
- 只支持纯文本笔记，没有图片、语音和提醒。
- 分类颜色由名称哈希决定，只在新建分类时确定，不能手动改色。

## 推送到 GitHub

仓库已经初始化并提交过一次，本地分支为 `main`。在 GitHub 上新建一个空仓库（不要勾选自动创建 README），然后：

```bash
git remote add origin https://github.com/<用户名>/NotePad.git
git push -u origin main
```

后续修改按常规流程提交：

```bash
git add .
git commit -m "说明这次改了什么"
git push
```
