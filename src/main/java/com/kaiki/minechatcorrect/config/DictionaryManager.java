package com.kaiki.minechatcorrect.config;

import net.neoforged.fml.loading.FMLPaths;

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
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public final class DictionaryManager {
    public static final String DEFAULT_DICTIONARY_URL = "http://download.huzheng.org/bigdict/stardict-Concise_Oxford_Thesaurus_2nd_Ed-2.4.2.tar.bz2";

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("mine_chatcorrect");
    private static final Path EXTRA_WORDS_FILE = CONFIG_DIR.resolve("additional_words.txt");
    private static final Path DICTIONARY_DIR = CONFIG_DIR.resolve("dictionaries");
    private static final Path IMPORT_DIR = DICTIONARY_DIR.resolve("imports");

    private final Set<String> builtInWords = new LinkedHashSet<>();
    private final Set<String> extraWords = new LinkedHashSet<>();
    private final List<ExternalDictionary> dictionaries = new ArrayList<>();

    public DictionaryManager() {
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
        String normalized = normalize(word);
        if (!normalized.isBlank()) {
            extraWords.add(normalized);
            saveExtraWords();
        }
    }

    public void removeExtraWord(String word) {
        if (extraWords.remove(normalize(word))) {
            saveExtraWords();
        }
    }

    public void replaceExtraWord(String oldWord, String newWord) {
        String oldNormalized = normalize(oldWord);
        String newNormalized = normalize(newWord);
        if (!newNormalized.isBlank()) {
            extraWords.remove(oldNormalized);
            extraWords.add(newNormalized);
            saveExtraWords();
        }
    }

    public String importDictionary(String source) throws IOException {
        Files.createDirectories(DICTIONARY_DIR);
        Files.createDirectories(IMPORT_DIR);

        String trimmed = source.trim();
        if (trimmed.isBlank()) {
            throw new IOException("Dictionary source is blank.");
        }

        String sourceName = sourceName(trimmed);
        Path importTarget = IMPORT_DIR.resolve(System.currentTimeMillis() + "-" + safeFileName(sourceName));
        Files.createDirectories(importTarget);

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            byte[] content = downloadBytes(trimmed);
            importBytes(sourceName, content, importTarget);
        } else {
            Path sourcePath = Path.of(trimmed);
            if (Files.isDirectory(sourcePath)) {
                copyDirectory(sourcePath, importTarget);
            } else {
                importBytes(sourcePath.getFileName().toString(), Files.readAllBytes(sourcePath), importTarget);
            }
        }

        ExternalDictionary dictionary = loadDictionaryFromDirectory(importTarget);
        if (dictionary == null || dictionary.words().isEmpty()) {
            throw new IOException("No usable dictionary words found. Try a .dic, word-list, .zip, .tar.gz, or .tar.bz2 StarDict archive.");
        }

        dictionaries.add(dictionary);
        return dictionary.name();
    }

    public void setDictionaryEnabled(String name, boolean enabled) {
        for (int i = 0; i < dictionaries.size(); i++) {
            ExternalDictionary dictionary = dictionaries.get(i);
            if (dictionary.name().equals(name)) {
                dictionaries.set(i, new ExternalDictionary(dictionary.name(), dictionary.path(), enabled, dictionary.words()));
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
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(DICTIONARY_DIR);
            Files.createDirectories(IMPORT_DIR);

            if (!Files.exists(EXTRA_WORDS_FILE)) {
                Files.write(EXTRA_WORDS_FILE, builtInMinecraftWords(), StandardCharsets.UTF_8);
            }

            for (String line : Files.readAllLines(EXTRA_WORDS_FILE, StandardCharsets.UTF_8)) {
                String normalized = normalize(line);
                if (!normalized.isBlank()) {
                    extraWords.add(normalized);
                }
            }

            try (Stream<Path> stream = Files.list(IMPORT_DIR)) {
                stream.filter(Files::isDirectory).forEach(path -> {
                    try {
                        ExternalDictionary dictionary = loadDictionaryFromDirectory(path);
                        if (dictionary != null && !dictionary.words().isEmpty()) {
                            dictionaries.add(dictionary);
                        }
                    } catch (IOException ignored) {
                    }
                });
            }

            try (Stream<Path> stream = Files.list(DICTIONARY_DIR)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        String content = Files.readString(path, StandardCharsets.UTF_8);
                        dictionaries.add(new ExternalDictionary(path.getFileName().toString(), path, true, parseDictionaryWords(content)));
                    } catch (IOException ignored) {
                    }
                });
            }
        } catch (IOException ignored) {
        }
    }

    private void saveExtraWords() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.write(EXTRA_WORDS_FILE, extraWords.stream().sorted().toList(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private byte[] downloadBytes(String source) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(source).toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "Mine-ChatCorrect/0.1.0-a3 Minecraft NeoForge");
        connection.setRequestProperty("Accept", "*/*");

        int status = connection.getResponseCode();
        if (status >= 300 && status < 400) {
            String location = connection.getHeaderField("Location");
            if (location != null && !location.isBlank()) {
                return downloadBytes(URI.create(source).resolve(location).toString());
            }
        }

        if (status < 200 || status >= 300) {
            throw new IOException("Download failed. HTTP status: " + status);
        }

        byte[] content = connection.getInputStream().readAllBytes();
        if (content.length == 0) {
            throw new IOException("Download failed. Received empty file.");
        }

        String sample = new String(content, 0, Math.min(content.length, 128), StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        if (sample.startsWith("<!doctype html") || sample.startsWith("<html")) {
            throw new IOException("Download returned HTML instead of a dictionary archive.");
        }

        return content;
    }

    private void importBytes(String name, byte[] content, Path targetDir) throws IOException {
        String lower = name.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".zip")) {
            extractZip(content, targetDir);
        } else if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
            extractTar(gunzip(content), targetDir);
        } else if (lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") || lower.endsWith(".tbz")) {
            extractTarBz2ViaSystemTar(name, content, targetDir);
        } else if (lower.endsWith(".gz") || lower.endsWith(".dz")) {
            String uncompressedName = name.substring(0, name.lastIndexOf('.'));
            Files.write(targetDir.resolve(safeFileName(uncompressedName)), gunzip(content));
        } else {
            Files.write(targetDir.resolve(safeFileName(name)), content);
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
            if (lower.endsWith(".idx")) {
                words.addAll(parseStarDictIndex(Files.readAllBytes(file)));
            } else if (lower.endsWith(".idx.gz")) {
                words.addAll(parseStarDictIndex(gunzip(Files.readAllBytes(file))));
            } else if (lower.endsWith(".dic") || lower.endsWith(".txt") || lower.endsWith(".words")) {
                words.addAll(parseDictionaryWords(readTextFallback(file)));
            }
        }

        return new ExternalDictionary(name, directory, true, words);
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
            } catch (RuntimeException ignored) {
                word = new String(bytes, start, index - start, Charset.forName("ISO-8859-1"));
            }
            String normalized = normalizeStarDictWord(word);
            if (!normalized.isBlank()) {
                words.add(normalized);
            }

            index++;
            index += 8;
        }

        return words;
    }

    private Set<String> parseDictionaryWords(String content) {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("/")) {
                continue;
            }

            if (trimmed.matches("\\d+")) {
                continue;
            }

            String word = trimmed.split("[/\\s]", 2)[0];
            String normalized = normalizeStarDictWord(word);
            if (!normalized.isBlank()) {
                words.add(normalized);
            }
        }
        return words;
    }

    private String normalizeStarDictWord(String word) {
        String normalized = normalize(word);
        if (normalized.indexOf(' ') >= 0 || normalized.indexOf('_') >= 0 || normalized.indexOf('-') >= 0) {
            return "";
        }

        if (!normalized.matches("[\\p{L}']{2,}")) {
            return "";
        }

        return normalized;
    }

    private String readTextFallback(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);

        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (RuntimeException ignored) {
            return new String(bytes, Charset.forName("ISO-8859-1"));
        }
    }

    private byte[] gunzip(byte[] bytes) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return input.readAllBytes();
        }
    }

    private void extractZip(byte[] bytes, Path targetDir) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    Path target = safeResolve(targetDir, entry.getName());
                    Files.createDirectories(target.getParent());
                    Files.write(target, zip.readAllBytes());
                }
                entry = zip.getNextEntry();
            }
        }
    }

    private void extractTar(byte[] bytes, Path targetDir) throws IOException {
        int index = 0;

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

            if (type != '5' && !name.isBlank()) {
                Path target = safeResolve(targetDir, name);
                Files.createDirectories(target.getParent());
                Files.write(target, java.util.Arrays.copyOfRange(bytes, index, (int) Math.min(bytes.length, index + size)));
            }

            long paddedSize = ((size + 511) / 512) * 512;
            index += (int) paddedSize;
        }
    }

    private void extractTarBz2ViaSystemTar(String name, byte[] content, Path targetDir) throws IOException {
        Path tempDir = Files.createTempDirectory("mine-chatcorrect-bzip2-");
        try {
            Path archive = tempDir.resolve(safeFileName(name));
            Files.write(archive, content);

            Process process = new ProcessBuilder(findTarCommand(), "-xjf", archive.toString(), "-C", targetDir.toString())
                    .redirectErrorStream(true)
                    .start();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            process.getInputStream().transferTo(output);

            try {
                int exit = process.waitFor();
                if (exit != 0) {
                    String message = output.toString(StandardCharsets.UTF_8);
                    throw new IOException("System tar could not extract .tar.bz2 archive. Exit status: " + exit + ". " + message);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while extracting .tar.bz2 archive.", exception);
            }
        } finally {
            deleteRecursively(tempDir);
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
            } catch (IOException | InterruptedException ignored) {
                if (ignored instanceof InterruptedException) {
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
        String value = tarString(header, offset, length).trim();
        if (value.isBlank()) {
            return 0;
        }

        return Long.parseLong(value, 8);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path resolved = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    Files.copy(path, resolved);
                }
            }
        }
    }

    private Path safeResolve(Path root, String name) throws IOException {
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root.normalize())) {
            throw new IOException("Unsafe archive entry: " + name);
        }
        return target;
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
        } catch (IOException ignored) {
        }
    }

    private String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String name) {
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "dictionary-" + System.currentTimeMillis() + ".txt" : safe;
    }

    private void addWords(String words) {
        for (String word : words.split("\\s+")) {
            String normalized = normalize(word);
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
