package com.ririv.quickoutline.textProcess.methods.seq;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;


// From ChatGPT
public class EnSeq {

    // 定义可以作为章节前缀的关键字
    private final List<String> keywords = Arrays.asList("Chapter", "Section", "Part", "Appendix", "Label");

    /**
     * 解析目录行列表
     *
     * @param lines 输入的目录行列表
     * @return 解析结果列表，每项为一个包含缩进、章节序号、章节标题和页码的 Map
     */
    public List<Map<String, String>> parseTableOfContents(List<String> lines) {
        List<Map<String, String>> result = new ArrayList<>();
        for (String line : lines) {
            Map<String, String> parsedLine = parseSingleLine(line);
            if (parsedLine != null) {
                result.add(parsedLine);
            }
        }
        return result;
    }

    /**
     * 解析单行目录
     *
     * @param line 单行目录
     * @return 解析结果 Map，包括缩进、章节序号、章节标题和页码，如果无法解析则返回 null
     */
    private Map<String, String> parseSingleLine(String line) {
        String trimmedLine = line.trim(); // 去掉行首尾的空白字符
        int pageNumberIndex = findPageNumberIndex(trimmedLine);

        if (pageNumberIndex != -1) {
            String contentPart = trimmedLine.substring(0, pageNumberIndex).trim(); // 去掉页码部分
            String pageNumber = trimmedLine.substring(pageNumberIndex).trim(); // 页码部分
            String indentation = getIndentation(line);
            String[] chapterInfo = extractChapterInfo(contentPart);

            if (chapterInfo != null) {
                Map<String, String> parsedLine = new HashMap<>();
                parsedLine.put("indentation", indentation);
                parsedLine.put("chapterNumber", chapterInfo[0]);
                parsedLine.put("chapterTitle", chapterInfo[1]);
                parsedLine.put("pageNumber", pageNumber);
                return parsedLine;
            }
        }
        return null; // 无法解析
    }

    // 找到页码部分的起始位置（假设页码部分是以数字结尾）
    private int findPageNumberIndex(String line) {
        for (int i = line.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(line.charAt(i))) {
                return i + 1;
            }
        }
        return -1; // 未找到页码
    }

    // 提取章节信息：章节编号和标题
    private String[] extractChapterInfo(String content) {
        for (String keyword : keywords) {
            if (content.startsWith(keyword)) {
                int keywordEndIndex = keyword.length();
                String remaining = content.substring(keywordEndIndex).trim(); // 去掉关键词部分

                int firstSpace = remaining.indexOf(' '); // 寻找编号和标题之间的空格
                if (firstSpace != -1) {
                    String chapterNumber = keyword + " " + remaining.substring(0, firstSpace); // 章节编号部分
                    String chapterTitle = remaining.substring(firstSpace).trim(); // 章节标题部分
                    return new String[]{chapterNumber, chapterTitle};
                } else {
                    return new String[]{keyword, remaining}; // 没有标题时，只返回编号
                }
            }
        }

        // 处理没有明确关键词的情况，如 "16.4.2 Testing Bayesian Networks"
        int firstSpace = content.indexOf(' ');
        if (firstSpace != -1) {
            String chapterNumber = content.substring(0, firstSpace); // 假设数字编号在标题前
            String chapterTitle = content.substring(firstSpace).trim(); // 剩余部分为标题
            return new String[]{chapterNumber, chapterTitle};
        }

        return null; // 无法解析
    }

    // 获取行首的缩进空白字符
    private String getIndentation(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(0, i);
    }

    public static void main(String[] args) {
        EnSeq parser = new EnSeq();

        // 示例目录行，可以根据需要添加更多行
        List<String> lines = Arrays.asList(
                "Foreword 7",
                "Preface 9",
                "Acknowledgements 14",
                "Contents 16",
                "    Chapter 1 Introduction 28",
                "        Label 1.1 Uncertainty 28",
                "            Label 1.1.1 Effects of Uncertainty 29",
                "    Section 2 Advanced Topics 300",
                "    Part IV Fundamentals 27",
                "    Appendix A A Python Library for Inference and Learning 362",
                "        A.1 Introduction 362",
                "        16.4.2 Testing Bayesian Networks 350",
                "    Appendix Glossary 364",
                "        B.1 Terms 365"
        );

        // 调用解析方法
        List<Map<String, String>> parsedLines = parser.parseTableOfContents(lines);

//        try {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));


            // 打印解析结果
        for (Map<String, String> line : parsedLines) {
            System.out.println("缩进: [" + line.get("indentation") + "], 章节序号: [" + line.get("chapterNumber") + "], 章节标题: [" + line.get("chapterTitle") + "], 页码: [" + line.get("pageNumber") + "]");
        }
    }
}
