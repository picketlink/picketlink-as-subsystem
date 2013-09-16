package test.org.picketlink.as.subsystem.idm.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.basic.User;
import test.org.picketlink.as.subsystem.module.idm.SaleAgent;

import javax.annotation.Resource;

import static org.junit.Assert.*;
import static org.picketlink.idm.credential.Credentials.*;

/**
 *
 * @author pedroigor
 */
@RunWith(Arquillian.class)
public class LDAPBasedPartitionManagerTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(LDAPBasedPartitionManagerTestCase.class.getClassLoader().getResource("deployment/emf-jndi-persistence.xml"), "META-INF/persistence.xml")
                .addAsManifestResource(LDAPBasedPartitionManagerTestCase.class.getClassLoader().getResource("deployment/jboss-deployment-structure-idm.xml"), "jboss-deployment-structure.xml")
                .addClass(LDAPBasedPartitionManagerTestCase.class)
                .addClass(SaleAgent.class);

        return deployment;
    }

    @Resource(mappedName="picketlink/LDAPBasedPartitionManager")
    private PartitionManager ldapBasedPartitionManager;

    @Test
    public void testPartitionManager() throws Exception {
        IdentityManager identityManager = this.ldapBasedPartitionManager.createIdentityManager();

        User user = new User("mark");

        identityManager.add(user);

        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Status.VALID, credentials.getStatus());
    }

}
