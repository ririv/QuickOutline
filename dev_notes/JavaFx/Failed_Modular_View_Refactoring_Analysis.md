# “模块化视图”重构方案失败复盘

本文档旨在记录一次失败的架构重构尝试，分析其背后的技术陷阱，并为未来提供经验教训。

## 初始目标

为了解决 `MainController` 可能变得臃肿的问题，我们曾尝试引入一个“模块化视图”或“复合组件”的架构。该方案的核心思想是：

- 创建一个抽象的 `FunctionalArea` 基类。
- 为每个功能区（如 `Bookmark`）创建一个具体的 `...Area` 类，该类继承 `FunctionalArea` 并负责**在 Java 代码中动态加载**其自身的 `...TabView.fxml` 和 `...BottomPane.fxml`。
- `MainController` 只负责管理和切换这些 `FunctionalArea` 对象，而不知道其内部视图的实现细节。

这个方案在理论上看起来非常优雅，因为它实现了高度的封装和解耦。

## 遇到的问题与失败原因

在实施过程中，这个方案引发了一系列难以解决的运行时异常，最终导致我们放弃了它。失败的根本原因在于，**该方案与 JavaFX FXML 的生命周期和依赖注入（DI）机制产生了根本性的冲突。**

### 陷阱 1: 依赖注入链断裂 (`NoSuchMethodException`)

- **现象**: 在 `BookmarkArea` 的构造函数中，`new FXMLLoader().load()` 尝试加载 `BookmarkTab.fxml` 时，因无法创建需要依赖注入的 `BookmarkTabController` 而抛出 `NoSuchMethodException`。
- **原因**: `new FXMLLoader()` 创建的加载器是一个“素”加载器，它与应用的 Guice `Injector`（注入器）完全隔离。它不知道如何为控制器提供 `AppEventBus` 等依赖，因此只能尝试调用无参构造函数，而该构造函数不存在。
- **教训**: 任何在运行时动态创建的、需要 DI 容器支持的 `FXMLLoader`，都必须通过 `loader.setControllerFactory(injector::getInstance)` 的方式，手动将其与 `Injector` 关联起来。这需要将 `Injector` 实例一路传递下来，增加了代码的复杂性和耦合度。

### 陷阱 2: 资源包 (`ResourceBundle`) 丢失 (`LoadException: No resources specified`)

- **现象**: 解决了 DI 问题后，`loader.load()` 又抛出异常，提示 FXML 中使用的 `%key` 国际化文本没有找到对应的资源。
- **原因**: 与陷阱1类似，`new FXMLLoader()` 不仅不认识 DI 容器，也不认识我们在应用启动时加载的 `ResourceBundle`。所有国际化文本都解析失败。
- **教训**: 动态创建的 `FXMLLoader` 必须像 `App.start()` 中那样，在构造时就传入 `ResourceBundle`：`new FXMLLoader(url, bundle)`。这进一步增加了参数传递的复杂性。

### 陷阱 3: UI 组件单例冲突 (`IllegalArgumentException`)

- **现象**: 解决了上述问题后，当视图发生切换和重建时，应用抛出 `IllegalArgumentException: ... is already set as root of another scene`。
- **原因**: 我们的“模块化”方案会在每次切换时都 `new BookmarkArea(...)`，这会导致 `BookmarkBottomPane` 被重复加载。而 `BookmarkBottomPane` 内部又依赖了被 Guice 设置为**单例 (Singleton)** 的 `GetContentsPopupController`。当第二次加载时，代码试图将这个**同一个** `PopupController` 的根节点（一个已经被旧场景占用的 UI 节点）添加到一个新创建的 `PopupCard` 场景中，从而违反了 JavaFX “一个节点只能有一个父节点”的核心原则。
- **教训**: UI 组件（尤其是 `Node` 及其子类）的控制器，绝对不能被配置为应用级的单例，除非能保证它在整个生命周期中只被创建和添加一次。UI 组件的生命周期应该由其父容器管理，而不是由 DI 容器的全局作用域管理。

## 最终结论

试图在 Java 代码中通过 `new FXMLLoader()` 动态构建和管理复杂的、依赖注入的、国际化的 FXML 视图体系，是一条充满陷阱的道路。它破坏了 FXML 框架提供的“声明式UI”和“自动注入”的便利性，迫使开发者手动处理大量本应由框架完成的底层工作，导致代码变得复杂、脆弱且难以调试。

**正确的、更稳健的方案是回归 JavaFX 的标准实践：**

1.  **让 FXML 做它最擅长的事**：在父 FXML (`MainView.fxml`) 中，通过 `<fx:include>` **声明式地**包含所有需要的子视图和功能区。
2.  **让 Controller 专注于状态**：`MainController` 的职责是获取所有这些预先加载好的、由框架负责注入的 `@FXML` 节点引用，然后通过简单的**属性绑定** (`visibleProperty().bind(...)`) 来控制它们的可见性，响应用户的操作和状态的变更。

这个方案虽然在 `MainController` 中看起来有较多的 `@FXML` 字段，但它逻辑清晰、行为可预测，并且完全规避了上述所有关于生命周期、依赖注入和资源加载的陷阱。
