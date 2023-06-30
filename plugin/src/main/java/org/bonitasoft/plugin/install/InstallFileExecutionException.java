/** 
 * Copyright (C) 2020 BonitaSoft S.A.
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
package org.bonitasoft.plugin.install;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstallFileExecutionException extends Exception {

    private final List<Throwable> errors = new ArrayList<>();

    public InstallFileExecutionException(String message, Throwable exception) {
        super(message, exception);
    }

    public InstallFileExecutionException(List<Throwable> errors) {
        this.errors.addAll(errors);
    }

    @Override
    public String getMessage() {
        return errors.stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining(System.lineSeparator()));
    }

}
