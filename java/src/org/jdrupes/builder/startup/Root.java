package org.jdrupes.builder.startup;

import java.util.stream.Stream;
import org.jdrupes.builder.api.AllResources;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.DefaultLauncher;

public class Root extends AbstractProject implements RootProject {

    public Root() {
        super(BootstrapProject.class);
    }

    public static void main(String[] args) {
        var launcher = new DefaultLauncher(Root.class);
        Stream.concat(launcher.provide(AllResources.of(Resource.KIND_CLASSES)),
            launcher.provide(AllResources.of(Resource.KIND_RESOURCES)))
            .forEach(r -> {
                System.out.println(r);
            });

    }
}
