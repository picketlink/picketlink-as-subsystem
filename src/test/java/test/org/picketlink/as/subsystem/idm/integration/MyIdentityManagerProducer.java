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

package test.org.picketlink.as.subsystem.idm.integration;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.picketlink.idm.IdentityManagerFactory;

/**
 * @author Pedro Silva
 *
 */
@ApplicationScoped
public class MyIdentityManagerProducer {

    @Resource (mappedName="picketlink/JPABasedWithDataSourceIMF")
    @Produces
    @IdentityManagerConfig ("dev")
    private IdentityManagerFactory developmentIMF;
    
    @Resource (mappedName="picketlink/JPABasedWithEntityManagerFactoryIMF")
    @Produces
    @IdentityManagerConfig ("staging")
    private IdentityManagerFactory stagingIMF;

    @Resource (mappedName="picketlink/file-based-complete")
    @Produces
    @IdentityManagerConfig ("production")
    private IdentityManagerFactory productionIMF;

    @Resource (mappedName="picketlink/FileBasedSimpleConfigIMF")
    @Produces
    @IdentityManagerConfig ("test")
    private IdentityManagerFactory testingIMF;
    
}
