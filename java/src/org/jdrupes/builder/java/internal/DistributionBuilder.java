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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.BuildException;

/// A base class for distribution builders.
///
public class DistributionBuilder {

    /// Initializes a new distribution builder.
    ///
    protected DistributionBuilder() {
        // Make javadoc happy
    }

    /// Base configuration.
    ///
    /// @return the configuration
    ///
    protected Configuration baseConfiguration() {
        var fmConfig = new Configuration(Configuration.VERSION_2_3_34);
        fmConfig.setDefaultEncoding("utf-8");
        fmConfig.setTemplateExceptionHandler(
            TemplateExceptionHandler.RETHROW_HANDLER);
        fmConfig.setLogTemplateExceptions(false);
        return fmConfig;
    }

    /// Builds the FreeMarker model.
    ///
    /// @param config the configuration
    /// @param distClassPath the dist class path
    /// @return the map
    ///
    protected Map<String, Object> buildModel(
            ApplicationConfigurationData config, List<String> distClassPath) {
        var model = new HashMap<String, Object>();
        model.put("applicationName", config.executableName());
        model.put("mainClassName", config.mainClassName());
        model.put("classpath",
            distClassPath.stream().collect(Collectors.joining(":")));
        model.put("optsEnvironmentVar", config.executableName()
            .toUpperCase(Locale.getDefault())
            .replaceAll("[^A-Z0-9_]", "_") + "_OPTS");
        // Fixed, unless we make the bin directory name configurable
        model.put("appHomeRelativePath", "..");
        model.put("defaultJvmOpts", config.applicationJvmOpts().stream()
            .map(s -> "\"" + s + "\"").collect(Collectors.joining(" ")));
        return model;
    }

    /// Adds the unix script.
    ///
    /// @param out the out
    /// @param model the model
    /// @throws IOException Signals that an I/O exception has occurred.
    ///
    protected void addUnixScript(OutputStream out, Map<String, Object> model)
            throws IOException {
        var fmConfig = baseConfiguration();
        fmConfig.setInterpolationSyntax(
            Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        Template template = new Template("unix", new InputStreamReader(
            getClass().getResourceAsStream(
                "/org/jdrupes/builder/java/unixStartScript.ftl"),
            StandardCharsets.UTF_8), fmConfig);
        try {
            var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            template.process(model, writer);
        } catch (TemplateException e) {
            throw new BuildException().cause(e);
        }
    }

    /// Adds the windows bat.
    ///
    /// @param out the out
    /// @param model the model
    /// @throws IOException Signals that an I/O exception has occurred.
    ///
    protected void addWindowsBat(OutputStream out, Map<String, Object> model)
            throws IOException {
        var fmConfig = baseConfiguration();
        Template template = new Template("windows", new InputStreamReader(
            getClass().getResourceAsStream(
                "/org/jdrupes/builder/java/windowsStartScript.ftl"),
            StandardCharsets.UTF_8), fmConfig);
        try {
            var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            template.process(model, writer);
        } catch (TemplateException e) {
            throw new BuildException().cause(e);
        }
    }

}
