
# 构建、运行、打包（非开发人员略过）

## 版本一致性
为确保兼容，请保证版本一致性，目前项目采用：
- Java 21 (LTS)
- JavaFX 21 (LTS)
- Gradle 8.12

由于项目使用了 jlink 打包需要模块化项目，却引用了非模块化项目（iText），因此需要注意在 Gradle 中处理模块化问题
```
plugins {
    ...
    id 'org.openjfx.javafxplugin' version '0.1.0'
    // 参考 https://github.com/openjfx/javafx-gradle-plugin#extra-plugins
    // 不加入此会出现找不到模块错误
    id 'org.javamodularity.moduleplugin' version '1.8.15'
    
    // 请使用高版本，'2.26.0'实测出现 "Unsupported class file major version 65"错误
    id 'org.beryx.jlink' version '3.1.1'
}
```

## 运行
请使用 Gradle 下的 Run 任务，不要使用IDEA自带的Main入口处运行（App）

## 打包

### 手动打包
运行 `Gradle - build - jpackageImage` 任务可以直接成功打包成应用镜像（可执行文件）

运行 `jpackage` 任务则打包成安装包文件，在生成安装包时，需要操作系统相关的工具，例如：
- Windows：需要安装 WiX Toolset。
- macOS：需要 Xcode 和开发者签名。
- Linux：需要 dpkg 或 rpm。

> **Note:** jpackage 依赖于旧版本的 WiX，提示"找不到 WiX 工具 (light.exe, candle.exe)"
>
> 因此请使用 WiX 3，并确保它添加到环境变量

### 自动打包
本仓库已在 github actions 启用自动打包功能，也可手动触发 acitons 打包

### 减小打包体积
发现3个因素影响：
- 排除不必要的依赖
- 加入 jlink 的压缩选项
- 不同的 jdk 发行版，目前采用的版本是 corretto-21（经测试几个发行版对比打包体积最小）

## Windows 下开发控制台输出中文乱码问题 TODO
打包时可能会出现日志乱码问题，设置 UTF-8 无果，临时解决方案（设置 GBK 编码）：

Gradle 运行配置（jpackage 等，你需要运行的任务）- 虚拟机选项（VM options）-添加
```
-Dfile.encoding=GBK
```

目前在 `.idea/runConfigurations` 中已包含了此配置项
