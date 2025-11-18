## 相关类
com/itextpdf/html2pdf/attach/impl/tags/SvgTagWorker.java
com/itextpdf/html2pdf/util/SvgProcessingUtil.java
com/itextpdf/svg/SvgConstants.java
com/itextpdf/svg/utils/SvgCssUtils.java
com/itextpdf/styledxmlparser/css/util/CssDimensionParsingUtils.java

## 流程

SvgTagWorker.java 中是SVG标签的处理器，负责将SVG元素转换为iText的PDF元素。它会解析SVG的属性和样式，然后调用SvgProcessingUtil中的方法进行具体的SVG处理。
SvgProcessingUtil.java 提供了一些实用方法，用于处理SVG数据，比如解析路径、转换坐标等。它是SvgTagWorker的辅助类，封装了SVG处理的核心逻辑。
SvgConstants.java 定义了一些与SVG相关的常量，比如命名空间、属性、名称等，方便在代码中引用，避免硬编码。
