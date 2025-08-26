package com.ririv.quickoutline.pdfProcess.itextImpl.contentExtractor;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.Message;
import com.ririv.quickoutline.model.Bookmark;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.LineWithMetadata;
import com.ririv.quickoutline.pdfProcess.itextImpl.model.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OpenAITocExtractorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OpenAIClient mockOpenAIClient;

    private OpenAITocExtractor extractor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = new OpenAITocExtractor("test-api-key");
        try {
            java.lang.reflect.Field field = extractor.getClass().getDeclaredField("openAI");
            field.setAccessible(true);
            field.set(extractor, mockOpenAIClient);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject mock client", e);
        }
    }

    @Test
    void testExtract() {
        // 1. Arrange: Set up the mock response
        String mockApiResponse = "1,Introduction,1\n" +
                                 "2,Chapter 1,5\n" +
                                 "2,Chapter 2,15";

        // Correct way to build the mock response with the new API
        ChatCompletion mockChatCompletion = ChatCompletion.builder()
                .id("chatcmpl-123")
                .addChoice(Message.fromAssistant(mockApiResponse))
                .model("gpt-3.5-turbo")
                .build();

        when(mockOpenAIClient.chat().completions().create(any(ChatCompletionCreateParams.class))).thenReturn(mockChatCompletion);

        // 2. Act: Call the method to be tested
        List<LineWithMetadata> dummyLines = List.of(
                new LineWithMetadata("dummy text", 0f, 0f, 0f, new Style("font", 12), 1, null, 0.0, List.of())
        );
        List<Bookmark> bookmarks = extractor.extract(dummyLines);

        // 3. Assert: Verify the result
        assertNotNull(bookmarks);
        assertEquals(3, bookmarks.size());

        assertEquals(1, bookmarks.get(0).getLevel());
        assertEquals("Introduction", bookmarks.get(0).getTitle());
        assertEquals(Optional.of(1), bookmarks.get(0).getOffsetPageNum());

        assertEquals(2, bookmarks.get(1).getLevel());
        assertEquals("Chapter 1", bookmarks.get(1).getTitle());
        assertEquals(Optional.of(5), bookmarks.get(1).getOffsetPageNum());

        assertEquals(2, bookmarks.get(2).getLevel());
        assertEquals("Chapter 2", bookmarks.get(2).getTitle());
        assertEquals(Optional.of(15), bookmarks.get(2).getOffsetPageNum());
    }
}