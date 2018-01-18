/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "project")
public class Project extends MyEntity {

    public static final int NO_PROJECT_ID = 0;

    public static Project noProject() {
        Project project = new Project();
        project.id = NO_PROJECT_ID;
        project.title = "<NO_PROJECT>";
        project.isActive = true;
        return project;
    }

    @Column(name = "is_active")
    public boolean isActive;
}
