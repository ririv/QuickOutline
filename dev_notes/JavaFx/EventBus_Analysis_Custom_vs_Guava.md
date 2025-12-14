# EventBus 设计模式：自定义实现与 Guava EventBus 对比分析

## 1. 引言

本文旨在深入探讨事件总线（EventBus）设计模式在项目中的应用。通过对比一个简洁的自定义实现 `AppEventBus` 与业界广泛使用的 Google Guava `EventBus`，分析两者的优劣，并为项目未来的技术选型提供明确的建议。

## 2. 自定义 `AppEventBus` 实现分析

项目中包含一个自定义的事件总线实现，代码如下：

```java
package com.ririv.quickoutline.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AppEventBus {

    private static final AppEventBus INSTANCE = new AppEventBus();
    private final Map<Class<?>, List<Consumer<?>>> subscribers = new HashMap<>();

    private AppEventBus() {}

    public static AppEventBus getInstance() {
        return INSTANCE;
    }

    public <T> void subscribe(Class<T> eventType, Consumer<T> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(subscriber);
    }

    public <T> void unsubscribe(Class<T> eventType, Consumer<T> subscriber) {
        List<Consumer<?>> eventSubscribers = subscribers.get(eventType);
        if (eventSubscribers != null) {
            eventSubscribers.remove(subscriber);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<Consumer<?>> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers != null) {
            for (Consumer<?> subscriber : new ArrayList<>(eventSubscribers)) {
                ((Consumer<T>) subscriber).accept(event);
            }
        }
    }
}
```

这是一个非常清晰、经典的观察者模式实现，采用了单例模式和编程式的订阅方式。它能够满足基本的事件通信需求，也是理解事件总线模式的绝佳实践。

## 3. 对比与核心优势：为何推荐 Guava EventBus？

尽管自定义实现清晰易懂，但与 Guava 这种工业级的实现相比，在健壮性和高级特性上存在差距。切换到 Guava EventBus 的核心好处如下：

| 特性 | 您的自定义实现 (`AppEventBus`) | Google Guava `EventBus` | 优势说明 |
| :--- | :--- | :--- | :--- |
| **线程安全** | **不安全**。多线程下并发操作可能导致程序崩溃。 | **完全线程安全**。 | 无需任何额外同步代码，即可在任何线程中安全地发布和订阅事件。**这是最重要的优势**。 |
| **异常处理** | **脆弱**。单个订阅者出错，将导致后续所有订阅者无法收到事件。 | **健壮**。自动隔离并报告出错的订阅者，不影响其他订阅者接收事件。 | 保证了系统的稳定性和可靠性，一个组件的错误不会引发雪崩效应。 |
| **事件继承** | **不支持**。只能订阅精确的事件类型。 | **支持**。订阅父事件，可以接收所有子类事件的通知。 | 极大地增强了灵活性，可以用一个方法处理一整类相关的事件。 |
| **订阅方式** | **编程式** (`subscribe(Class, Consumer)`)。 | **声明式** (`@Subscribe` 注解)。 | 代码更简洁，可读性更高，组件与事件总线的耦合度更低。 |
| **维护成本** | **您自己**。需要自行修复bug和进行功能迭代。 | **Google 团队**。 | 免费享受顶尖团队的维护和更新，可以更专注于自身业务。 |

## 4. Guava EventBus 工作原理解析

Guava 的设计非常优雅，它通过 Java 的“反射”机制，将事件和订阅者方法智能地关联起来。

### 核心规则

1.  **注册**: 调用 `eventBus.register(this)` 时，Guava会扫描这个对象里所有被 `@Subscribe` 注解标记的 `public` 方法。
2.  **识别**: Guava 通过检查每个 `@Subscribe` 方法的**唯一参数的类型**，来确定这个方法订阅的是哪一种事件。
3.  **广播**: 调用 `eventBus.post(someEvent)` 时，Guava 会找到所有订阅了 `someEvent` **这个类型**（或其父类型/接口）的方法，并调用它们。

### 示例

```java
// 1. 定义事件（支持继承）
class ErrorEvent { /* ... */ }
class FileErrorEvent extends ErrorEvent { /* ... */ }

// 2. 定义订阅者
public class ErrorHandler {
    public ErrorHandler(AppEventBus eventBus) {
        eventBus.register(this); // 注册自身
    }

    // 此方法订阅了 FileErrorEvent
    @Subscribe
    public void handleFileError(FileErrorEvent event) {
        System.out.println("处理文件错误...");
    }

    // 此方法订阅了父类 ErrorEvent
    @Subscribe
    public void handleAnyError(ErrorEvent event) {
        System.out.println("处理所有错误...");
    }
    
    // 此方法也订阅了父类 ErrorEvent
    @Subscribe
    public void logAnyError(ErrorEvent event) {
        System.out.println("记录所有错误日志...");
    }
}

// 3. 发布事件
eventBus.post(new FileErrorEvent());
```

**执行结果:**
当发布一个 `FileErrorEvent` 时，输出将会是：
```
处理文件错误...
处理所有错误...
记录所有错误日志...
```
因为 `FileErrorEvent` 同时满足了三个方法的订阅条件（它本身、它的父类`ErrorEvent`），所以这三个方法都会被调用。

**重要**: Guava **不保证**这三个方法的执行顺序，因此每个订阅者方法都应该是独立的。

## 5. 结论与建议

在软件工程中，我们应遵循“不要重复造轮子”的原则，特别是当社区已存在一个近乎完美的、工业级的解决方案时。

**强烈建议项目切换到 Google Guava 的 `EventBus` 实现。**

这样做能让您用更少的代码，免费获得一个功能更强大、更安全可靠的事件处理系统，从而让您能更专注于应用本身的核心功能开发。
