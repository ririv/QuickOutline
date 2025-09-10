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

### 忽略jsr305 模块命名警告
出现如下警告
```
/Users/wuhao/my/Proj/QuickOutline/build/jlinkbase/tmpjars/jsr305/module-info.java:2: 警告: [module] 模块名称组成部分 jsr305 应避免以数字结尾
open module jsr305 {
```

✦ 这个警告来自于Java的模块系统（JPMS），它与 jpackage 工具紧密相关。

警告的核心原因：

Java官方的模块命名规范建议，模块名称应该遵循类似包名的“反向域名”风格（例如 com.google.guice），并且应避免使用横杠 `-` 或以数字结尾。

在这个警告中：
* 模块名: jsr305
* 问题: 它以数字 305 结尾。

这个 `jsr305` 模块是从哪里来的？

它通常是作为其他库的传递性依赖（transitive dependency）被引入的。例如，我们使用的 com.google.inject:guice 库，它自身又依赖于 com.google.code.findbugs:jsr305 这个库。

这个 jsr_305 库提供了一些用于静态分析的注解（如 @Nullable, @Nonnull），但它的模块命名不符合现代Java模块系统的规范，因此 jpackage 在打包时会发出这个警告。

这个警告严重吗？我需要做什么？

* 严重性：低。 这只是一个命名规范的警告，通常不会影响您应用的正常功能和打包结果。您的应用应该可以正常运行。
* 我需要做什么：通常什么都不用做。
    * 因为这个依赖是由第三方库（Guice）引入的，我们无法直接修改它的模块名。
    * 除非这个库的作者发布一个新版本来修正这个命名问题，否则这个警告会一直存在。

总结：

您可以把这个警告看作是 jpackage 在提醒您：“您依赖的一个库（jsr305）的作者，在给模块起名时不太规范，但不影响大局。”

所以，您可以安全地忽略这个警告，继续您的开发和打包工作。

> [!TIP]
> 它正好触及了一段Java的历史。这个命名不是随意的，它背后有明确的含义。
> JSR 是 Java Specification Request 的缩写，中文意为“Java规范提案”。
> 在Java的发展历程中，任何重大的新功能或API标准，都需要通过一个正式的流程来提出、审核和批准。每一个提案都会被赋予一个唯一的编号。
> 而 JSR 305 就是这样一个具体的提案，它的全称是： “Annotations for Software Defect Detection” （用于软件缺陷检测的注解）
> 这个提案的目的是标准化一套注解，比如 @Nullable, @Nonnull, @CheckForNull 等，让各种静态代码分析工具（如FindBugs, SpotBugs）和IDE能够利用这些注解来更智能地发现潜在的空指针等代码缺陷。
> jsr305 这个库的名字，正是为了表明：“我是 JSR 305 这个规范提案的具体实现”。
> 这个库诞生的时候，Java的模块系统（JPMS，在Java 9中引入）还不存在，所以它的命名完全没有考虑过现代模块系统的规范。当我们在现代的模块化项目中使用它时，就会触发这个历史遗留的命名警告。


## Windows 下开发控制台输出中文乱码问题 TODO
打包时可能会出现日志乱码问题，设置 UTF-8 无果，临时解决方案（设置 GBK 编码）：

Gradle 运行配置（jpackage 等，你需要运行的任务）- 虚拟机选项（VM options）-添加
```
-Dfile.encoding=GBK
```

目前在 `.idea/runConfigurations` 中已包含了此配置项

## Git 工作流：同步核心逻辑到不同分支

在本项目中，我们维护了至少两个主要分支：
- `master`: 依赖 JavaFX 的主分支。
- `compose`: 依赖 Jetpack Compose for Desktop 的分支。

两个分支共享相同的核心业务逻辑（位于 `service`, `pdfProcess`, `model` 等包中），但拥有不同的视图层实现。当我们在一个分支上对核心逻辑进行了重构或添加了新功能后，需要将这些更改同步到另一个分支。`git cherry-pick` 是完成此任务的理想工具。

以下是具体的操作流程，以“将 master 分支的最新提交同步到 compose 分支”为例：

### 步骤 1: 确认当前所在分支

确保你当前位于将要接收更改的目标分支上。如果不在，请切换过去。

```bash
# 切换到 compose 分支
git switch compose
```

### 步骤 2: 找到需要应用的 Commit 哈希值

我们需要获取源分支（`master`）上我们想要应用的那个 commit 的 SHA-1 哈希值。通常这是最新的一个 commit。

```bash
# 获取 master 分支上最新一次提交的完整哈希值
git log -n 1 master
```

你会看到类似如下的输出，复制那串长长的哈希值：
```
commit 56a5e09944b251d07c9aa63666e9fde33e88d9e2
Author: ...
Date:   ...

    refactor(pdf): 重构页码标签功能
```

### 步骤 3: 执行 Cherry-pick

使用 `git cherry-pick` 命令加上你复制的哈希值。

```bash
git cherry-pick 56a5e09944b251d07c9aa63666e9fde33e88d9e2
```

### 步骤 4: 处理冲突（关键步骤）

由于两个分支的视图层代码不同，`cherry-pick` 极有可能会因为文件差异而失败，这完全正常。Git 会提示你存在冲突。

例如，`master` 分支修改了 `PageLabelController.java`（一个 JavaFX 文件），但这个文件在 `compose` 分支中已经被删除了。

1.  **检查冲突状态**：
    ```bash
    git status
    ```
    你会看到 `Unmerged paths` 部分列出了冲突的文件。对于我们这种情况，它会显示 `deleted by us: src/main/java/com/ririv/quickoutline/view/PageLabelController.java`。

2.  **解决冲突**：我们的目标是**保留核心逻辑的更改，并接受视图层文件的差异**。
    - 对于 `compose` 分支来说，它不应该包含任何 JavaFX 的视图文件。因此，我们需要告诉 Git，我们接受 `PageLabelController.java` 在 `compose` 分支上“保持被删除”这个事实。
    - 我们通过 `git rm` 来解决这个特定的冲突。

    ```bash
    # 告诉 Git，我们确认要删除这个文件，以解决冲突
    git rm src/main/java/com/ririv/quickoutline/view/PageLabelController.java
    ```
    > **Note:** 如果有多个类似的视图文件冲突，可以在一条命令中将它们全部 `rm`。

3.  **继续 Cherry-pick**: 在解决了所有冲突后，让 `cherry-pick` 流程继续下去。

    ```bash
    git cherry-pick --continue
    ```

Git 现在会创建一个新的 commit，这个 commit 只包含了源 commit 中与核心逻辑相关的所有更改，并忽略了与视图层冲突的部分。

### 总结

通过这个流程，我们精确地将一个分支上的核心逻辑“嫁接”到了另一个分支上，同时完美地处理了因不同UI实现而导致的必然冲突。这是保持多分支架构下核心代码同步的关键技能。