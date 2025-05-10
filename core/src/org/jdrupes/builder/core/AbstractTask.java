package org.jdrupes.builder.core;

import java.util.logging.Logger;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Task;

public abstract class AbstractTask implements Task {

    protected final Logger log = Logger.getLogger(getClass().getName());

    private Project project;
    private String name;

    public AbstractTask(Project project, String name) {
        this.project = project;
        this.name = name;
    }

    public AbstractTask(Project project) {
        this.project = project;
        name = getClass().getSimpleName();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Project project() {
        return project;
    }
}
