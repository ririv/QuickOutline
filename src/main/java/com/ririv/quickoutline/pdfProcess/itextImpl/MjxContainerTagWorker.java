package com.ririv.quickoutline.pdfProcess.itextImpl;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.impl.tags.SpanTagWorker;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Image;
import com.itextpdf.styledxmlparser.node.IElementNode;
import com.itextpdf.svg.element.SvgImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MjxContainerTagWorker
 * * 核心逻辑：继承 SpanTagWorker。
 * 这不仅解决了 ClassCastException，还让 iText 确信这个容器就是一行文字（Span），
 * 从而强制其内部的图片（公式）必须在行内显示，绝不换行。
 */
public class MjxContainerTagWorker extends SpanTagWorker {

    private static final Logger log = LoggerFactory.getLogger(MjxContainerTagWorker.class);

    public MjxContainerTagWorker(IElementNode element, ProcessorContext context) {
        // 必须调用父类构造函数，初始化内部的 Span 对象
        super(element, context);
    }

    @Override
    public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) {
        IPropertyContainer childResult = childTagWorker.getElementResult();

        // 如果子元素是Svg (来自 MjxSvgTagWorker)
        if (childResult instanceof SvgImage) {
            log.info("MjxContainer: Processing Inline Image into Span.");

            // 【关键】：直接调用父类方法
            // SpanTagWorker 会自动把这个 Image 添加到它内部的 Span 里
            // 效果等同于 HTML 结构：<span><img /></span>
            return super.processTagChild(childTagWorker, context);
        }

        // 对于其他类型的子元素，也交给父类处理
        return super.processTagChild(childTagWorker, context);
    }

    // 不需要重写 getElementResult()
    // 父类 SpanTagWorker 会自动返回那个包含了 Image 的 Span 对象

    // 不需要重写 processEnd()，父类有默认实现
}