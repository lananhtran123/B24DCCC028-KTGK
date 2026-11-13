package org.gradle.wrapper;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Small transparent bootstrap compatible with GradleWrapperMain invocation.
 * Project source includes this class because the official Gradle wrapper binary
 * could not be downloaded in the artifact-generation environment.
 * It downloads only the distributionUrl declared in gradle-wrapper.properties.
 */
public final class GradleWrapperMain {
    public static void main(String[] args) throws Exception {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path propPath = findProperties(current);
        if (propPath == null) {
            System.err.println("Cannot locate gradle/wrapper/gradle-wrapper.properties");
            System.exit(1);
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propPath)) { props.load(in); }
        String url = props.getProperty("distributionUrl");
        if (url == null || !url.startsWith("https://services.gradle.org/distributions/gradle-")) {
            throw new IllegalArgumentException("Unsupported distributionUrl: " + url);
        }
        String zipName = url.substring(url.lastIndexOf('/') + 1);
        String version = zipName.substring("gradle-".length(), zipName.indexOf("-bin.zip"));
        String gradleHomeEnv = System.getenv("GRADLE_HOME");
        Path home = null;
        if (gradleHomeEnv != null && !gradleHomeEnv.isEmpty()) {
            Path candidate = Paths.get(gradleHomeEnv);
            if (Files.exists(candidate.resolve("bin").resolve(isWindows() ? "gradle.bat" : "gradle"))) home = candidate;
        }
        if (home == null) {
            Path cache = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "library-bootstrap", version);
            home = cache.resolve("gradle-" + version);
            if (!Files.exists(cache.resolve(".complete"))) {
                Files.createDirectories(cache);
                Path temp = cache.resolve(zipName + ".downloading");
                System.out.println("Downloading Gradle " + version + " from " + url);
                URLConnection connection = new URL(url).openConnection();
                connection.setConnectTimeout(30_000);
                connection.setReadTimeout(60_000);
                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                }
                try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(temp))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        Path dest = cache.resolve(entry.getName()).normalize();
                        if (!dest.startsWith(cache)) throw new IOException("Unsafe zip path: " + entry.getName());
                        if (entry.isDirectory()) Files.createDirectories(dest);
                        else {
                            Files.createDirectories(dest.getParent());
                            Files.copy(zip, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                Files.deleteIfExists(temp);
                Files.createFile(cache.resolve(".complete"));
            }
        }
        Path executable = home.resolve("bin").resolve(isWindows() ? "gradle.bat" : "gradle");
        executable.toFile().setExecutable(true);
        List<String> command = new ArrayList<>();
        command.add(executable.toAbsolutePath().toString());
        command.addAll(Arrays.asList(args));
        Process p = new ProcessBuilder(command)
                .directory(current.toFile())
                .inheritIO()
                .start();
        System.exit(p.waitFor());
    }

    private static Path findProperties(Path dir) {
        Path p = dir;
        while (p != null) {
            Path path = p.resolve("gradle/wrapper/gradle-wrapper.properties");
            if (Files.exists(path)) return path;
            p = p.getParent();
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
