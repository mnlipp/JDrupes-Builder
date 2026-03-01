/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jdrupes.builder.ext.nodejs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ResourceProvider;

/// Manages cached node js downloads.
///
public class NodeJsDownloader {

    private final ResourceProvider provider;
    private final Path baseDir;
    private final Platform platform;

    /// Initializes a new node js downloader.
    ///
    /// @param provider the provider
    /// @param baseDir the base dir
    ///
    public NodeJsDownloader(ResourceProvider provider, Path baseDir) {
        this.provider = provider;
        this.baseDir = baseDir;
        platform = detectPlatform();
    }

    private Platform detectPlatform() {
        String opSys = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch
            = System.getProperty("os.arch").contains("64") ? "x64" : "x86";

        if (opSys.contains("win")) {
            return new Platform("win-" + arch, true, "zip");
        } else if (opSys.contains("mac")) {
            return new Platform("darwin-" + arch, false, "tar.gz");
        } else if (opSys.contains("nux")) {
            return new Platform("linux-" + arch, false, "tar.gz");
        }
        throw new BuildException().from(provider)
            .message("Unsupported OS: %s", opSys);
    }

    /// Returns the path to the npm executable for the given version.
    ///
    /// @param version the version
    /// @return the path
    ///
    public Path npmExecutable(String version) {
        Path installDir = downloadAndInstall(version);
        return executable(installDir);
    }

    private Path executable(Path installDir) {
        if (platform.isWindows) {
            return installDir.resolve("npm.cmd");
        } else {
            return installDir.resolve("bin/npm");
        }
    }

    private Path downloadAndInstall(String version) {
        Path unpackedIn = baseDir
            .resolve(String.format("node-v%s-%s", version, platform.name));
        if (Files.exists(unpackedIn)) {
            return unpackedIn;
        }
        try {
            Files.createDirectories(baseDir);
            var archive = download(baseDir, version);
            if (archive.toString().endsWith(".zip")) {
                unzip(baseDir, archive);
            } else if (archive.toString().endsWith(".tar.gz")) {
                untarGz(baseDir, archive);
            }
            executable(unpackedIn).toFile().setExecutable(true, false);
            Files.deleteIfExists(archive);
            return unpackedIn;
        } catch (IOException | InterruptedException e) {
            throw new BuildException().from(provider).cause(e);
        }
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private Path download(Path targetDir, String version)
            throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            String archiveName = String.format("node-v%s-%s.%s", version,
                platform.name, platform.extension);
            String downloadUrl = String.format("https://nodejs.org/dist/v%s/%s",
                version, archiveName);
            var request = HttpRequest.newBuilder().GET()
                .uri(URI.create(downloadUrl)).build();
            var target = targetDir.resolve(archiveName);
            var response = client.send(request, BodyHandlers.ofFile(target));
            if (response.statusCode() / 100 != 2) {
                throw new BuildException().from(provider).message(
                    "Attempt to download %s failed with %d", downloadUrl,
                    response.statusCode());
            }
            return target;
        }
    }

    private void unzip(Path targetDir, Path zipFile) throws IOException {
        try (ZipInputStream zis
            = new ZipInputStream(Files.newInputStream(zipFile))) {
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                Path newPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath,
                        StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void untarGz(Path targetDir, Path tarGz) throws IOException {
        try (InputStream tarGzIn = Files.newInputStream(tarGz);
                InputStream tarIn = new GzipCompressorInputStream(tarGzIn);
                TarArchiveInputStream tar = new TarArchiveInputStream(tarIn)) {

            while (true) {
                TarArchiveEntry entry = tar.getNextEntry();
                if (entry == null) {
                    break;
                }
                Path newPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else if (entry.isSymbolicLink()) {
                    Files.createDirectories(newPath.getParent());
                    Path target = Paths.get(entry.getLinkName());
                    Files.createSymbolicLink(newPath, target);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(tar, newPath,
                        StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private record Platform(String name, boolean isWindows,
            String extension) {
    }

}