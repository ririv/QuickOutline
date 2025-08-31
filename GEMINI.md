# GEMINI Project: QuickOutline

## Project Overview

QuickOutline is a cross-platform desktop application built with Java and JavaFX that allows users to add, edit, and manage the table of contents (outlines) of PDF files. The application provides a user-friendly interface for manipulating PDF outlines, with features like automatic indentation, page offsets, and integration with Visual Studio Code for advanced text editing. It leverages the iText and Apache PDFBox libraries for PDF manipulation and uses Gradle for build automation, with support for creating native installers for Windows, macOS, and Linux using the jpackage tool.

## 使用compose重写javafx应用
目前我们正使用compose重写javafx应用
- 对于项目原有的事件总线机制（用于javafx视图更新），使用状态管理代替
- 对于Guice，使用Koin代替
- 对于javafx的UI，使用compose代替
- 你只需要重写View及其相关的层，核心业务逻辑不需要改动
- 对于javafx自定义的控件，其都为基本控件，你需要使用compose的基本控件代替
- javafx的相关代码我们放到了根目录下的fx目录下，它虽然已经被git忽略，但你应该参考，你随时可以查阅
- 你需要最大程度的的将重写后的UI与原有UI在视觉层面保持一致
- 对于图标的载入，先试用占位符代替
- 这是个桌面应用，你应该先考虑desktop，当然安卓的支持以后也会引入，但目前不是重点

我们目前已经实现了一部分，你需要继续

## 回答语言
请使用简体中文回答

对于依赖的API调用，可以搜索官方文档

你可以在编辑代码完成后，使用 `./gradlew build` 来检查代码是否编译通过，其他时候请不要运行

请不要修改项目中的正则表达式
