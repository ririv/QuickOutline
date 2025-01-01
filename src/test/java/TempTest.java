import com.itextpdf.kernel.pdf.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TempTest {

    String path1 = "D:/Probabilistic Graphical Models_ Principles and Applications.pdf";

    @Test
    public void test1() {
        try {


            PdfDocument doc = new PdfDocument(new PdfReader(path1));
            PdfOutline rootOutline = doc.getOutlines(false);
//            root.removeOutline();

            String a = rootOutline.getTitle();
//            System.out.println(a+doc.getPageNumber(rootOutline.getContent()));
            List<PdfOutline> outlines = rootOutline.getAllChildren();
            var nameTree = doc.getCatalog().getNameTree(PdfName.Dests);
//            List<Bookmark> bookmarks = new ArrayList<>();
            System.out.println(a);


            for (PdfOutline child : outlines) {
                if (child.getDestination() != null) {
//             Note: 这里返回类型为PdfObject，但调用PdfObject.getType()发现返回为3，查看源码发现3对应Dictionary，因此可以放心将其强制转换为PdfDictionary
//             names参数是负责解决指定目的地的参数，是正确获取页码所必需的，因为PDF可能包含明确的和命名的目的地，要获取参照上面
                    String title = child.getTitle();
                    int pageNum = doc.getPageNumber((PdfDictionary) child.getDestination().getDestinationPage(nameTree));
//                    int pageNum = doc.getPageNumber(child.getContent());

                    System.out.println(title + "  " + pageNum);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    void test2() {
        PdfDocument doc;
        try {
            doc = new PdfDocument(new PdfReader(path1),new PdfWriter("D:/test2.pdf"));

            PdfOutline rootOutline = doc.getOutlines(false);
            rootOutline.removeOutline();
            rootOutline.getAllChildren().clear();
            PdfOutline current1 = rootOutline.addOutline("123");
            PdfOutline current2 = current1.addOutline("234");
            PdfOutline current3 = current2.addOutline("345");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void test5(){
        //  TO CHECK WHETHER A FILE IS OPENED
        //  OR NOT (not for .txt files)

        //  the file we want to check
        String fileName = "D:\\2.txt";
        File file = new File(fileName);

        // try to rename the file with the same name
        File sameFileName = new File("D:\\123.txt");

        if(file.renameTo(sameFileName)){
            // if the file is renamed
            System.out.println("file is closed");
        }else{
            // if the file didnt accept the renaming operation
            System.out.println("file is opened");
        }
    }
}
