package org.jdrupes.builder.java;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractTask;
import org.jdrupes.builder.core.AnyResource;
import org.jdrupes.builder.core.FileSet;

public class AppJarBuilder extends AbstractTask<FileSet> {

    public AppJarBuilder(Project project) {
        super(project);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Resources<FileSet> provide(Resource resource) {
        if (!Resource.KIND_APP_JAR.equals(resource.kind())) {
            return Resources.empty();
        }

        var destDir = project().buildDirectory().resolve("app");
        log.fine(() -> "Getting app jar content for " + project().name());
        project().provided(AnyResource.of(Resource.KIND_CLASSES));
        return Resources.empty();
    }

}
