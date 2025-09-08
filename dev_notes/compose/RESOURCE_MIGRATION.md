# QuickOutline 资源系统迁移与规范

本文记录从早期字符串路径加载到采用官方 Compose Multiplatform 资源生成体系的演进、关键问题、当前规范与扩展路线。

## 0. 目标
- 摆脱弃用的 `painterResource(String)` / ClassLoader 手工加载
- 使用官方 `compose.components.resources` 生成类型安全访问器
- 统一资源目录，减少出错面
- 抽象 `AppIcon` 语义层，便于未来主题/本地化/预加载

## 1. 演进阶段（摘要）
| 阶段 | 方案 | 动机 | 结果 |
|------|------|------|------|
| 初始 | 直接 `painterResource("drawable/xxx.svg")` | 快速实现 | 弃用警告 |
| 过渡 | 自建 `ResourceLoader` + `SvgIcon` | 临时规避警告 | 仍然字符串脆弱 |
| 修正 | 删除重复 drawable 目录 | 恢复 Res 生成 | 获得 `Res.drawable.*` |
| 优化 | 引入 `AppIcon` | 统一语义、封装包名 | 可扩展 |
| 精简 | group 改为 `com.ririv` | 消除包名重复风险 | 当前结构稳定 |
| 清理 | 移除临时 Loader / SvgIcon | 减少维护 | 纯净 |

## 2. 生成失败根因与解决
| 症状 | 根因 | 解决 |
|------|------|------|
| `Unresolved reference: Res` | 双资源目录冲突 | 仅保留 `src/main/resources/drawable` |
| 包名含双重 quickoutline | group 与 rootProject.name 重复 | 精简 group = `com.ririv` |
| 想自定义包名失败 | 本版本无 DSL 字段 | 使用默认规则 |

## 3. 目录结构
```
src/main/resources/
  drawable/        # 图标 (svg/png)
  (未来) strings/  # 字符串资源
```
生成（只读）：`com/ririv/quickoutline/generated/resources/Res.kt`

## 4. 图标使用规范
新增：
1. 放入 `drawable/xxx.svg`
2. 构建生成访问器
3. `AppIcons.kt` 添加枚举(data object)与映射
4. UI 使用 `AppIcon(icon = AppIcon.Xxx)`

禁止直接分散引用 `Res.drawable.*`（集中在 `AppIcons.kt`）。

## 5. 预加载
可选：`PreloadPrimaryIcons()` 在根 Composable 调用一次。

## 6. 命名约定
| 类型 | 规则 |
|------|------|
| 文件 | snake_case 或中文直写 |
| AppIcon | PascalCase |
| 中文文件名 | 生成反引号标识符，谨慎避免与英文重名 |

## 7. 清理状态
| 项目元素 | 状态 |
|----------|------|
| `ResourceLoader.kt` | 已删除 |
| `SvgIcon.kt` | 已删除 |
| 旧字符串路径调用 | 已替换 |

## 8. 路线图
| 方向 | 说明 | 优先级 |
|------|------|--------|
| 字符串资源化 | 引入 `strings/` + 访问封装 | 中 |
| 暗色图标变体 | 主题判断切换资源 | 中 |
| 图标使用统计 | 在 AppIcon 层插桩 | 低 |
| 缺失资源测试 | 遍历 Res 校验 | 低 |

## 9. 决策摘要
| 决策 | 理由 |
|------|------|
| 保留 AppIcon | 控制点单一，未来扩展方便 |
| 不 hack 包名 | 避免升级破裂 |
| 精简 group | 清晰坐标 & 避免重复 |

## 10. 快速检查
- [ ] 仅一个 drawable 目录
- [ ] 新图标已添加 AppIcon 分支
- [ ] 无直接 `painterResource("drawable/")` 调用
- [ ] 构建通过

更新日期: 2025-09-09
