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

package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Impl extends AbstractProject implements JavaLibraryProject {

    public Impl() {
        super(name("impl"));
        dependency(Expose, project(Api.class));
        dependency(Reveal, new MvnRepoLookup().resolve(
            "com.electronwill.night-config:yaml:[3.6.7,3.7.0)",
            // https://security.snyk.io/package/maven/org.yaml:snakeyaml
            "org.yaml:snakeyaml:[1.33,2)"));
    }

}
