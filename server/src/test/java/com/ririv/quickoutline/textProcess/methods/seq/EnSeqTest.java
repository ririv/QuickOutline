package com.ririv.quickoutline.textProcess.methods.seq;

import com.ririv.quickoutline.model.Bookmark;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

class EnSeqTest {

    @org.junit.jupiter.api.Test
    void parseTest() {
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


        List<Bookmark> parsedLines = parser.parse(lines);

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));


        // 打印解析结果
        for (Bookmark bookmark : parsedLines) {

            System.out.println(bookmark);
        }
    }

}