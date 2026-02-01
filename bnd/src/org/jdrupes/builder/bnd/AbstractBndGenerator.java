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

package org.jdrupes.builder.bnd;

import aQute.bnd.osgi.Processor;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.AbstractGenerator;

// TODO: Auto-generated Javadoc
/// A base class for providers using bndlib.
///
public abstract class AbstractBndGenerator extends AbstractGenerator {

    private final List<Tuple2<String, String>> instructions = new ArrayList<>();

    /// Initializes a new abstract bnd generator.
    ///
    /// @param project the project
    ///
    public AbstractBndGenerator(Project project) {
        super(project);
    }

    /// Add the instruction specified by key and value.
    ///
    /// @param key the key
    /// @param value the value
    /// @return the bnd analyzer
    ///
    public AbstractBndGenerator instruction(String key, String value) {
        instructions.add(Tuple.of(key, value));
        return this;
    }

    /// Add the given instructions for the analyzer.
    ///
    /// @param instructions the instructions
    /// @return the bnd analyzer
    ///
    public AbstractBndGenerator instructions(Map<String, String> instructions) {
        instructions.forEach(this::instruction);
        return this;
    }

    /// Add the instructions from the given bnd (properties) file.
    ///
    /// @param bndFile the bnd file
    /// @return the bnd analyzer
    ///
    public AbstractBndGenerator instructions(Path bndFile) {
        var props = new Properties();
        try {
            props.load(Files.newInputStream(bndFile));
            props.forEach((k, v) -> instruction(k.toString(), v.toString()));
        } catch (IOException e) {
            throw new BuildException("Cannot read bnd file %s: %s", bndFile,
                e).from(this).cause(e);
        }
        return this;
    }

    /// Apply the collected instructions to the given bnd processor.
    ///
    /// @param processor the processor
    ///
    protected void applyInstructions(Processor processor) {
        instructions.forEach(t -> processor.setProperty(t._1, t._2));
    }
}
