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

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Credentials.Status;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.SimpleUser;

/**
 * @author Pedro Silva
 *
 */
@RunWith(Arquillian.class)
public class PicketLinkIDMCDIExtensionTestCase {
    
    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(PicketLinkIDMCDIExtensionTestCase.class.getClassLoader().getResource("deployment/jboss-deployment-structure-idm.xml"), "jboss-deployment-structure.xml")
                .addClass(PicketLinkIDMCDIExtensionTestCase.class)
                .addClass(MyIdentityConfigurationProducer.class);;

        System.out.println(deployment.toString(true));
        
        return deployment;
    }
    
    @Inject
    private IdentityManager identityManager;

    @Test
    public void testDevIdentityManager() throws Exception {
        SimpleUser user = new SimpleUser("jonhy");
        
        this.identityManager.add(user);
        
        Password password = new Password("mypassWd");
        
        this.identityManager.updateCredential(user, password);
        
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);
        
        this.identityManager.validateCredentials(credentials);
        
        Assert.assertEquals(Status.VALID, credentials.getStatus());
    }
    
}
