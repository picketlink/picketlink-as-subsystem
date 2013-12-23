package test.org.picketlink.as.subsystem.federation.integration;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * <p> Integration Test to verify if an IDP configured using the subsystem is working as expected. See
 * <b>src/test/resources/picketlink-subsystem.xml.</b> </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 9, 2012
 */
public class FederationWithBadConfigurationTestCase extends AbstractIntegrationTests {

    @Deployment(name = "idp-bad", testable = false)
    @TargetsContainer("jboss-as7")
    public static WebArchive createIDPBadDeployment() {
        return createIdentityProviderWebArchive("idp-bad.war");
    }

    @Deployment(name = "sales-bad", testable = false)
    @TargetsContainer("jboss-as7")
    public static WebArchive createSalesBadDeployment() {
        return createServiceProviderWebArchive("sales-bad.war");
    }

    @Test
    @OperateOnDeployment("sales-bad")
    public void testSalesBad() throws Exception {
        HtmlPage welcomePage = login(new WebClient());

        HtmlElement customErrorMessage = welcomePage.getElementById("customErrorMessage");

        assertNotNull("Custom error page not reached.", customErrorMessage);
    }
}
