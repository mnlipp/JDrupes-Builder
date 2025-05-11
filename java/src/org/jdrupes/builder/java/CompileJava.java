package org.jdrupes.builder.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.FileResource;
import org.jdrupes.builder.core.FileSet;
import org.jdrupes.builder.core.ResourceSet;
import org.jdrupes.builder.core.AbstractTask;

public class CompileJava extends AbstractTask<FileSet> {

    private List<FileSet> sources = new ArrayList<>();

    public CompileJava(Project project) {
        super(project);
    }

    public CompileJava addSources(FileSet sources) {
        this.sources.add(sources);
        return this;
    }

    public Collection<Path> sourcePaths() {
        return sources.stream().map(Resources::stream)
            .flatMap(Function.identity()).map(FileResource::path)
            .collect(Collectors.toList());
    }

    private String classpath(Resource resource) {
        return project().provided(resource).stream()
            .<Path> mapMulti((r, sink) -> {
                if (r instanceof FileSet fileSet) {
                    sink.accept(fileSet.root());
                }
            }).map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Resources<FileSet> provide(Resource resource) {
        var destDir = project().buildDirectory().resolve("classes");
        log.fine(() -> "Getting classpath in " + project().name());
        var classpath = classpath(resource);

        log.info(() -> "Compiling Java in " + project().name());
        var javac = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var fileManager
            = javac.getStandardFileManager(diagnostics, null, null)) {
            var compilationUnits
                = fileManager.getJavaFileObjectsFromPaths(sourcePaths());
            if (!javac.getTask(null, fileManager, null,
                List.of("-d", destDir.toString(),
                    "-cp", classpath),
                null, compilationUnits).call()) {
                throw new BuildException("Compilation failed");
            }
        } catch (Exception e) {
            log.log(java.util.logging.Level.SEVERE,
                "Project " + project().name() + ": "
                    + "Problem compiling Java: " + e.getMessage());
            throw new BuildException(e);
        } finally {
            for (var diagnostic : diagnostics.getDiagnostics()) {
                log.info(() -> String.format("Error on line %d in %s%n",
                    diagnostic.getLineNumber(),
                    diagnostic.getSource().toUri()));
            }
        }

        return ResourceSet.of(new FileSet(project(), destDir, "**/*"));
    }

}
