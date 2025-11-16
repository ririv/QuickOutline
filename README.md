# QuickOutline

> [!note] 
> QuickOutline 3 将在不久后发布，将支持制作TOC、Markdown插入PDF页面等新功能
> 
> 想提前体验的小伙伴们可以下载nightly构建版本，敬请期待！


## 使用方式 Usage


![interface](image/screenshot.png)


---

1. 选择/拖动PDF文件到窗口
2. 写入目录文本，格式如下
3. 设定 页面偏移量=PDF中的页码–原书的页码
4. 添加目录，完成！
---
1. Select / drag the PDF file to the window
2. Write the table of contents text in the following format
3. Set page offset = (page number in PDF) - (page number in original book)
4. Add the bookmarks, done!

---

### 添加目录

#### 按序号
```
1  我是标题  1
1.1  我是子标题  2
1.1.1  我是子子标题 3
```
此方式如有缩进将会自动去除，最终生成的PDF中标题中也会带序号

#### 按缩进（推荐使用制表符Tab键）
```
我是标题  1
    我是子标题  2
        我是子子标题  3
```
此方式如有序号将会视作标题

> **正则表达式**可以使用软件内置的VSCode同步功能：文本视图下使用 VSCode 功能进行编辑，内容会自动同步，期间可以使用”自动缩进"功能



#### Tips
1. 页码偏移量
   添加目录时，会自动加上页码偏移量，支持负数，但相加后的结果不要超出实际页码范围

2. 自动缩进

自动缩进是按序号进行的

不仅仅是会自动缩进， 同时也会自动格式化：

- 自动切分，如
```
第一章我是标题21
->
第一章 我是标题 21
```

使用自动缩进得到的文本层次结构，与直接使用按序号的方式添加的目录层次结构是一样的

> 请注意自动缩进识别**中文序号**，仅支持部分可识别的序号，如 篇、章、节、部分




### 获取目录

- 本身自带书签，需要微调，打开文件后直接载入

- 文字类 PDF，如带有TOC，选中获取目录弹出框的的 **提取 TOC** 功能

- 将PDF上传到 AI（如DeepSeek，注意容量限制），让它识别

- 各大书评、卖书等网站，均能找到相应书的目录，这里推荐，京东、豆瓣、淘宝


> **Note**: 本软件不支持 OCR，因此也不支持图片类PDF（如扫描件）的 TOC 提取，图片 PDF 可以先使用外部 OCR 提取
>
> 目前最新的 Mac 与 Windows 系统均自带 OCR 功能，Windows 也可使用微软官方出品的 PowerToys 里的文本提取器功能。




## 下载与安装

请到 [GitHub Releases](https://github.com/ririv/QuickOutline/releases) 下载

### Windows:
- msi 安装包
- zip 压缩包（解压后直接运行 exe 文件）

### Mac
- dmg 安装包

### Linux
- deb 安装包
- tar.gz

测试环境为 Ubuntu 22


Q：缺少对应的依赖 liboss4-salsa-asound2 或 libasound2t64

A：安装对应的依赖即可

Q: 安装出现 `xdg-desktop-menu: No writable system menu directory found.`

该问题常见于 WSL2，没有真正的 Ubuntu 图形界面引起的

A：运行如下命令

```shell
sudo mkdir /usr/share/desktop-directories/
```


---

## 功能特性

- 添加目录
  - 按缩进添加目录
  - 按序号添加目录
  - 页码偏移（支持负数）
  - 设置缩放模式
  - 文本 / 树 双视图操作
- 文本编辑模式下
  - 自动缩进
  - Tab | Shift+Tab 快速缩进
  - VSCode编辑器同步（可使用正则表达式替换）
- TOC提取
- 添加页面标签（PageLabel）
- 多平台 Windows，Mac，Linux(Ubuntu)
- 语言 / Language: 中文，English

> Tip: This software has added English support and will automatically switch the software language according to the system language and region.

---


## 使用 VSCode 编辑以使用正则表达式等功能
<details>
<summary>展开/收起</summary>

本软件不提供高级编辑功能（如正则表达式，VSCode 自带此功能）

如想使用，请使用软件中提供的 VSCode 按钮以启动

VSCode 中的内容会自动同步至软件窗口中（需在 VSCode 中保存文件，可以打开自动保存功能）

注意此同步是单项同步，即 VSCode → 本软件

但在此期间，你可以使用软件中的自动缩进功能，此时软件中文本也会立即至 VSCode 中

### 配置

请先下载 [VSCode](https://code.visualstudio.com/)

需要添加至环境变量，方法也很简单

### Windows

参考 [Visual Studio Code on Windows](https://code.visualstudio.com/docs/setup/windows)

安装时勾选"添加到 Path"（默认已勾选，用户无需进行任何操作），安装后需重启

> **Tip:** 若在下载时将其不慎取消勾选，可在找到安装目录下的 bin 文件夹，将其添加到系统环境变量中的 Path

### MacOS

> **Note:** 由于该功能需要签名，目前 v2.0+ 的 Mac 版本已停用该功能

参考 [Visual Studio Code on macOS](https://code.visualstudio.com/docs/setup/mac#_launching-from-the-command-line)

1. 启动 VSCode.

2. 按下组合键 (Cmd+Shift+P)，输入 'shell command' 找到命令行: Install 'code' command in PATH command.

</details>

---

## 开发
想要为此项目做贡献继续开发的小伙伴们请参考此[文档](Dev_doc.md)

### 开源软件使用

- iText (AGPL Licence)


### TODO
- TOC插入
- 页码罗马数字支持
- AI：OpenAI SDK 支持

---

## ☕ 感谢支持！

如果您觉得 QuickOutline 对您有帮助，欢迎通过以下方式支持我，您的支持是我持续维护和开发新功能的巨大动力！

*   **关注我的小红书**  <a href="https://www.xiaohongshu.com/user/profile/5f988414000000000101ca29"><img src="https://img.shields.io/badge/小红书-关注我-FF6A6A?style=flat-square&logo=xiaohongshu" alt="小红书" style="vertical-align: middle;"></a>

[//]: # (*   **请我喝杯咖啡**  [微信支付]&#40;https://your_wechat_payment_link.com&#41; | [支付宝]&#40;https://your_alipay_payment_link.com&#41;)