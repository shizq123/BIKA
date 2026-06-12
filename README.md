<p align="center">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/app/src/main/res/mipmap-xhdpi/ic_launcher.png" width="100" height="100" alt="BIKA Icon">
</p>

<h1 align="center">BIKA</h1>

<p align="center">
  <a href="https://github.com/shizq123/BIKA/releases"><img src="https://img.shields.io/badge/Android-7.0%2B-brightgreen.svg" alt="Android Support"></a>
  <a href="https://github.com/shizq123/BIKA/releases"><img src="https://img.shields.io/github/release/shizq123/BIKA.svg" alt="GitHub Release"></a>
</p>

---

## 📖 项目简介

**BIKA** 是一款采用全新现代 Android 技术栈重构的第三方哔咔漫画 (Pica Comic) 移动端客户端。本客户端基于 **Jetpack Compose** 与 **Material Design 3 (Material You)** 规范构建，提供极其流畅、美观的漫画浏览与阅读体验。

> [!NOTE]
> 本项目接口基于抓包毕咔官方接口实现，旨在进行 Android Jetpack Compose 现代化重构与技术探索，代码与架构正在持续优化中。

---

## 🚀 核心亮点

- **✨ 现代 UI 体验**：全界面基于 **Jetpack Compose** 开发，完美融入 **Material Design 3** 规范，支持 **Material You 动态色彩** 匹配，带来前所未有的丝滑视觉享受。
- **📚 极致硬核漫画阅读器体验**：基于 Jetpack Compose 进行了极致的人性化与极客交互打磨：
  - **“首页一键续读”**：首页与侧滑抽屉实时加载最后一次阅读历史，点击直通阅读器恢复现场。
  - **“防刺眼护眼蒙层”**：基于 `drawWithContent` 仅在渲染层加盖 10%-60% 不透明度遮罩，完全不增加节点，实现 100% 手势与菜单点击穿透。
  - **“大屏自适应双页”**：横屏或折叠屏智能开启双页合并，对大宽图进行智能独占单页保护，防拼错乱。
  - **“长按悬浮放大镜”**：利用 `Modifier.magnifier` 长按无延迟淡出 1.8x 圆形放大镜，松手即隐，零手势拦截，专治各种旁白小字。
  - **“极简状态电量时间胶囊”**：沉浸全屏阅读时，于右上角常驻毛玻璃小胶囊显示时间与电量，菜单唤出时自动平滑隐藏防重合。
  - **“返回进度非阻断保存”**：物理/虚拟返回双重绑定，通过全局 `ApplicationScope` 解决 ViewModel 销毁协程被 Cancel 的 Bug；同时通过 `LaunchedEffect` 进行图片加载后重定位，彻底击破 PagerState 异步零页截断 Bug，确保 100% 精确保存进度。
  - **“跨章连续自动滚动”**：自动滚动滑动到底部时保持状态不退出，静待下一章后台加载，重建后自动**无缝恢复自动滚动**，提供跨章无感连读；同时将速度档位精细扩展至 1-10 档，满足极速读者需求。
- **🧹 结构化存储与 Coil 缓存管理**：独立的存储空间分析面板。可视化呈现 Coil 图片缓存与离线包大小，清除缓存与删除离线章节包改用**内存状态同步更新**，彻底规避无谓的重复全盘 I/O 物理扫描，即便在数万离线文件的极端环境下删除反馈也在 **16 毫秒**内瞬间完成并完美同步刷新 UI。
- **🔝 下载章节手动置顶与调度重排**：在章节下载列表提供“置顶插队”操作，Room 扩展 priority 权重字段，底层线程自动以 `priority DESC` 模式智能优先下载。
- **🤖 智能自动打卡**：每次加载用户信息时，系统会在后台安全、静默地**自动完成打卡签到**，并移除了原有的前台强阻断弹窗，打造完全无感的智能体验。
- **📥 智能离线下载管理**：提供高稳定性多线程漫画章节下载及断点续传能力。在下载详情页配备了高档的“双核”大操作按钮：
  - **“批量 WorkManager 任务调度”**：一键全部下载与多选章节下载重构为通过后台协程在 ViewModel 集中指派，利用批量 `enqueue(List)` 的 WorkManager 事务能力，**彻底避免了高频 SQLite 单次写入造成的 UI 线程阻塞和 ANR 隐患**。
  - **“进度对齐与防回退闪烁”**：前置过滤已下载文件并平滑对齐进度计数器，彻底消除了离线重试时进度“跳水式”的闪烁回退，并优化了高并发下的 SQLite 写锁竞争。
  - **“重新下载”**：专门针对 `FAILED` 失败任务启动重试保护。只有在存在下载失败章节时才可点击，且点击时**只对失败章节发起重新请求**，完全消除冗余请求开销。
  - **“开始阅读”**：优雅悬浮固定在详情页最底部，有已完成下载的章节时可直接一键智能启动阅读器浏览，带来极佳的离线闭环阅读体验。
- **🌐 网络高自愈与直连 DNS 竞速**：
  - **“直连 DNS 防污染”**：实现防劫持的 IP 直连解析，并前置 `isBikaHost` 过滤，防范 GitHub 升级检测等第三方正常网络请求被直连解析污染。
  - **“Fallback 超时自愈竞速”**：将多域名 Fallback 竞速超时合理化扩展为 3 秒，覆盖网络建连、图片拉取与 Coil 解码的全周期，保障在弱网环境下域名切换机制的真实有效性与自愈下载成功率。
- **🔄 应用内无缝更新**：内置版本更新检测机制，发现新版本时支持**应用内直接下载**。配备实时的**多线程下载进度条**，下载完成后引导一键安装，省时省力。
- **🪵 极客系统运行与调试日志**：在设置中提供一站式本地系统日志管理版块。支持“调试日志开关”（本地持久化）、“查看系统日志”（采用等宽字体 Monospace 对话框提供极佳阅读感，附带一键复制与清空），**读取海量日志大文件采用后台异步 I/O 线程调度**，彻底消除在查看日志时的界面短暂卡死冻结；基于安全 `FileProvider` 调用系统的 `ACTION_SEND` 分享意图安全地导出与分享日志。
- **🏷️ 流式历史阅读进度徽章墙**：重构了本地历史卡片。封面左上角半透明悬浮 `[已收藏]` 桃红色 Badge；右侧标题上方横向流式展现精美的彩色状态 Badge 墙（`[有更新]` / `[已读完]` / `[已阅读]` / `[已完结]`）。
  - **“一键追更快捷动作”**：在历史记录卡片被判定为 `[有更新]` 时，直接在卡片上悬浮高对比度“追更最新”按钮，点击秒开阅读器去读最新一话，缩短追更路径。
  - 采用 Room 版本 5 并利用高能协程，在详情页收藏/取消收藏时，本地历史库的收藏状态做到秒级无感同步！
- **🛠️ 顶尖现代化技术栈**：
  - **架构设计**：采用推荐的 MVVM / MVI 架构，数据单向流控制。
  - **依赖注入**：全面集成 **Hilt (Dagger)** 框架，保证依赖管理的整洁与高可扩展性。
  - **图片加载**：升级为 **Coil 3**，针对大图和漫画流进行了深度性能优化，加载更快，内存占用更低。
  - **异步编程**：基于 Kotlin **Coroutines (协程)** & **Flow**，全异步非阻塞操作，体验极其顺畅。

---

## 📁 项目模块结构

本项目采用现代 Android **多模块 (Multi-Module) 架构**设计，遵循高内聚、低耦合的模块化规范（如 Google *Now in Android* 架构模型）。

```text
BIKA
├── app/                  # 主应用模块：包含所有导航、页面流控及界面层(UI)组件
├── core/                 # 核心公共能力基础模块库
│   ├── common/           # 公共工具类、协程调度器与全局常量
│   ├── data/             # 数据统一分发仓储层 (Repository)，统一网络与本地存储数据源
│   ├── database/         # 本地数据库缓存层 (Room)，用于漫画、历史、下载等本地数据存储
│   ├── datastore/        # 键值对首选项存储层 (DataStore)，用于用户 Token 与偏好配置
│   ├── designsystem/     # 设计系统，统一定义 Material Design 3 主题色、字体及圆角样式
│   ├── model/            # 领域层实体模型 (Domain Model)，供全局使用的数据对象
│   ├── network/          # 网络层通信数据源 (Ktor/Retrofit)，提供与官方服务器的 API 通信
│   ├── testing/          # 单元测试与仪器测试辅助模块
│   └── ui/               # 共享基础 UI 组件与骨架屏
├── build-logic/          # 集中式 Gradle 统一构建逻辑插件 (Convention Plugins)
├── sync/                 # 后台同步与后台任务管理模块 (WorkManager)
└── benchmarks/           # 性能基准测试与启动速度测试模块
```

### 模块详细说明

| 模块路径 | 依赖层次 | 职能描述 |
| :--- | :--- | :--- |
| `:app` | App 顶层 | 包含应用的 Application 类、MainActivity 以及各功能模块（Dashboard, Login 等 Compose 页面）。管理全局 Compose 路由导航树。 |
| `:core:network` | Core 底层 | 封装 Ktor 客户端，编写与哔咔服务端交互的全部网络请求与序列化/反序列化逻辑。 |
| `:core:database` | Core 底层 | 基于 Room 实现的结构化本地存储，缓存需要离线展示或本地保存的漫画记录。 |
| `:core:datastore` | Core 底层 | 使用 Jetpack DataStore，加密/安全存储当前登录用户的 Token、打卡状态及个性配置。 |
| `:core:data` | Core 中间层 | 桥接 Network 与 Database/DataStore，对外暴露单一可信的数据源头 (Repository)，负责数据缓存更新策略。 |
| `:core:designsystem` | Core 底层 | 承载 BIKA 自定义的主题、颜色系统（适配动态色彩 Material You）、图标库等。 |
| `:core:ui` | Core 底层 | 提供全局可复用的 Compose UI 基础组件（如加载动画 CircularProgressIndicator 等）。 |
| `:core:model` | Core 底层 | 纯 Kotlin 模块，存储不包含任何框架代码的数据类（如 User, Comic 等领域模型）。 |
| `:core:common` | Core 底层 | 提供协程作用域调度、日志、字符串转换等通用 Extension 和工具类。 |
| `:build-logic` | 构建层 | 用于构建通用 build.gradle.kts 模板，解耦繁杂的 Gradle 依赖，提高多模块构建的性能与一致性。 |

---

## 📱 截图展示

<p align="center">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s2.webp" width="24%">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s3.webp" width="24%">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s4.webp" width="24%">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s5.webp" width="24%">
</p>
<p align="center">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s6.webp" width="24%">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s7.webp" width="24%">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s8.webp" width="24%">
  <img src="https://raw.githubusercontent.com/shizq123/BIKA/master/Screenshot/s9.webp" width="24%">
</p>

---

## 🛠️ 功能清单

- **🔑 账号系统**
  - [x] 登录与注册
  - [x] 忘记密码 / 修改密码
- **👤 用户中心**
  - [x] 个人信息精美展示
  - [x] **后台智能自动打卡 (签到)**
  - [x] **个性签名 (自我介绍) 修改**
  - [x] 头像修改与展示
  - [x] 本地历史记录 / 我的收藏 / 我的消息 / 我的评论
- **🏠 主页浏览**
  - [x] 精彩推荐 (本子妹 / 本子母)
  - [x] 多维度排行榜 (日榜 / 周榜 / 月榜 / 骑士榜)
  - [x] 最近更新 / 随机本子 / 漫画分类
- **📖 漫画与阅读**
  - [x] 漫画列表：排序选择、封印(筛选)、翻页导航
  - [x] **全局漫画卡片状态感知**（**全站所有展示漫画卡片的地方，封面单独悬浮叠加 `[已收藏]` 桃红色角标、标题上方横向流式 `[有更新]/[已读完]/[已阅读]/[已完结]` 彩色状态徽章墙**）
  - [x] **历史记录卡片「一键追更」快捷跳转**（检测到有更新时直接在历史记录页一键追更，秒开阅读器最新话，闭环操作路径）
  - [x] 漫画详情、推荐本子、查看评论
  - [x] **智能多线程离线下载与章节详情管理**（支持断点续传、失败章节“重新下载”重试保护、一键“开始阅读”底部悬浮智能按钮、**批量入队 WorkManager 并收拢调度防 ANR**）
  - [x] **高性能漫画阅读器** (支持平滑翻页与手势、**跨章无感连续自动滚动与 1-10 档精细化调速**)
  - [x] **首页“一键续读”卡片与抽屉快捷通道**（无历史记录时自动智能隐藏）
  - [x] **全穿透防刺眼暗光/护眼蒙层**（10% - 60% 亮度细微滑动调节，100% 手势穿透）
  - [x] **自适应大屏/横屏双页合并阅读**（带自适应宽图独占单页跨页保护，且清理了 PagerLayout 编译器冗余警告）
  - [x] **长按局部浮空放大镜**（1.8x 无级跟手动效，松手即隐，零手势拦截）
  - [x] **沉浸式极简毛玻璃时间/电量状态胶囊**（高精度动态刷新，呼出菜单自动避让）
  - [x] **返回进度保存防协程取消与 PagerState 零页裁剪加载定位纠偏机制**
  - [x] **存储空间与 Coil 缓存清理**（一键清空及按漫画目录级递归删除，**利用内存状态同步瞬时响应，规避磁盘全扫卡顿**）
  - [x] **下载队列手动一键置顶与 priority 权重重排调度**
- **🔍 智能搜索**
  - [x] 关键词搜索 / 热搜推荐 / 搜索历史记录
- **⚙️ 系统功能**
  - [x] **在线版本检查 & 应用内多线程升级包下载（带进度条）**
  - [x] **系统运行与调试日志配置面板**（支持日志开关本地持久化、Monospace 实时查看与清空、**异步后台 I/O 非阻塞读取**、FileProvider 系统安全分享与导出）
  - [x] **防劫持直连 DNS 机制**（引入 `isBikaHost` 过滤防非哔咔域名污染，配备 3 秒超时 Fallback 竞速高连通率）
  - [x] 支持多渠道与系统设置

---

## 📥 下载安装

最新版客户端可前往 GitHub Release 页面进行下载：

👉 **[[点击前往 GitHub Releases 下载最新 APK]](https://github.com/shizq123/BIKA/releases)**

---

## 💝 致谢与参考

感谢以下开发者对本项目做出的贡献与灵感启发：

- [@SOCK-MAGIC](https://github.com/SOCK-MAGIC) —— 提供了优秀的阅读器功能实现。
- [@niuhuan](https://github.com/niuhuan) —— 提供了诸多体验与功能的深度优化。

---

## 📝 开发笔记

关于项目更多的开发笔记、调试说明或技术架构分析，请参阅：
- 📄 **[[BIKA 开发笔记]](https://github.com/shizq123/BIKA/blob/master/NOTE.md)**
