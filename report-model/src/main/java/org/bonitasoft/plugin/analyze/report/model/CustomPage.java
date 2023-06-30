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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = RestAPIExtension.class, name = "APIEXTENSION"),
        @Type(value = Theme.class, name = "THEME"),
        @Type(value = Form.class, name = "FORM"),
        @Type(value = Page.class, name = "PAGE")
})
public abstract class CustomPage {

    public static final String DISPLAY_NAME_PROPERTY = "displayName";

    public static final String DESCRIPTION_PROPERTY = "description";

    public static final String NAME_PROPERTY = "name";

    private String name;

    private String displayName;

    private String description;

    private Artifact artifact;

    protected static <T extends CustomPage> T create(String name, String displayName, String description,
            Artifact artifact, Class<T> type) {
        try {
            T o = type.getDeclaredConstructor().newInstance();
            o.setName(name);
            o.setDisplayName(displayName);
            o.setDescription(description);
            o.setArtifact(artifact);
            return o;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create a new instance of class: " + type.getName(), e);
        }
    }

    public enum CustomPageType {
        FORM, PAGE, THEME, APIEXTENSION;
    }

}
