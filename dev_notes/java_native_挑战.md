## 直接使用 GraalVM 本地编译

### 很多 native 配置需要进行
尤其是 gson 使用了反射
但这还好，可以解决

### awt 问题
PDFBox 依赖 awt 进行图像渲染
macos 下会报读取 awt 库错误

## 尝试使用 Quarkus native 解决

### 配置
帮我们省去了很多配置工作，很容易
且由于底层基于 vertx，因此迁移成本几乎为0

### awt问题依旧

https://github.com/quarkusio/quarkus/issues/50414
https://quarkus.io/guides/quarkus-runtime-base-image#extending-the-image

### 安卓运行问题
Quarkus 在安卓上运行可能有困难，Vertx 相对简单，虽然安卓上可以不走 http服务，但图像传输还是要走，可能需要使用替代的 http 方案