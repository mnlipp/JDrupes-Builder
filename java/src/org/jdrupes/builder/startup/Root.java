package org.jdrupes.builder.startup;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;
import org.jdrupes.builder.api.AllResources;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
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
        var cpUrls = Stream
            .concat(launcher.provide(AllResources.of(Resource.KIND_CLASSES)),
                launcher.provide(AllResources.of(Resource.KIND_RESOURCES)))
            .map(ft -> {
                try {
                    return ((FileTree) ft).root().toFile().toURI().toURL();
                } catch (MalformedURLException e) {
                    // Cannot happen
                    throw new BuildException(e);
                }
            }).toArray(URL[]::new);
        ;
        new DefaultLauncher(new URLClassLoader(cpUrls,
            Thread.currentThread().getContextClassLoader())).start(args);
    }
}
