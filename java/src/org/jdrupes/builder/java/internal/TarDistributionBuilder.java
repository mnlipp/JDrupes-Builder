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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.ResourceType.BaseFileTreeType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.java.ApplicationTarFile;
import org.jdrupes.builder.java.ClasspathElement;
import static org.jdrupes.builder.java.JavaTypes.JarFileType;

/// The Class TarDistributionBuilder.
///
public class TarDistributionBuilder extends DistributionBuilder {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    /// Initializes a new tar distribution builder.
    ///
    public TarDistributionBuilder() {
        // Make javadoc happy
    }

    /// Builds the tar.
    ///
    /// @param file the file
    /// @param config the config
    /// @param cpElements the cp elements
    ///
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount",
        "PMD.AvoidUsingOctalValues" })
    public void build(ApplicationTarFile file,
            ApplicationConfigurationData config,
            Resources<ClasspathElement> cpElements) {
        try (var tos = new TarArchiveOutputStream(
            Files.newOutputStream(file.path()))) {
            List<String> distClassPath = new ArrayList<>();
            int classTreeCount = 0;
            for (var cpe : cpElements.get()) {
                if (JarFileType.isAssignableFrom(cpe.type())) {
                    var entryName = Path.of("lib").resolve(
                        cpe.toPath().getFileName()).toString();
                    distClassPath.add("$APP_HOME/" + entryName);
                    tos.putArchiveEntry(
                        new TarArchiveEntry(cpe.toPath(), entryName));
                    Files.copy(cpe.toPath(), tos);
                    tos.closeArchiveEntry();
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
                    addClassTree(tos, distClassPath, classTreeCount, fileTree);
                    continue;
                }
                logger.atWarning().log("Cannot add %s to distribution", cpe);
            }

            // Add generated scripts, first Unix
            var model = buildModel(config, distClassPath);
            var bos = new ByteArrayOutputStream();
            addUnixScript(bos, model);
            byte[] data = bos.toByteArray();
            var entry = new TarArchiveEntry(
                Path.of("bin", config.executableName()).toString());
            entry.setSize(data.length);
            entry.setMode(0755);
            tos.putArchiveEntry(entry);
            tos.write(data);
            tos.closeArchiveEntry();

            // Now Windows
            bos.reset();
            addWindowsBat(bos, model);
            data = bos.toByteArray();
            entry = new TarArchiveEntry(Path.of("bin",
                config.executableName() + ".bat").toString());
            entry.setSize(data.length);
            entry.setMode(0755);
            tos.putArchiveEntry(entry);
            tos.write(data);
            tos.closeArchiveEntry();
        } catch (IOException e) {
            throw new BuildException().cause(e);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void addClassTree(TarArchiveOutputStream tos,
            List<String> distClassPath, int classTreeCount,
            FileTree<FileResource> tree) throws IOException {
        var treeDir = Path.of("classes")
            .resolve(Integer.toString(classTreeCount));
        distClassPath.add("$APP_HOME/" + treeDir.toString());
        var iter = tree.entries().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            tos.putArchiveEntry(
                new TarArchiveEntry(tree.root().resolve(entry.path()),
                    treeDir.resolve(entry.path()).toString()));
            Files.copy(tree.root().resolve(entry.path()), tos);
            tos.closeArchiveEntry();
        }
    }

}
