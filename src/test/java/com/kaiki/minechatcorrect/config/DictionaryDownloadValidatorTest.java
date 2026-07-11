package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryDownloadValidatorTest {
    @Test
    void acceptsNonEmptyNonHtmlPayloads() {
        assertDoesNotThrow(() -> DictionaryDownloadValidator.validateDownloadedContent("word\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsEmptyAndHtmlPayloads() {
        assertThrows(IOException.class, () -> DictionaryDownloadValidator.validateDownloadedContent(new byte[0]));

        IOException exception = assertThrows(IOException.class, () ->
                DictionaryDownloadValidator.validateDownloadedContent("<!doctype html><html></html>".getBytes(StandardCharsets.UTF_8)));

        assertTrue(exception.getMessage().contains("HTML"));
    }

    @Test
    void rejectsTooManyRedirects() {
        assertDoesNotThrow(() -> DictionaryDownloadValidator.validateRedirectCount(5, 5));
        assertThrows(IOException.class, () -> DictionaryDownloadValidator.validateRedirectCount(6, 5));
    }
}
