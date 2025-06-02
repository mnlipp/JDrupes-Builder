package org.jdrupes.builder.java;

import java.util.Locale;
import java.util.logging.Level;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.core.AbstractGenerator;

public abstract class JavaTool<T extends Resource>
        extends AbstractGenerator<T> {

    public JavaTool(Project project) {
        super(project);
    }

    protected void logDiagnostic(
            Diagnostic<? extends JavaFileObject> diagnostic) {
        String msg;
        if (diagnostic.getSource() == null) {
            msg = diagnostic.getMessage(Locale.ENGLISH);
        } else {
            msg = String.format("%s:%d: %s",
                diagnostic.getSource().toUri().getPath(),
                diagnostic.getLineNumber(),
                diagnostic.getMessage(Locale.ENGLISH));
        }
        Level level = switch (diagnostic.getKind()) {
        case ERROR -> Level.SEVERE;
        case WARNING -> Level.WARNING;
        case MANDATORY_WARNING -> Level.WARNING;
        default -> Level.INFO;
        };
        log.log(level, () -> msg);
    }

    protected void
            logDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        for (var diagnostic : diagnostics.getDiagnostics()) {
            logDiagnostic(diagnostic);
        }
    }
}
