package com.kaiki.minechatcorrect.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryManagerArchiveImportTest {
    @TempDir
    Path tempDir;

    @Test
    void importsZipWordList() throws Exception {
        Path zip = tempDir.resolve("words.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("nested/words.txt"));
            output.write("zippedword\narchiveword\n".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        DictionaryManager manager = new DictionaryManager(tempDir.resolve("config"));
        manager.importDictionary(zip.toString());

        assertTrue(manager.allWords().contains("zippedword"));
        assertTrue(manager.allWords().contains("archiveword"));
    }

    @Test
    void rejectsZipPathTraversalAndCleansFailedImport() throws Exception {
        Path zip = tempDir.resolve("evil.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("../evil.txt"));
            output.write("evilword\n".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        Path configDir = tempDir.resolve("config");
        DictionaryManager manager = new DictionaryManager(configDir);

        assertThrows(Exception.class, () -> manager.importDictionary(zip.toString()));
        assertTrue(importsDirMissingOrEmpty(configDir));
    }

    @Test
    void importsGzipWordList() throws Exception {
        Path gzip = tempDir.resolve("words.txt.gz");
        try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(gzip))) {
            output.write("gzipword\n".getBytes(StandardCharsets.UTF_8));
        }

        DictionaryManager manager = new DictionaryManager(tempDir.resolve("config"));
        manager.importDictionary(gzip.toString());

        assertTrue(manager.allWords().contains("gzipword"));
    }

    @Test
    void importsTarGzWordList() throws Exception {
        Path tarGz = tempDir.resolve("words.tar.gz");
        byte[] tar = tarEntry("words.txt", "tarword\n".getBytes(StandardCharsets.UTF_8));

        try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(tarGz))) {
            output.write(tar);
        }

        DictionaryManager manager = new DictionaryManager(tempDir.resolve("config"));
        manager.importDictionary(tarGz.toString());

        assertTrue(manager.allWords().contains("tarword"));
    }

    @Test
    void rejectsTarGzPathTraversal() throws Exception {
        Path tarGz = tempDir.resolve("evil.tar.gz");
        byte[] tar = tarEntry("../evil.txt", "evilword\n".getBytes(StandardCharsets.UTF_8));

        try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(tarGz))) {
            output.write(tar);
        }

        DictionaryManager manager = new DictionaryManager(tempDir.resolve("config"));

        assertThrows(Exception.class, () -> manager.importDictionary(tarGz.toString()));
    }

    private boolean importsDirMissingOrEmpty(Path configDir) throws Exception {
        Path importsDir = configDir.resolve("dictionaries").resolve("imports");
        if (!Files.exists(importsDir)) {
            return true;
        }

        try (var stream = Files.list(importsDir)) {
            return stream.findAny().isEmpty();
        }
    }

    private byte[] tarEntry(String name, byte[] content) throws Exception {
        ByteArrayOutputStream tar = new ByteArrayOutputStream();
        byte[] header = new byte[512];

        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
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
        byte[] magic = "ustar".getBytes(StandardCharsets.US_ASCII);
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
        tar.write(new byte[1024]);

        return tar.toByteArray();
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
}
