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

package org.jdrupes.builder.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.aether.repository.RemoteRepository;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import static org.jdrupes.builder.api.Intent.Consume;
import static org.jdrupes.builder.api.Intent.Expose;
import static org.jdrupes.builder.api.Intent.Forward;
import static org.jdrupes.builder.api.Intent.Reveal;
import static org.jdrupes.builder.api.Intent.Supply;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.java.ClassTree;
import org.jdrupes.builder.java.JavaCompiler;
import static org.jdrupes.builder.java.JavaTypes.CodeContributionType;
import static org.jdrupes.builder.java.JavaTypes.LibraryJarFileType;
import org.jdrupes.builder.mvnrepo.MvnRepoJarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoLibraryJarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.MvnRepoResourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/// The Class LaunchGenerator.
///
public class LaunchGenerator {

    private final EclipseConfigurator configurator;

    /// Initializes a new launch generator.
    ///
    /// @param configurator the eclipse configurator
    ///
    public LaunchGenerator(EclipseConfigurator configurator) {
        this.configurator = configurator;
    }

    @SuppressWarnings("PMD.ShortMethodName")
    private <T extends Resource> ResourceRequest<T>
            of(ResourceType<? extends T> type) {
        return configurator.of(type);
    }

    private Project project() {
        return configurator.project();
    }

    /// Provide the launch configuration.
    ///
    /// @param <T> the generic type
    /// @param requested the requested
    /// @return the collection
    ///
    @SuppressWarnings({ "unchecked", "PMD.ConfusingTernary" })
    public <T extends Resource> Collection<T> provideLauncher(
            ResourceRequest<T> requested) {
        // Generate nothing for test projects.
        if (project() instanceof MergedTestProject
            || !(requested instanceof EclipseLauncherRequest)) {
            return Collections.emptyList();
        }

        var launcherName = requested.name().orElseThrow(
            () -> new ConfigurationException().message(
                "EclipseLauncherRequest must have a name"));

        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        var launchFile
            = project().directory().resolve(launcherName + ".launch");
        var request = (EclipseLauncherRequest) requested;

        if (!Files.exists(launchFile)) {
            configurator.generateXmlFile(
                doc -> generateLaunchConfiguration(doc, request),
                launcherName + ".launch");
        } else {
            configurator.generateXmlFile(doc -> {
                var existing = parseExisting(launchFile);
                generateLaunchConfiguration(doc, request);
                mergeSettings(existing, doc);
                return existing;
            }, launcherName + ".launch");
        }

        // Create result
        return (Collection<T>) List.of(EclipseLauncher.of(
            project().directory().resolve(launcherName + ".launch")));
    }

    private Document parseExisting(Path path) {
        try {
            return configurator.documentBuilderFactory().newDocumentBuilder()
                .parse(path.toFile());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new BuildException().from(configurator).cause(e);
        }
    }

    /// Generates the launch configuration XML into the given document.
    ///
    /// @param doc an empty document as starting point
    /// @param request the request
    /// @return the generated document
    ///
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    protected Document generateLaunchConfiguration(Document doc,
            EclipseLauncherRequest request) {
        var mainClass = request.mainClass();
        if (mainClass == null) {
            throw new ConfigurationException().from(configurator)
                .message("mainClass is required for %s", request.name().get());
        }
        var launchConfig = (Element) doc.appendChild(
            doc.createElement("launchConfiguration"));
        launchConfig.setAttribute("type",
            "org.eclipse.jdt.launching.localJavaApplication");

        // Debug core attributes
        addStringAttribute(doc, launchConfig,
            "org.eclipse.debug.core.source_locator_id",
            "org.eclipse.jdt.launching"
                + ".sourceLocator.JavaAdvancedSourceLookupDirector");
        addBooleanAttribute(doc, launchConfig,
            "org.eclipse.debug.core.ATTR_FORCE_SYSTEM_CONSOLE_ENCODING",
            "false");

        // JDT launching attributes
        addBooleanAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.ATTR_ATTR_USE_ARGFILE", "false");
        addBooleanAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.ATTR_EXCLUDE_TEST_CODE", "true");
        addBooleanAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.ATTR_SHOW_CODEDETAILS"
                + "_IN_EXCEPTION_MESSAGES",
            "true");
        addBooleanAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.ATTR_USE_CLASSPATH_ONLY_JAR", "false");

        // Classpath
        generateLaunchClasspath(doc, launchConfig);
        addBooleanAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.DEFAULT_CLASSPATH", "false");

        // JRE
        addJreToLaunch(doc, launchConfig);

        // Main class
        addStringAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.MAIN_TYPE", mainClass);

        // Module path
        addModulePath(doc, launchConfig);

        // Program arguments
        if (request.arguments() != null && !request.arguments().isBlank()) {
            addStringAttribute(doc, launchConfig,
                "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS",
                request.arguments());
        }

        // Project attribute
        addStringAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.PROJECT_ATTR",
            configurator.eclipseAlias());

        // VM arguments
        if (request.vmArguments() != null
            && !request.vmArguments().isBlank()) {
            addStringAttribute(doc, launchConfig,
                "org.eclipse.jdt.launching.VM_ARGUMENTS",
                request.vmArguments());
        }

        // Working directory
        addStringAttribute(doc, launchConfig,
            "org.eclipse.jdt.launching.WORKING_DIRECTORY",
            "${workspace_loc:" + configurator.eclipseAlias() + "}");

        // Return updated document
        return doc;
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void generateLaunchClasspath(Document doc, Node parent) {
        var classpathAttr = (Element) parent
            .appendChild(doc.createElement("listAttribute"));
        classpathAttr.setAttribute("key",
            "org.eclipse.jdt.launching.CLASSPATH");

        // Add JRE container (type 4)
        var javaCompiler = project().providers()
            .select(Consume, Reveal, Supply)
            .filter(p -> p instanceof JavaCompiler)
            .map(JavaCompiler.class::cast).findFirst();
        var version = javaCompiler
            .map(jc -> jc.optionArgument("-target", "--target", "--release"))
            .map(opt -> opt.orElse("25"))
            .orElse("25");
        addListEntry(doc, classpathAttr,
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<runtimeClasspathEntry "
                + "containerPath=\"org.eclipse.jdt.launching.JRE_CONTAINER"
                + "/org.eclipse.jdt.internal.debug.ui.launcher"
                + ".StandardVMType/JavaSE-" + version + "\""
                + " path=\"5\" type=\"4\"/>");

        // Add project dependencies
        addListEntry(doc, classpathAttr, "<?xml version=\"1.0\""
            + " encoding=\"UTF-8\" standalone=\"no\"?>\n"
            + "<runtimeClasspathEntry projectName=\""
            + configurator.refName(project()) + "\" "
            + "path=\"5\" type=\"1\"/>");
        project().providers().filter(p -> p instanceof Project)
            .select(Consume, Reveal, Expose, Forward)
            .toList().stream().map(Project.class::cast).forEach(p -> {
                addListEntry(doc, classpathAttr, "<?xml version=\"1.0\""
                    + " encoding=\"UTF-8\" standalone=\"no\"?>\n"
                    + "<runtimeClasspathEntry "
                    + "projectName=\"" + configurator.refName(p) + "\" "
                    + "path=\"5\" type=\"1\"/>");
            });

        // Add library jars
        var cpes = Resources.with(LibraryJarFileType);
        Set<RemoteRepository> repos = new HashSet<>();
        var repoRefs = Resources.with(MvnRepoResourceType);
        project().resources(of(LibraryJarFileType)
            .usingAll()).forEach(r -> {
                if (r instanceof MvnRepoJarFile repoJar) {
                    repoRefs.add(repoJar.reference());
                    repos.addAll(repoJar.repositories());
                } else {
                    cpes.add(r);
                }
            });
        // Jar files from maven repositories must be resolved before
        // they can be added to the application to avoid duplicates.
        var lookup = new MvnRepoLookup().name(configurator.name())
            .addRepositories(repos.toArray(RemoteRepository[]::new));
        lookup.resolve(repoRefs.stream());
        project().context()
            .resources(lookup, of(CodeContributionType)
                .using(Consume, Reveal, Supply, Expose))
            .forEach(cpe -> {
                if (cpe instanceof MvnRepoLibraryJarFile jarFile) {
                    cpes.add(jarFile);
                }
            });

        cpes.stream().forEach(jf -> {
            var jarPath = jf.path().toString();
            addListEntry(doc, classpathAttr, "<?xml version=\"1.0\""
                + " encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<runtimeClasspathEntry "
                + "externalArchive=\"" + escapeXml(jarPath) + "\" "
                + "path=\"5\" type=\"2\"/>");
        });
    }

    private void addJreToLaunch(Document doc, Node parent) {
        var javaCompiler = project().providers()
            .select(Consume, Reveal, Supply)
            .filter(p -> p instanceof JavaCompiler)
            .map(JavaCompiler.class::cast).findFirst();
        var version = javaCompiler
            .map(jc -> jc.optionArgument("-target", "--target", "--release"))
            .map(opt -> opt.orElse("25"))
            .orElse("25");
        addStringAttribute(doc, parent,
            "org.eclipse.jdt.launching.JRE_CONTAINER",
            "org.eclipse.jdt.launching.JRE_CONTAINER"
                + "/org.eclipse.jdt.internal.debug.ui.launcher"
                + ".StandardVMType/JavaSE-" + version);
    }

    private void addModulePath(Document doc, Node parent) {
        var modulePath = (Element) parent
            .appendChild(doc.createElement("listAttribute"));
        modulePath.setAttribute("key",
            "org.eclipse.jdt.launching.MODULEPATH");

        var isModular = project()
            .resources(of(CodeContributionType).using(Supply))
            .filter(cc -> cc instanceof ClassTree).map(cc -> (ClassTree) cc)
            .filter(cc -> cc.isModular()).findAny().isPresent();

        if (isModular) {
            // Check for modular project dependencies
            project().providers().filter(p -> p instanceof Project)
                .select(Consume, Reveal, Expose, Forward)
                .toList().stream().map(Project.class::cast).forEach(p -> {
                    if (p
                        .resources(
                            of(CodeContributionType).using(Supply))
                        .filter(cc -> cc instanceof ClassTree)
                        .map(cc -> (ClassTree) cc)
                        .filter(cc -> cc.isModular()).findAny().isPresent()) {
                        addListEntry(doc, modulePath,
                            "<runtimeClasspathEntry "
                                + "path=\"5\" projectName=\""
                                + configurator.refName(p)
                                + "\" type=\"1\"/>");
                    }
                });

            // Set module name
            var moduleName = project()
                .resources(of(CodeContributionType)
                    .using(Supply))
                .filter(cc -> cc instanceof ClassTree)
                .map(cc -> (ClassTree) cc)
                .filter(cc -> cc.isModular()).findAny()
                .flatMap(ClassTree::moduleName).orElse(null);
            if (moduleName != null) {
                addStringAttribute(doc, parent,
                    "org.eclipse.jdt.launching.MODULE_NAME", moduleName);
            }
        }
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void addStringAttribute(Document doc, Node parent,
            String key, String value) {
        var attr = (Element) parent
            .appendChild(doc.createElement("stringAttribute"));
        attr.setAttribute("key", key);
        attr.setAttribute("value", value);
    }

    private void addBooleanAttribute(Document doc, Node parent,
            String key, String value) {
        var attr = (Element) parent
            .appendChild(doc.createElement("booleanAttribute"));
        attr.setAttribute("key", key);
        attr.setAttribute("value", value);
    }

    private void addListEntry(Document doc, Node parent, String value) {
        var entry = (Element) parent
            .appendChild(doc.createElement("listEntry"));
        entry.setAttribute("value", value);
    }

    private String escapeXml(String str) {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    /// Updates the classpath and modulepath in the target document
    /// from a freshly generated source document.
    ///
    /// @param target the existing document to update
    /// @param source the freshly generated document
    ///
    protected void mergeSettings(Document target, Document source) {
        replaceListAttribute(target, source,
            "org.eclipse.jdt.launching.CLASSPATH");
        replaceListAttribute(target, source,
            "org.eclipse.jdt.launching.MODULEPATH");
        replaceStringAttribute(target, source,
            "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS");
        replaceStringAttribute(target, source,
            "org.eclipse.jdt.launching.VM_ARGUMENTS");
    }

    private void replaceListAttribute(Document target, Document source,
            String key) {
        var targetNode = findListAttribute(target, key);
        var sourceNode = findListAttribute(source, key);
        if (sourceNode == null) {
            return;
        }
        if (targetNode != null) {
            var imported = target.importNode(sourceNode, true);
            targetNode.getParentNode().replaceChild(imported, targetNode);
        } else {
            target.getDocumentElement().appendChild(
                target.importNode(sourceNode, true));
        }
    }

    private void replaceStringAttribute(Document target, Document source,
            String key) {
        var targetNode = findStringAttribute(target, key);
        var sourceNode = findStringAttribute(source, key);
        if (sourceNode == null) {
            return;
        }
        if (targetNode != null) {
            ((Element) targetNode).setAttribute("value",
                ((Element) sourceNode).getAttribute("value"));
        } else {
            target.getDocumentElement().appendChild(
                target.importNode(sourceNode, true));
        }
    }

    private Node findListAttribute(Document doc, String key) {
        var root = doc.getDocumentElement();
        var children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            var child = children.item(i);
            if (child instanceof Element
                && "listAttribute".equals(child.getNodeName())
                && key.equals(((Element) child).getAttribute("key"))) {
                return child;
            }
        }
        return null;
    }

    private Node findStringAttribute(Document doc, String key) {
        var root = doc.getDocumentElement();
        var children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            var child = children.item(i);
            if (child instanceof Element
                && "stringAttribute".equals(child.getNodeName())
                && key.equals(((Element) child).getAttribute("key"))) {
                return child;
            }
        }
        return null;
    }

}
