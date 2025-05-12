package org.jdrupes.builder.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.AllResources;
import org.jdrupes.builder.core.FileResource;
import org.jdrupes.builder.core.FileSet;

public class AppJarBuilder extends AbstractGenerator<FileResource> {

    public AppJarBuilder(Project project) {
        super(project);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Optional<FileResource> provide(Resource resource) {
        if (!Resource.KIND_APP_JAR.equals(resource.kind())) {
            return Optional.empty();
        }

        log.info(() -> "Building application jar in " + project().name());
        log.fine(() -> "Getting app jar content for " + project().name());
        var classSets
            = project().provided(AllResources.of(Resource.KIND_CLASSES));

        var destDir = project().buildDirectory().resolve("app");
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }
        var jarPath = destDir.resolve(project().name() + ".jar");

        Manifest manifest = new Manifest();
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jos
            = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            classSets.stream().forEach(classSet -> {
                if (Resource.KIND_CLASSES.equals(classSet.kind())) {
                    var d = (Resources<FileResource>) classSet;
                }
            });
//            for (int i = 2; i < args.length; i++) {
//                addClassFile(jos, args[i]);
//            }
        } catch (IOException e) {
            throw new BuildException(e);
        }

        return Optional.empty();
    }

//    private static void addClassFile(JarOutputStream jos, String classFilePath)
//            throws IOException {
//        File classFile = new File(classFilePath);
//        if (!classFile.exists()) {
//            throw new FileNotFoundException(
//                "Class file not found: " + classFilePath);
//        }
//
//        // Create JAR entry (use / separators even on Windows)
//        String entryName = classFile.getName().replace("\\", "/");
//        JarEntry entry = new JarEntry(entryName);
//        entry.setTime(classFile.lastModified());
//        jos.putNextEntry(entry);
//
//        // Write class file contents
//        try (FileInputStream fis = new FileInputStream(classFile)) {
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = fis.read(buffer)) != -1) {
//                jos.write(buffer, 0, bytesRead);
//            }
//        }
//
//        jos.closeEntry();
//    }
}
