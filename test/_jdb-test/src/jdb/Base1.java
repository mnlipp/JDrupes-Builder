package jdb;

import org.jdrupes.builder.core.FileSet;

import java.nio.file.Path;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.CompileJava;

public class Base1 extends DefaultProject {

    public Base1(Project parent) {
        super(parent, "base1");
        provider(new CompileJava(this)
            .addSources(new FileSet(this, Path.of("src"), "**/*.java")));
    }

}
