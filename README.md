# QuickOutline

## 功能特性

- 添加目录
  - 按缩进添加目录
  - 按序号添加目录
  - 页码偏移（支持负数）
  - 设置缩放模式
- 文本编辑模式下
    - 自动缩进
    - Tab | Shift+Tab 快速缩进
    - VSCode编辑器同步（可使用正则表达式替换）
- TOC提取
- 多平台 Windows，Mac，Linux(Ubuntu)
- 简洁的 UI

## 界面

![interface](image/screenshot.png)

## 使用方式

1. 拖动PDF文件到窗口
2. 写入目录文本，格式在下面
3. 设定 页面偏移量=PDF中的页码–原书的页码
4. 添加目录，完成！

### 按序号
```
1  我是标题  1
1.1  我是子标题  2
1.1.1  我是子子标题 3
```
此方式如有缩进将会自动去除，不会影响，最终生成的PDF中标题中也会带序号

### 按缩进（推荐使用制表符Tab键）
```
我是标题  1
    我是子标题  2
        我是子子标题  3
```
此方式如有序号将会视作标题，不会影响

### 查找想要的目录

1. 各大书评、卖书等网站，均能找到相应书的目录，这里推荐，京东、豆瓣、淘宝

2. 文字类 PDF，使用软件中的 TOC 提取功能（或自己手动打开 PDF 复制 TOC 到本软件中）

> **Note**: 本软件不支持 OCR，因此也不支持图片类PDF（如扫描件）的 TOC 提取，图片 PDF 可以先使用外部 OCR 提取
>
> 目前最新的 Mac 与 Windows 系统均自带 OCR 功能，Windows 也可使用微软官方出品的 PowerToys 里的文本提取器功能。

演示

![screenvideo_1](image/screenvideo_1.gif)

[1.0版本使用说明移步这里](https://zhuanlan.zhihu.com/p/390719305)


### Tips
1. 页码偏移量
添加目录时，会自动加上页码偏移量，支持负数，但相加后的结果不要超出实际页码范围

注：1.0 版本在获取当前PDF目录时，会自动减去页码偏移量；2.0+版本此按钮改为获取目录，不会有此行为，即页码偏移量仅会在添加目录时作用（相加）

2. 中文序号支持

> 请注意，仅支持部分可识别的中文序号，如 篇、章、节、部分

3. 自动缩进

自动缩进是按序号进行的

不仅仅是会自动缩进， 同时也会自动格式化：

- 自动切分，如
```
第一章我是标题21
->
第一章 我是标题 21
```


注意，使用自动缩进得到的文本层次结构，与直接使用按序号的方式添加的目录层次结构是一样的

主要用于在软件无法按序号识别某条目的层级时，可手动添加缩进进行快速层级微调，添加目录时记得选按缩进方式

## 安装

### Linux

Linux 提供 deb 和 tar.gz 两种包，测试环境为 Ubuntu 22

#### deb 安装包


Q：缺少对应的依赖 liboss4-salsa-asound2 或 libasound2t64

A：安装对应的依赖即可

Q: 安装出现 `xdg-desktop-menu: No writable system menu directory found.`

该问题常见于 WSL2，没有真正的 Ubuntu 图形界面引起的

A：运行如下命令

```shell
sudo mkdir /usr/share/desktop-directories/
```

#### tar.gz 压缩包

解压后直接运行 bin 文件夹下的同名软件即可

## 使用 VSCode 以使用高级编辑功能

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

## 开源软件使用

- iText (AGPL Licence)

## 下载

Windows: 解压后直接运行 `.exe` 文件

Mac: 提供安装包，目前打开所生成的 PDF 文件所在位置功能有点小问题，不可用

[下载地址](https://github.com/ririv/QuickOutline/releases)

---

## 开发
想要为此项目做贡献继续开发的小伙伴们请参考此[文档](Dev_doc.md)

---

## TODO
- 树视图操作
- TOC插入
- Page Label
- 自定义正则表达式
- 页码罗马数字支持
- 文档分析自动生成目录，类似 Chrome插件-谷歌学术PDF阅读器（不知如何实现，有想法的可以交流）
