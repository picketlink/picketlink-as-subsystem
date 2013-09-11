package test.org.picketlink.as.subsystem.core.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.Identity;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.inject.Inject;

import static org.junit.Assert.*;

/**
 *
 * @author pedroigor
 */
@RunWith(Arquillian.class)
public class AuthenticationWithEmbeddedPartitionManagerTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(AuthenticationWithEmbeddedPartitionManagerTestCase.class.getClassLoader().getResource("deployment/jboss-deployment-structure-core.xml"), "jboss-deployment-structure.xml")
                .addClass(AuthenticationWithEmbeddedPartitionManagerTestCase.class);

        return deployment;
    }

    @Inject
    private IdentityManager identityManager;

    @Inject
    private RelationshipManager relationshipManager;

    @Inject
    private Identity identity;

    @Inject
    private DefaultLoginCredentials credentials;

    @Before
    public void onBefore() throws Exception {
        User user = new User("johny");

        this.identityManager.add(user);

        Password password = new Password("abcd1234");

        this.identityManager.updateCredential(user, password);

        Role role = new Role("admin");

        this.identityManager.add(role);

        BasicModel.grantRole(this.relationshipManager, user, role);
    }

    @Test
    public void testAuthentication() {
        this.credentials.setUserId("johny");
        this.credentials.setPassword("abcd1234");

        this.identity.login();

        assertTrue(this.identity.isLoggedIn());

        User user = BasicModel.getUser(this.identityManager, "johny");
        Role role = BasicModel.getRole(this.identityManager, "admin");

        assertTrue(BasicModel.hasRole(this.relationshipManager, user, role));
    }

}
