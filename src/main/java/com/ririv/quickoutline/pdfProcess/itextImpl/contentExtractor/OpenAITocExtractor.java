package com.ririv.quickoutline.pdfProcess.itextImpl.contentExtractor;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.LineWithMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OpenAITocExtractor {

    private final OpenAIClient openAI;
    private static final String MODEL_NAME = ChatModel.GPT_3_5_TURBO.toString();

    public OpenAITocExtractor(String apiKey) {
        this.openAI = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public List<Bookmark> extract(List<LineWithMetadata> lines) {
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }

        String textContent = lines.stream()
                                  .map(LineWithMetadata::getTextContent)
                                  .collect(Collectors.joining("\n"));

        String prompt = buildPrompt(textContent);

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(MODEL_NAME)
                    .addUserMessage(prompt)
                    .build();

            ChatCompletion response = openAI.chat().completions().create(params);

            if (response != null && !response.choices().isEmpty()) {
                Optional<String> rawResultOpt = response.choices().get(0).message().content();
                if (rawResultOpt.isPresent()) {
                    return parseResponseToBookmarks(rawResultOpt.get());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    private String buildPrompt(String textContent) {
        int maxTokens = 4000; 
        if (textContent.length() > maxTokens) {
            textContent = textContent.substring(0, maxTokens);
        }
        
        return "Please extract the table of contents from the following text. "
                + "Format each entry as 'level,title,page_number'. For example: '1,Introduction,1'. "
                + "Do not include anything else in your response. Here is the text:\n\n" + textContent;
    }

    private List<Bookmark> parseResponseToBookmarks(String response) {
        List<Bookmark> bookmarks = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\s*(\\d+)\\s*,\\s*(.*?)\\s*,\\s*(\\d+)\\s*");

        for (String line : response.split("\n")) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.matches()) {
                try {
                    int level = Integer.parseInt(matcher.group(1));
                    String title = matcher.group(2).trim();
                    int pageNum = Integer.parseInt(matcher.group(3));
                    bookmarks.add(new Bookmark(title, pageNum, level));
                } catch (NumberFormatException e) {
                    // Ignore lines that are not correctly formatted
                }
            }
        }
        return bookmarks;
    }
}
