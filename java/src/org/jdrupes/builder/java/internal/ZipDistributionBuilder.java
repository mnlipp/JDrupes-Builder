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

package org.jdrupes.builder.java.internal;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.ResourceType.BaseFileTreeType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.java.ApplicationZipFile;
import org.jdrupes.builder.java.ClasspathElement;
import static org.jdrupes.builder.java.JavaTypes.JarFileType;

/// The Class ZipDistributionBuilder.
///
public class ZipDistributionBuilder extends DistributionBuilder {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    /// Initializes a new zip distribution builder.
    ///
    public ZipDistributionBuilder() {
        super();
    }

    /// Builds the zip.
    ///
    /// @param file the file
    /// @param config the config
    /// @param cpElements the cp elements
    ///
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.AvoidUsingOctalValues" })
    public void build(ApplicationZipFile file,
            ApplicationConfigurationData config,
            Resources<ClasspathElement> cpElements) {
        try (var zos = new ZipArchiveOutputStream(
            Files.newOutputStream(file.path()))) {
            List<String> distClassPath = new ArrayList<>();
            int classTreeCount = 0;
            for (var cpe : cpElements.get()) {
                if (JarFileType.isAssignableFrom(cpe.type())) {
                    String entryName = Path.of("lib")
                        .resolve(cpe.toPath().getFileName()).toString();
                    distClassPath.add("$APP_HOME/" + entryName);
                    zos.putArchiveEntry(new ZipArchiveEntry(entryName));
                    Files.copy(cpe.toPath(), zos);
                    zos.closeArchiveEntry();
                    continue;
                }
                if (BaseFileTreeType.isAssignableFrom(cpe.type())) {
                    @SuppressWarnings("unchecked")
                    FileTree<FileResource> fileTree
                        = (FileTree<FileResource>) cpe;
                    if (fileTree.isEmpty()) {
                        continue;
                    }
                    classTreeCount += 1;
                    addClassTree(
                        zos, distClassPath, classTreeCount, fileTree);
                    continue;
                }
                logger.atWarning().log("Cannot add %s to distribution", cpe);
            }

            // Add generated scripts
            var model = buildModel(config, distClassPath);
            var entry = new ZipArchiveEntry(
                Path.of("bin", config.executableName()).toString());
            entry.setUnixMode(0755);
            zos.putArchiveEntry(entry);
            addUnixScript(zos, model);
            zos.closeArchiveEntry();
            entry = new ZipArchiveEntry(
                Path.of("bin", config.executableName()).toString() + ".bat");
            entry.setUnixMode(0755);
            zos.putArchiveEntry(entry);
            addWindowsBat(zos, model);
            zos.closeArchiveEntry();
        } catch (IOException e) {
            throw new BuildException().cause(e);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void addClassTree(ZipArchiveOutputStream zos,
            List<String> distClassPath, int classTreeCount,
            FileTree<FileResource> tree) throws IOException {
        var treeDir = Path.of("classes")
            .resolve(Integer.toString(classTreeCount));
        distClassPath.add("$APP_HOME/" + treeDir.toString());
        var iter = tree.entries().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            zos.putArchiveEntry(new ZipArchiveEntry(treeDir
                .resolve(entry.path()).toString()));
            Files.copy(tree.root().resolve(entry.path()), zos);
            zos.closeArchiveEntry();
        }
    }

}
