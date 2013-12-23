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

import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p> Parses a subsystem configuration considering all domain model elements. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 9, 2012
 */
public class ParseCompleteDomainModelTestCase extends AbstractPicketLinkSubsystemTestCase {

    /**
     * Tests that the xml is parsed into the correct operations.
     */
    @Test
    public void testParseSubsystem() throws Exception {
        Assert.assertNotNull("No operations found. Check if the XML used is valid.", super.parse(getValidSubsystemXML()));
    }

    /**
     * Tests that the xml installs properly into the controller.
     *
     * @throws Exception
     */
    @Test
    public void testInstallIntoController() throws Exception {
        ModelNode model = getResultingModelNode();

        Assert.assertNotNull("Expected a populated model.", model);
        Assert.assertNotNull("ModelNode instance is not defined.", model.isDefined());
    }

    @Override
    protected String getSubsystemXmlFileName() {
        return "picketlink-subsystem.xml";
    }
}
