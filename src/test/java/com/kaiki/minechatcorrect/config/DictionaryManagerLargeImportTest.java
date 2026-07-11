package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DictionaryManagerLargeImportTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsOversizedLocalDictionarySourceAndDoesNotCreateImportDirectory() throws Exception {
        Path source = tempDir.resolve("large.words");
        Files.write(source, new byte[(int) DictionaryImportLimits.MAX_SOURCE_BYTES + 1]);

        Path configDir = tempDir.resolve("config");
        DictionaryManager manager = new DictionaryManager(configDir);

        assertThrows(Exception.class, () -> manager.importDictionary(source.toString()));
        assertEquals(0, importDirectoryCount(configDir));
    }

    @Test
    void rejectsOversizedZipEntryAndCleansImportDirectory() throws Exception {
        Path zip = tempDir.resolve("large.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("large.words"));
            output.write(new byte[(int) DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES + 1]);
            output.closeEntry();
        }

        Path configDir = tempDir.resolve("config");
        DictionaryManager manager = new DictionaryManager(configDir);

        assertThrows(Exception.class, () -> manager.importDictionary(zip.toString()));
        assertEquals(0, importDirectoryCount(configDir));
    }

    @Test
    void rejectsOversizedGzipOutputAndCleansImportDirectory() throws Exception {
        Path gzip = tempDir.resolve("large.words.gz");
        try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(gzip))) {
            output.write(new byte[(int) DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES + 1]);
        }

        Path configDir = tempDir.resolve("config");
        DictionaryManager manager = new DictionaryManager(configDir);

        assertThrows(Exception.class, () -> manager.importDictionary(gzip.toString()));
        assertEquals(0, importDirectoryCount(configDir));
    }

    @Test
    void rejectsTarGzWhenTotalExtractedSizeExceedsLimit() throws Exception {
        System.setProperty("minechatcorrect.test.maxEntryBytes", "100");
        System.setProperty("minechatcorrect.test.maxTotalExtractedBytes", "150");
        try {
            Path tarGz = tempDir.resolve("total-too-large.tar.gz");
            byte[] tar = twoEntryTar();

            try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(tarGz))) {
                output.write(tar);
            }

            Path configDir = tempDir.resolve("config");
            DictionaryManager manager = new DictionaryManager(configDir);

            assertThrows(Exception.class, () -> manager.importDictionary(tarGz.toString()));
            assertEquals(0, importDirectoryCount(configDir));
        } finally {
            System.clearProperty("minechatcorrect.test.maxEntryBytes");
            System.clearProperty("minechatcorrect.test.maxTotalExtractedBytes");
        }
    }

    private byte[] twoEntryTar() throws Exception {
        java.io.ByteArrayOutputStream tar = new java.io.ByteArrayOutputStream();
        appendTarEntry(tar, "one.words", "a".repeat(90).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        appendTarEntry(tar, "two.words", "b".repeat(90).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        tar.write(new byte[1024]);
        return tar.toByteArray();
    }

    private void appendTarEntry(java.io.ByteArrayOutputStream tar, String name, byte[] content) throws Exception {
        byte[] header = new byte[512];
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 100));
        writeOctal(header, 100, 8, 0644);
        writeOctal(header, 108, 8, 0);
        writeOctal(header, 116, 8, 0);
        writeOctal(header, 124, 12, content.length);
        writeOctal(header, 136, 12, 0);
        for (int i = 148; i < 156; i++) {
            header[i] = 32;
        }
        header[156] = '0';
        byte[] magic = "ustar".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, header, 257, magic.length);

        long checksum = 0;
        for (byte b : header) {
            checksum += Byte.toUnsignedInt(b);
        }
        writeOctal(header, 148, 8, checksum);

        tar.write(header);
        tar.write(content);
        int padding = (int) ((512 - (content.length % 512)) % 512);
        tar.write(new byte[padding]);
    }

    private void writeOctal(byte[] header, int offset, int length, long value) {
        String octal = Long.toOctalString(value);
        int start = offset + length - octal.length() - 1;
        for (int i = offset; i < offset + length; i++) {
            header[i] = 0;
        }
        for (int i = 0; i < octal.length(); i++) {
            header[start + i] = (byte) octal.charAt(i);
        }
        header[offset + length - 1] = 0;
    }

    private long importDirectoryCount(Path configDir) throws Exception {
        Path importsDir = configDir.resolve("dictionaries").resolve("imports");
        if (!Files.exists(importsDir)) {
            return 0;
        }

        try (var stream = Files.list(importsDir)) {
            return stream.filter(Files::isDirectory).count();
        }
    }
}
