package com.kaiki.minechatcorrect.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DictionaryManager {
    private static final Logger LOGGER = Logger.getLogger(DictionaryManager.class.getName());

    public static final String DEFAULT_DICTIONARY_URL = "https://github.com/en-wl/wordlist/releases/download/rel-2026.02.25/aspell6-en-2026.02.25-0.tar.bz2";

    private static final int MAX_DOWNLOAD_REDIRECTS = 5;

    private final Path configDir;
    private final Path extraWordsFile;
    private final Path dictionaryDir;
    private final Path importDir;

    private final Set<String> builtInWords = new LinkedHashSet<>();
    private final Set<String> extraWords = new LinkedHashSet<>();
    private final List<ExternalDictionary> dictionaries = new ArrayList<>();

    public DictionaryManager(Path configDir) {
        this.configDir = configDir;
        this.extraWordsFile = configDir.resolve("additional_words.txt");
        this.dictionaryDir = configDir.resolve("dictionaries");
        this.importDir = dictionaryDir.resolve("imports");

        loadBuiltInWords();
        loadMinecraftWords();
        load();
    }

    public Set<String> allWords() {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        words.addAll(builtInWords);
        words.addAll(extraWords);

        for (ExternalDictionary dictionary : dictionaries) {
            if (dictionary.enabled()) {
                words.addAll(dictionary.words());
            }
        }

        return words;
    }

    public List<String> extraWords() {
        return new ArrayList<>(extraWords);
    }

    public List<ExternalDictionary> dictionaries() {
        return new ArrayList<>(dictionaries);
    }

    public void addExtraWord(String word) {
        if (AcceptedWordsStore.add(extraWords, word)) {
            saveExtraWords();
        }
    }

    public void removeExtraWord(String word) {
        if (AcceptedWordsStore.remove(extraWords, word)) {
            saveExtraWords();
        }
    }

    public void replaceExtraWord(String oldWord, String newWord) {
        if (AcceptedWordsStore.replace(extraWords, oldWord, newWord)) {
            saveExtraWords();
        }
    }

    public Path localImportDropDir() throws IOException {
        Path dropDir = dictionaryDir.resolve("drop");
        Files.createDirectories(dropDir);
        return dropDir;
    }


    public String importDictionary(String source) throws IOException {
        Files.createDirectories(dictionaryDir);
        Files.createDirectories(importDir);

        String trimmed = source.trim();
        if (trimmed.isBlank()) {
            throw new IOException("Dictionary source is blank.");
        }

        String sourceName = sourceName(trimmed);
        Path importTarget = importDir.resolve(System.currentTimeMillis() + "-" + safeFileName(sourceName));
        Files.createDirectories(importTarget);

        try {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                byte[] content = downloadBytes(trimmed);
                importBytes(sourceName, content, importTarget);
            } else {
                Path sourcePath = Path.of(trimmed);
                if (Files.isDirectory(sourcePath)) {
                    copyDirectory(sourcePath, importTarget);
                } else {
                    importBytes(sourcePath.getFileName().toString(), DictionaryImportLimits.readFile(sourcePath), importTarget);
                }
            }

            ExternalDictionary dictionary = loadDictionaryFromDirectory(importTarget);
            if (dictionary == null || dictionary.words().isEmpty()) {
                throw new IOException("No usable dictionary words found. Try a .dic, word-list, .zip, .tar.gz, or .tar.bz2 StarDict archive.");
            }

            DictionaryMetadataStore.writeEnabled(dictionary.path(), dictionary.enabled());
            dictionaries.add(dictionary);
            return dictionary.name();
        } catch (IOException | RuntimeException exception) {
            deleteRecursively(importTarget);
            throw exception;
        }
    }

    public void setDictionaryEnabled(String name, boolean enabled) {
        for (int i = 0; i < dictionaries.size(); i++) {
            ExternalDictionary dictionary = dictionaries.get(i);
            if (dictionary.name().equals(name)) {
                ExternalDictionary updated = new ExternalDictionary(dictionary.name(), dictionary.path(), enabled, dictionary.words());
                dictionaries.set(i, updated);
                try {
                    DictionaryMetadataStore.writeEnabled(updated.path(), enabled);
                } catch (IOException exception) {
                    LOGGER.log(Level.WARNING, "Could not save dictionary enabled state for " + updated.path(), exception);
                }
                return;
            }
        }
    }

    public void removeDictionary(String name) {
        dictionaries.removeIf(dictionary -> {
            if (dictionary.name().equals(name)) {
                deleteRecursively(dictionary.path());
                return true;
            }
            return false;
        });
    }

    public void reload() {
        extraWords.clear();
        dictionaries.clear();
        load();
    }

    private void load() {
        try {
            Files.createDirectories(configDir);
            Files.createDirectories(dictionaryDir);
            Files.createDirectories(importDir);

            if (!Files.exists(extraWordsFile)) {
                AcceptedWordsStore.write(extraWordsFile, builtInMinecraftWords());
            }

            extraWords.addAll(AcceptedWordsStore.read(extraWordsFile));

            try (Stream<Path> stream = Files.list(importDir)) {
                stream.filter(Files::isDirectory).forEach(path -> {
                    try {
                        ExternalDictionary dictionary = loadDictionaryFromDirectory(path);
                        if (dictionary != null && !dictionary.words().isEmpty()) {
                            dictionaries.add(dictionary);
                        }
                    } catch (IOException exception) {
                        LOGGER.log(Level.WARNING, "Could not load imported dictionary from " + path, exception);
                    }
                });
            }

            try (Stream<Path> stream = Files.list(dictionaryDir)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        String content = readTextFallback(path);
                        dictionaries.add(new ExternalDictionary(path.getFileName().toString(), path, true, DictionaryWordParser.parseDictionaryWords(content)));
                    } catch (IOException exception) {
                        LOGGER.log(Level.WARNING, "Could not load dictionary file from " + path, exception);
                    }
                });
            }
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not load Mine-ChatCorrect dictionaries.", exception);
        }
    }

    private void saveExtraWords() {
        try {
            AcceptedWordsStore.write(extraWordsFile, extraWords);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not save Mine-ChatCorrect accepted words.", exception);
        }
    }

    private byte[] downloadBytes(String source) throws IOException {
        return downloadBytes(source, 0);
    }

    private byte[] downloadBytes(String source, int redirects) throws IOException {
        DictionaryDownloadValidator.validateRedirectCount(redirects, MAX_DOWNLOAD_REDIRECTS);

        HttpURLConnection connection = (HttpURLConnection) URI.create(source).toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "Mine-ChatCorrect/0.1.3 Minecraft");
        connection.setRequestProperty("Accept", "*/*");

        int status = connection.getResponseCode();
        if (status >= 300 && status < 400) {
            String location = connection.getHeaderField("Location");
            if (location != null && !location.isBlank()) {
                return downloadBytes(URI.create(source).resolve(location).toString(), redirects + 1);
            }
            throw new IOException("Download failed. Redirect response did not include a Location header.");
        }

        if (status < 200 || status >= 300) {
            throw new IOException("Download failed. HTTP status: " + status);
        }

        byte[] content = DictionaryImportLimits.readStream(connection.getInputStream(), DictionaryImportLimits.MAX_SOURCE_BYTES, "Downloaded dictionary");
        DictionaryDownloadValidator.validateDownloadedContent(content);

        return content;
    }

    private void importBytes(String name, byte[] content, Path targetDir) throws IOException {
        String lower = name.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".zip")) {
            extractZip(content, targetDir);
        } else if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
            extractTar(gunzip(content, DictionaryImportLimits.MAX_TOTAL_EXTRACTED_BYTES, "Gzip tar archive content"), targetDir);
        } else if (lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") || lower.endsWith(".tbz")) {
            extractTarBz2ViaSystemTar(name, content, targetDir);
        } else if (lower.endsWith(".gz") || lower.endsWith(".dz")) {
            String uncompressedName = name.substring(0, name.lastIndexOf('.'));
            byte[] uncompressed = gunzip(content, DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES, "Gzip dictionary content");
            DictionaryImportLimits.validateEntrySize(uncompressed.length, uncompressedName);
            Files.write(safeResolve(targetDir, safeFileName(uncompressedName)), uncompressed);
        } else {
            Files.write(safeResolve(targetDir, safeFileName(name)), content);
        }
    }

    private ExternalDictionary loadDictionaryFromDirectory(Path directory) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(directory)) {
            files = stream.filter(Files::isRegularFile).toList();
        }

        LinkedHashSet<String> words = new LinkedHashSet<>();
        String name = directory.getFileName().toString();

        for (Path file : files) {
            String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (DictionaryMetadataStore.isMetadataFile(file)) {
                continue;
            }

            if (lower.endsWith(".ifo")) {
                for (String line : readTextFallback(file).split("\\R")) {
                    if (line.startsWith("bookname=")) {
                        name = line.substring("bookname=".length()).trim();
                    }
                }
            }
        }

        for (Path file : files) {
            String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (DictionaryMetadataStore.isMetadataFile(file)) {
                continue;
            }

            if (lower.endsWith(".idx")) {
                words.addAll(parseStarDictIndex(DictionaryImportLimits.readFile(file)));
            } else if (lower.endsWith(".idx.gz")) {
                words.addAll(parseStarDictIndex(gunzip(DictionaryImportLimits.readFile(file), DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES, "Gzip StarDict index content")));
            } else if (lower.endsWith(".index")) {
                words.addAll(parseDictIndex(readTextFallback(file)));
            } else if (lower.endsWith(".index.gz")) {
                String content = new String(gunzip(DictionaryImportLimits.readFile(file), DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES, "Gzip DICT index content"), StandardCharsets.UTF_8);
                words.addAll(parseDictIndex(content));
            } else if (lower.endsWith(".dic") || lower.endsWith(".txt") || lower.endsWith(".words")) {
                words.addAll(DictionaryWordParser.parseDictionaryWords(readTextFallback(file)));
            }
        }

        return new ExternalDictionary(name, directory, DictionaryMetadataStore.readEnabled(directory), words);
    }

    private Set<String> parseStarDictIndex(byte[] bytes) {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        int index = 0;

        while (index < bytes.length) {
            int start = index;
            while (index < bytes.length && bytes[index] != 0) {
                index++;
            }

            if (index >= bytes.length) {
                break;
            }

            String word;
            try {
                word = new String(bytes, start, index - start, StandardCharsets.UTF_8);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.FINE, "Could not decode StarDict index word as UTF-8; falling back to ISO-8859-1.", exception);
                word = new String(bytes, start, index - start, Charset.forName("ISO-8859-1"));
            }
            String normalized = DictionaryWordParser.normalizeDictionaryWord(word);
            if (!normalized.isBlank()) {
                words.add(normalized);
            }

            index++;
            index += 8;
        }

        return words;
    }

    private Set<String> parseDictIndex(String content) {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return words;
        }

        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            String headword = trimmed.split("\\t", 2)[0];
            String normalized = DictionaryWordParser.normalizeDictionaryWord(headword);
            if (!normalized.isBlank()) {
                words.add(normalized);
            }
        }

        return words;
    }

    private String readTextFallback(Path path) throws IOException {
        byte[] bytes = DictionaryImportLimits.readFile(path);

        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.FINE, "Could not decode dictionary text as UTF-8; falling back to ISO-8859-1.", exception);
            return new String(bytes, Charset.forName("ISO-8859-1"));
        }
    }

    private byte[] gunzip(byte[] bytes, long maxBytes, String description) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return DictionaryImportLimits.readStream(input, maxBytes, description);
        }
    }

    private void extractZip(byte[] bytes, Path targetDir) throws IOException {
        long extractedBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    DictionaryImportLimits.validateEntrySize(entry.getSize(), entry.getName());
                    Path target = safeResolve(targetDir, entry.getName());
                    byte[] entryBytes = DictionaryImportLimits.readStream(zip, DictionaryImportLimits.MAX_EXTRACTED_ENTRY_BYTES, "Archive entry " + entry.getName());
                    extractedBytes = DictionaryImportLimits.addExtractedBytes(extractedBytes, entryBytes.length);
                    Files.createDirectories(target.getParent());
                    Files.write(target, entryBytes);
                }
                entry = zip.getNextEntry();
            }
        }
    }

    private void extractTar(byte[] bytes, Path targetDir) throws IOException {
        int index = 0;
        long extractedBytes = 0;

        while (index + 512 <= bytes.length) {
            byte[] header = java.util.Arrays.copyOfRange(bytes, index, index + 512);
            index += 512;

            if (isEmptyBlock(header)) {
                break;
            }

            String name = tarString(header, 0, 100);
            String prefix = tarString(header, 345, 155);
            if (!prefix.isBlank()) {
                name = prefix + "/" + name;
            }

            long size = tarOctal(header, 124, 12);
            byte type = header[156];

            DictionaryImportLimits.validateEntrySize(size, name);
            if (index + size > bytes.length) {
                throw new IOException("Truncated tar archive entry: " + name);
            }

            if (type != '5' && type != 'L' && type != 'K' && !name.isBlank()) {
                Path target = safeResolve(targetDir, name);
                Files.createDirectories(target.getParent());
                byte[] entryBytes = java.util.Arrays.copyOfRange(bytes, index, (int) (index + size));
                extractedBytes = DictionaryImportLimits.addExtractedBytes(extractedBytes, entryBytes.length);
                Files.write(target, entryBytes);
            }

            long paddedSize = ((size + 511) / 512) * 512;
            if (index + paddedSize > bytes.length) {
                throw new IOException("Truncated tar archive after entry: " + name);
            }
            index += (int) paddedSize;
        }
    }

    private void extractTarBz2ViaSystemTar(String name, byte[] content, Path targetDir) throws IOException {
        Path tempDir = Files.createTempDirectory("mine-chatcorrect-bzip2-");
        try {
            Path archive = tempDir.resolve(safeFileName(name));
            Files.write(archive, content);

            String tarCommand = findTarCommand();
            validateSystemTarArchiveEntries(tarCommand, archive, targetDir);

            Process process = new ProcessBuilder(tarCommand, "-xjf", archive.toString(), "-C", targetDir.toString())
                    .redirectErrorStream(true)
                    .start();

            ByteArrayOutputStream output = readProcessOutput(process, "System tar extraction");

            try {
                int exit = process.waitFor();
                if (exit != 0) {
                    String message = output.toString(StandardCharsets.UTF_8);
                    throw new IOException("System tar could not extract .tar.bz2 archive. Exit status: " + exit + ". " + message);
                }

                validateExtractedDirectory(targetDir);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while extracting .tar.bz2 archive.", exception);
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private void validateSystemTarArchiveEntries(String tarCommand, Path archive, Path targetDir) throws IOException {
        Process process = new ProcessBuilder(tarCommand, "-tjf", archive.toString())
                .redirectErrorStream(true)
                .start();

        ByteArrayOutputStream output = readProcessOutput(process, "System tar listing");

        try {
            int exit = process.waitFor();
            String listing = output.toString(StandardCharsets.UTF_8);
            if (exit != 0) {
                throw new IOException("System tar could not list .tar.bz2 archive. Exit status: " + exit + ". " + listing);
            }

            for (String entry : listing.split("\\R")) {
                if (!entry.isBlank()) {
                    safeResolve(targetDir, entry);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while validating .tar.bz2 archive.", exception);
        }
    }

    private ByteArrayOutputStream readProcessOutput(Process process, String description) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = process.getInputStream().read(buffer)) != -1) {
            output.write(buffer, 0, read);
            if (output.size() > 64_000) {
                throw new IOException(description + " produced too much diagnostic output.");
            }
        }
        return output;
    }

    private void validateExtractedDirectory(Path directory) throws IOException {
        long extractedBytes = 0;
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                long size = Files.size(path);
                DictionaryImportLimits.validateEntrySize(size, path.toString());
                extractedBytes = DictionaryImportLimits.addExtractedBytes(extractedBytes, size);
            }
        }
    }

    private String findTarCommand() {
        String[] candidates = {"tar", "/usr/bin/tar", "/bin/tar"};
        for (String candidate : candidates) {
            try {
                Process process = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true)
                        .start();
                int exit = process.waitFor();
                if (exit == 0) {
                    return candidate;
                }
            } catch (IOException | InterruptedException exception) {
                LOGGER.log(Level.FINE, "Could not run tar candidate: " + candidate, exception);
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return "tar";
    }

    private boolean isEmptyBlock(byte[] header) {
        for (byte b : header) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private String tarString(byte[] header, int offset, int length) {
        int end = offset;
        while (end < offset + length && end < header.length && header[end] != 0) {
            end++;
        }

        return new String(header, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private long tarOctal(byte[] header, int offset, int length) {
        int limit = Math.min(header.length, offset + length);
        StringBuilder value = new StringBuilder();

        for (int index = offset; index < limit; index++) {
            byte current = header[index];
            if (current >= '0' && current <= '7') {
                value.append((char) current);
            }
        }

        if (value.isEmpty()) {
            return 0;
        }

        return Long.parseLong(value.toString(), 8);
    }


    private void copyDirectory(Path source, Path target) throws IOException {
        long copiedBytes = 0;
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path resolved = safeResolve(target, relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(resolved);
                } else {
                    byte[] bytes = DictionaryImportLimits.readFile(path);
                    copiedBytes = DictionaryImportLimits.addExtractedBytes(copiedBytes, bytes.length);
                    Files.createDirectories(resolved.getParent());
                    Files.write(resolved, bytes);
                }
            }
        }
    }

    private Path safeResolve(Path root, String name) throws IOException {
        return ArchiveEntrySafety.safeResolve(root, name);
    }

    private String sourceName(String source) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            String path = URI.create(source).getPath();
            int slash = path.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < path.length()) {
                return path.substring(slash + 1);
            }
            return "dictionary-" + System.currentTimeMillis() + ".txt";
        }

        return Path.of(source).getFileName().toString();
    }

    private void deleteRecursively(Path path) {
        try {
            if (!Files.exists(path)) {
                return;
            }

            try (Stream<Path> stream = Files.walk(path)) {
                List<Path> paths = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
                for (Path item : paths) {
                    Files.deleteIfExists(item);
                }
            }
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not delete path recursively: " + path, exception);
        }
    }

    private String safeFileName(String name) {
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "dictionary-" + System.currentTimeMillis() + ".txt" : safe;
    }

    private void addWords(String words) {
        for (String word : words.split("\\s+")) {
            String normalized = DictionaryWordParser.normalize(word);
            if (!normalized.isBlank()) {
                builtInWords.add(normalized);
            }
        }
    }

    private void loadBuiltInWords() {
        addWords("""
                a able about above accept across act actually add after again against age ago agree
                all almost along already also always am an and animal another answer any anyone
                are area around as ask at away back bad be because become been before began begin
                being best better between big bit black blue both boy bring but by call came can
                cannot car care carry case change chat check child city close come common correct
                could country cut day did different do does dog done dont door down each early easy
                end enough even every example eye face fact far fast feel few find first follow for
                found friend from full game gave get give go good got great green group grow had
                hand hard has have he head hear hello help her here high him his home house how idea if
                important in into is it its just keep kind know large last late later learn leave
                left less let life light like line list little live long look made make man many may
                me mean men might minecraft mod more most move much must my name near need never new
                next night no normal not now number of off often old on once one only open or order other
                our out over own part people place play player point put question quite read real
                red right room run said same saw say school see seem sentence server set she should
                show side small so some something sound spell still story such sure take talk tell
                test than that the their them then there these they thing think this those thought three
                through time to together too took try turn two under up us use used very want was
                water way we well went were what when where which while white who why will with word
                work works world would write wrong year yellow yes yet you your
                """);
    }

    private List<String> builtInMinecraftWords() {
        return List.of(
                "creeper", "elytra", "enderman", "ghast", "netherite"
        );
    }

    private void loadMinecraftWords() {
        for (String word : builtInMinecraftWords()) {
            builtInWords.add(word);
        }
    }

    public record ExternalDictionary(String name, Path path, boolean enabled, Set<String> words) {
    }
}
