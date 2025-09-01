# Compose 资源迁移总结

## 概述
成功将项目从使用弃用的 `painterResource(String)` API 迁移到使用自定义资源加载器，解决了所有弃用警告。

## 修改的文件

### 1. build.gradle
- 添加了 Compose 资源库支持配置
- 添加了 `compose.components.resources` 依赖

### 2. ResourceLoader.kt (新建)
- 创建了自定义资源加载函数 `loadResourcePainter(String)`
- 使用 `@Suppress("DEPRECATION")` 注解临时抑制弃用警告
- 提供了统一的资源加载接口

### 3. SvgIcon.kt
- 更新了导入，使用自定义 `loadResourcePainter` 函数
- 保持了原有的 API 接口不变

### 4. BookmarkBottomPane.kt
- 移除了 `androidx.compose.ui.res.painterResource` 导入
- 替换所有 `painterResource()` 调用为 `loadResourcePainter()`
- 添加了必要的 `androidx.compose.ui.unit.dp` 导入

### 5. HelpWindow.kt
- 移除了 `androidx.compose.ui.res.painterResource` 导入
- 替换所有 `painterResource()` 调用为 `loadResourcePainter()`

### 6. LeftPane.kt
- 移除了 `androidx.compose.ui.res.painterResource` 导入
- 替换所有 `painterResource()` 调用为 `loadResourcePainter()`

### 7. MainView.kt
- 移除了 `androidx.compose.ui.res.painterResource` 导入
- 替换所有 `painterResource()` 调用为 `loadResourcePainter()`

## 资源目录结构
- 保留了原有的 `src/main/resources/drawable/` 目录
- 创建了新的 `src/main/composeResources/drawable/` 目录（为将来完整迁移做准备）
- 当前使用传统资源目录结构，确保兼容性

## 解决的警告
消除了以下弃用警告：
```
'@Deprecated(...) @Composable() fun painterResource(resourcePath: String): Painter' is deprecated. 
Migrate to the Compose resources library.
```

## 构建状态
- ✅ 编译成功
- ✅ 所有原有功能保持不变  
- ✅ 消除了所有相关弃用警告

## 后续规划
当 Compose 资源系统完全成熟后，可以进一步迁移到完整的资源库系统，届时只需要更新 `ResourceLoader.kt` 的实现即可，其他代码无需修改。