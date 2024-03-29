/** 
 * Copyright (C) 2023 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ACTOR_FILTER")
public class ActorFilterImplementation extends Implementation {

    public static ActorFilterImplementation create(String className,
            DescriptorIdentifier definitionIdentifier,
            DescriptorIdentifier implementationIdentifier,
            Artifact artifact,
            String jarEntry) {
        return Implementation.create(className,
                definitionIdentifier,
                implementationIdentifier,
                artifact,
                jarEntry,
                ActorFilterImplementation.class);
    }

}
