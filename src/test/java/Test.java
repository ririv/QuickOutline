import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfReader;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.OutlineProcessor;
import com.ririv.quickoutline.pdfProcess.ViewScaleType;
import com.ririv.quickoutline.pdfProcess.itextImpl.ItextOutlineProcessor;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.textProcess.methods.Method;

import java.io.IOException;


public class Test {
    final String text = """
彩图  4
第1版序言  11
Part I  监督学习  25
	第1章  统计学习及监督学习概论  26
		1.1  统计学习  26
		1.2  统计学习的分类  27
			1.2.3  按算法分类  36
			1.2.4  按技巧分类  36
		1.3  统计学习方法三要素  38
			1.3.1  模型  38

		习题  56
		参考文献  57
	第2章  感知机  58
		2.1  感知机模型  58
		2.2  感知机学习策略  59
			2.2.1  数据集的线性可分性  59
			2.2.2  感知机学习策略  60
		2.3  感知机学习算法  61
			2.3.1  感知机学习算法的原始形式  61
			2.3.2  算法的收敛性  64
			2.3.3  感知机学习算法的对偶形式  66
		习题  69

附录 A  梯度下降法  452
附录 B  牛顿法和拟牛顿法  454

            """;

//    final String srcFilePath = "D:/a b/Probabilistic Graphical Models_ Principles and Applications.pdf";
    final String srcFilePath = "D:/a b/Probabilistic Graphical Models_ Principles and Applications.pdf";
    final Method method = Method.SEQ;

    PdfOutlineService pdfOutlineService = new PdfOutlineService();
    final Bookmark rootBookmark = pdfOutlineService.convertTextToBookmarkTreeByMethod(text, 2, method);

    @org.junit.jupiter.api.Test
    void test1() throws IOException {
        OutlineProcessor outlineProcessor = new ItextOutlineProcessor();
        outlineProcessor.setContents(rootBookmark, srcFilePath,"D:/gen.pdf", ViewScaleType.ACTUAL_SIZE);
    }

    @org.junit.jupiter.api.Test
    void test2() throws IOException {
        PdfDocument srcDoc = new PdfDocument(new PdfReader(srcFilePath));

        PdfOutline rootOutline = srcDoc.getOutlines(false);
    }


}
