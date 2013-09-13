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

package test.org.picketlink.as.subsystem;

import org.junit.After;
import org.junit.Before;
import org.picketbox.test.ldap.AbstractLDAPTest;

/**
 * Abstract base for all LDAP test suites. It handles
 *
 * @author Peter Skopek: pskopek at redhat dot com
 */
public class LDAPEmbeddedServer extends AbstractLDAPTest {

    public LDAPEmbeddedServer() {
        super();
    }

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    public void importLDIF(String fileName) throws Exception {
        super.importLDIF(fileName);
    }

}