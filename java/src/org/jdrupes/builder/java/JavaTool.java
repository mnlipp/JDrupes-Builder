/*
 * JDrupes Builder
 * Copyright (C) 2025 Michael N. Lipp
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

package org.jdrupes.builder.java;

import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.core.AbstractGenerator;

/// A base class for generators that invoke java tools.
///
/// @param <T> the generic type
///
public abstract class JavaTool<T extends Resource>
        extends AbstractGenerator<T> {

    /// Instantiates a new java tool.
    ///
    /// @param project the project
    ///
    public JavaTool(Project project) {
        super(project);
    }

    /// Log diagnostic.
    ///
    /// @param diagnostic the diagnostic
    ///
    protected void logDiagnostic(
            Diagnostic<? extends JavaFileObject> diagnostic) {
        String msg;
        if (diagnostic.getSource() == null) {
            msg = diagnostic.getMessage(Locale.ENGLISH);
        } else {
            msg = String.format("%s:%d: %s",
                project().rootProject().directory().relativize(
                    Path.of(diagnostic.getSource().toUri().getPath())),
                diagnostic.getLineNumber(),
                diagnostic.getMessage(null));
        }
        Level level = switch (diagnostic.getKind()) {
        case ERROR -> Level.SEVERE;
        case WARNING -> Level.WARNING;
        case MANDATORY_WARNING -> Level.WARNING;
        default -> Level.INFO;
        };
        log.log(level, () -> msg);
    }

    /// Log diagnostics.
    ///
    /// @param diagnostics the diagnostics
    ///
    protected void
            logDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        for (var diagnostic : diagnostics.getDiagnostics()) {
            logDiagnostic(diagnostic);
        }
    }
}
