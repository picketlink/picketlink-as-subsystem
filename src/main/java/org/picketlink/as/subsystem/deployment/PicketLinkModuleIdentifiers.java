/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.picketlink.as.subsystem.deployment;

import org.jboss.modules.ModuleIdentifier;

/**
 * @author Pedro Igor
 */
public interface PicketLinkModuleIdentifiers {

    ModuleIdentifier ORG_PICKETLINK_MODULE = ModuleIdentifier.create("org.picketlink");
    ModuleIdentifier ORG_PICKETLINK_IDM_API_MODULE = ModuleIdentifier.create("org.picketlink.idm.api");
    ModuleIdentifier ORG_PICKETLINK_IDM_MODULE = ModuleIdentifier.create("org.picketlink.idm");
    ModuleIdentifier ORG_PICKETLINK_CORE_API_MODULE = ModuleIdentifier.create("org.picketlink.core.api");
    ModuleIdentifier ORG_PICKETLINK_CORE_MODULE = ModuleIdentifier.create("org.picketlink.core");
}
