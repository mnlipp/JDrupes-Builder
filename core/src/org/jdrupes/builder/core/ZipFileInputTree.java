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

package org.jdrupes.builder.core;

import com.google.common.flogger.FluentLogger;
import io.github.azagniotov.matcher.AntPathMatcher;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.InputResource;
import org.jdrupes.builder.api.InputTree;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;

/// The default implementation of a [ZipFileInputTree].
///
/// @param <T> the type of the [InputResource]s in the tree.
///
public class ZipFileInputTree<T extends InputResource> extends ResourceObject
        implements InputTree<T> {
    @SuppressWarnings({ "unused" })
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final AntPathMatcher pathMatcher
        = new AntPathMatcher.Builder().build();
    private Instant latestChange;
    private final Path zipFilePath;
    private ZipFile zipFile;
    private final String[] patterns;
    private final List<String> excludes = new ArrayList<>();

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree provided by the zip file.
    ///
    /// @param type the resource type
    /// @param zipFile the ZIP file
    /// @param patterns the patterns
    ///
    @SuppressWarnings({ "PMD.ArrayIsStoredDirectly", "PMD.UseVarargs" })
    protected ZipFileInputTree(ResourceType<?> type,
            org.jdrupes.builder.api.ZipFile zipFile, String[] patterns) {
        super(type);
        this.zipFilePath = zipFile.path();
        if (patterns.length == 0) {
            this.patterns = new String[] { "**/*" };
        } else {
            this.patterns = patterns;
        }
    }

    @Override
    public Resources<T> clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resources<T> add(T resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return !zipFile().entries().hasMoreElements();
    }

    @Override
    public ZipFileInputTree<T> exclude(String pattern) {
        excludes.add(pattern);
        return this;
    }

    private ZipFile zipFile() {
        try {
            if (zipFile == null) {
                zipFile = new ZipFile(zipFilePath.toFile());
            }
            return zipFile;
        } catch (IOException e) {
            throw new BuildException().cause(e);
        }
    }

    @Override
    public Optional<Instant> asOf() {
        if (latestChange == null) {
            latestChange = zipFile().stream().map(ZipEntry::getLastModifiedTime)
                .max(FileTime::compareTo).map(FileTime::toInstant)
                .orElse(Instant.EPOCH);
        }
        return Optional.ofNullable(latestChange);
    }

    @SuppressWarnings({ "unchecked", "PMD.AvoidInstantiatingObjectsInLoops" })
    @Override
    public Stream<Entry<T>> entries() {
        List<Entry<T>> result = new ArrayList<>();
        var entries = zipFile().entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (!Arrays.stream(patterns).anyMatch(
                pattern -> pathMatcher.isMatch(pattern, entry.getName()))
                || excludes.stream().anyMatch(
                    ex -> pathMatcher.isMatch(ex, entry.getName()))
                || entry.isDirectory()) {
                continue;
            }
            try {
                result.add(new Entry<>(Path.of(entry.getName()),
                    ResourceFactory.create(
                        (ResourceType<T>) type().containedType(),
                        entry.getLastModifiedTime().toInstant(),
                        zipFile.getInputStream(entry))));
            } catch (IOException e) {
                throw new BuildException().cause(e);
            }
        }
        return result.stream();
    }

    @Override
    public Stream<Path> paths() {
        return entries().map(Entry::path);
    }

    @Override
    public Stream<T> stream() {
        return entries().map(Entry::resource);
    }

    @Override
    public SequencedSet<T> get() {
        return stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
