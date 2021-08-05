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

import static org.assertj.core.api.Assertions.assertThat;

import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.junit.jupiter.api.Test;

class IssueTest {

    @Test
    void shouldCreateIssueWithoutContext() throws Exception {
        Issue issue = Issue.create(Type.INCOMPATIBLE_DEPENDENCY, "bla bla", Severity.ERROR);

        assertThat(issue.getContext()).isEmpty();
        assertThat(issue.getType()).isEqualTo(Type.INCOMPATIBLE_DEPENDENCY.name());
        assertThat(issue.getMessage()).isEqualTo("bla bla");
        assertThat(issue.getSeverity()).isEqualTo(Severity.ERROR.name());
    }

    @Test
    void shouldCreateIssueWithContext() throws Exception {
        Issue issue = Issue.create(Type.INCOMPATIBLE_DEPENDENCY, "bla bla", Severity.ERROR, "a", "b");

        assertThat(issue.getContext()).containsExactly("a", "b");
    }

    @Test
    void shouldBeSymetric() throws Exception {
        Issue issue1 = Issue.create(Type.INCOMPATIBLE_DEPENDENCY, "bla bla", Severity.ERROR, "a", "b");
        Issue issue2 = Issue.create(Type.INCOMPATIBLE_DEPENDENCY, "bla bla", Severity.ERROR, "a", "b");
        assertThat(issue1).isEqualTo(issue2);
        assertThat(issue2).isEqualTo(issue1)
                .hasSameHashCodeAs(issue1)
                .hasToString(issue1.toString());
    }
}
