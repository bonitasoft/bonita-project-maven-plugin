/**
 * Copyright (C) 2021 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze.report.model;

import java.util.Arrays;
import java.util.List;

import lombok.Data;

@Data
public class Issue {

    public enum Severity {
        ERROR, WARNING, INFO
    }

    public enum Type {
        INCOMPATIBLE_DEPENDENCY, INVALID_DESCRIPTOR_FILE, UNKNOWN_DEFINITION_TYPE
    }

    private String severity;
    private String type;
    private List<String> context;
    private String message;

    public static Issue create(Type type, String message, Severity severity, String... context) {
        Issue issue = new Issue();
        issue.setType(type.name());
        issue.setSeverity(severity.name());
        issue.setMessage(message);
        issue.setContext(Arrays.asList(context));
        return issue;
    }

}
