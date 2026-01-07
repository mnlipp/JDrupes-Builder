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

package org.jdrupes.builder.junit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Intend;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaTypes;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/// A [Generator] for [TestResult]s using the JUnit platform. The test
/// runner uses the
/// [compilation classpath resources][JavaTypes#CompilationClasspathType]
/// from its associated project's [consumed][Intend#Consume] dependencies
/// to detect test classes. It then runs the tests using all compilation
/// classpath resources from dependencies with [Intend#Consume],
/// [Intend#Expose], and [Intend#Supply].
/// 
/// Libraries for compiling the tests and a test engine of your choice
/// must be provided explicitly to the runner's project as dependencies,
///  e.g. as:
/// ```
/// project.dependency(Consume, new MvnRepoLookup()
///     .bom("org.junit:junit-bom:5.12.2")
///     .resolve("org.junit.jupiter:junit-jupiter-api")
///     .resolve(Scope.Runtime,
///        "org.junit.jupiter:junit-jupiter-engine"));
/// ```
///
/// In order to track the execution of the each test, you can enable
/// [Level#FINE] logging for this class.
/// 
public class JUnitTestRunner extends AbstractGenerator {

    /// Initializes a new test runner.
    ///
    /// @param project the project
    ///
    public JUnitTestRunner(Project project) {
        super(project);
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.collects(new ResourceType<TestResult>() {})) {
            return Stream.empty();
        }

        // Collect the classpath. The project under test and the test
        // classes are usually consumed by the test project. Just in case
        // we also include the usual exposed and supplied resources to
        // avoid unexpected behavior.
        var cpResources = newResource(ClasspathType)
            .addAll(project().from(Consume, Expose, Supply)
                .get(requestFor(CompilationClasspathType)));
        log.finest(() -> "Testing in " + project() + " with classpath "
            + cpResources.stream().map(e -> e.toPath().toString())
                .collect(Collectors.joining(File.pathSeparator)));

        // Run the tests
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader testLoader = new URLClassLoader(
            Stream.concat(project().from(Consume)
                .get(requestFor(RuntimeClasspathType)),
                cpResources.stream()).map(ClasspathElement::toPath)
                .map(Path::toUri).map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                }).toArray(URL[]::new),
            ClassLoader.getSystemClassLoader())) {
            Thread.currentThread().setContextClassLoader(testLoader);

            // Discover all tests from compiler's output
            var testClassTrees = project().providers(Supply, Consume)
                .filter(p -> p instanceof JavaCompiler)
                .map(p -> ((JavaCompiler) p).destination())
                .collect(Collectors.toSet());
            LauncherDiscoveryRequest request
                = LauncherDiscoveryRequestBuilder.request().selectors(
                    DiscoverySelectors.selectClasspathRoots(testClassTrees))
                    .build();

            // Run the tests
            var launcher = LauncherFactory.create();
            var summaryListener = new SummaryGeneratingListener();
            var testListener = new TestListener();
            launcher.registerTestExecutionListeners(
                LoggingListener.forJavaUtilLogging(), summaryListener,
                testListener);
            launcher.execute(request);

            // Evaluate results
            var summary = summaryListener.getSummary();
            @SuppressWarnings("unchecked")
            var result = Stream.of((T) project().newResource(TestResultType,
                this, buildName(testListener), summary.getTestsStartedCount(),
                summary.getTestsFailedCount()));
            return result;
        } catch (IOException e) {
            log.log(Level.WARNING, e, () -> "Failed to close classloader");
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }

        // Return result
        return Stream.empty();
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private String buildName(TestListener testListener) {
        StringBuilder asList = new StringBuilder();
        for (var testId : testListener.testIds()) {
            if (!asList.isEmpty()) {
                asList.append(", ");
            }
            if (asList.length() > 20) {
                asList.append(" ...");
                break;
            }
            asList.append(testId.getDisplayName());
        }
        return asList.toString();
    }

    /// A [TestExecutionListener] for JUnit.
    ///
    /// @see TestEvent
    ///
    @SuppressWarnings("PMD.TestClassWithoutTestCases")
    private final class TestListener implements TestExecutionListener {

        private final Set<TestIdentifier> tests = new HashSet<>();
        private TestPlan testPlan;

        /// Return the test classes.
        ///
        /// @return the sets the
        ///
        @SuppressWarnings("PMD.UnitTestShouldUseTestAnnotation")
        public Set<TestIdentifier> testIds() {
            return tests;
        }

        private String prettyTestName(TestIdentifier testId) {
            return Stream.iterate(testId, Objects::nonNull,
                id -> testPlan.getParent(id).orElse(null))
                .toList().reversed().stream().skip(1)
                .map(TestIdentifier::getDisplayName)
                .collect(Collectors.joining(" > "));
        }

        @Override
        @SuppressWarnings("PMD.UnitTestShouldUseTestAnnotation")
        public void testPlanExecutionStarted(TestPlan testPlan) {
            this.testPlan = testPlan;
        }

        @Override
        @SuppressWarnings("PMD.UnitTestShouldUseTestAnnotation")
        public void testPlanExecutionFinished(TestPlan testPlan) {
            // Not tracked
        }

        @Override
        public void dynamicTestRegistered(TestIdentifier testIdentifier) {
            // Not tracked
        }

        @Override
        public void executionSkipped(TestIdentifier testIdentifier,
                String reason) {
            // Not tracked
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if (testIdentifier.getSource().isPresent()
                && testIdentifier.getSource().get() instanceof ClassSource) {
                tests.add(testIdentifier);
            }
        }

        @Override
        @SuppressWarnings("PMD.GuardLogStatement")
        public void executionFinished(TestIdentifier testIdentifier,
                TestExecutionResult testExecutionResult) {
            if (testExecutionResult.getStatus() == Status.SUCCESSFUL) {
                if (testIdentifier.getType() != Type.TEST) {
                    return;
                }
                log.log(Level.FINE,
                    () -> "Succeeded: " + prettyTestName(testIdentifier));
                return;
            }
            if (testExecutionResult.getThrowable().isEmpty()) {
                log.log(Level.WARNING,
                    () -> "Failed: " + prettyTestName(testIdentifier));
                return;
            }
            log.log(Level.WARNING, testExecutionResult.getThrowable().get(),
                () -> "Failed: " + prettyTestName(testIdentifier));
        }

        @Override
        public void reportingEntryPublished(TestIdentifier testIdentifier,
                ReportEntry entry) {
            // Not tracked
        }

        @Override
        public void fileEntryPublished(TestIdentifier testIdentifier,
                FileEntry file) {
            // Not tracked
        }
    }
}
