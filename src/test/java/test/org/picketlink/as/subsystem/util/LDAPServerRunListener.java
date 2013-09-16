package test.org.picketlink.as.subsystem.util;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.picketbox.test.ldap.AbstractLDAPTest;

/**
 * <p>Used to start an embedded LDAP server before the tests execution.</p>
 *
 * @author Pedro Igor
 */
public class LDAPServerRunListener extends RunListener {

    private LDAPEmbeddedServer embeddedServer = new LDAPEmbeddedServer();

    @Override
    public void testRunStarted(final Description description) throws Exception {
        super.testRunStarted(description);
        this.embeddedServer.setup();
        this.embeddedServer.importLDIF("ldap/users.ldif");
    }

    @Override
    public void testRunFinished(final Result result) throws Exception {
        super.testRunFinished(result);
        this.embeddedServer.tearDown();
    }

    private class LDAPEmbeddedServer extends AbstractLDAPTest {

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

}
