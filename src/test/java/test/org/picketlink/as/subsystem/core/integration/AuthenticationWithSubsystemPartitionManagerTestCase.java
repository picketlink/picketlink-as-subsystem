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
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.annotation.Resource;
import javax.inject.Inject;

import static org.junit.Assert.assertTrue;

/**
 * @author pedroigor
 */
@RunWith(Arquillian.class)
public class AuthenticationWithSubsystemPartitionManagerTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
                   .create(WebArchive.class, "test.war")
                   .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                   .addAsWebInfResource(AuthenticationWithEmbeddedPartitionManagerTestCase.class.getClassLoader().getResource("deployment/web-idm-resource-ref.xml"), "web.xml")
                   .addAsManifestResource(AuthenticationWithSubsystemPartitionManagerTestCase.class.getClassLoader().getResource("deployment/jboss-deployment-structure-core.xml"), "jboss-deployment-structure.xml")
                   .addClass(AuthenticationWithSubsystemPartitionManagerTestCase.class);
    }

    @Resource(mappedName = "picketlink/FileBasedPartitionManager")
    private PartitionManager partitionManager;

    @Inject
    private Identity identity;

    @Inject
    private IdentityManager identityManager;

    @Inject
    private RelationshipManager relationshipManager;

    @Inject
    private DefaultLoginCredentials credentials;

    @Before
    public void onBefore() throws Exception {
        Realm defaultRealm = this.partitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (defaultRealm != null) {
            this.partitionManager.remove(defaultRealm);
        }

        this.partitionManager.add(new Realm(Realm.DEFAULT_REALM));
    }

    @Test
    public void testAuthentication() throws Exception {
        User user = new User("johny");

        this.identityManager.add(user);

        Password password = new Password("abcd1234");

        this.identityManager.updateCredential(user, password);

        Role role = new Role("admin");

        this.identityManager.add(role);

        BasicModel.grantRole(this.relationshipManager, user, role);

        this.credentials.setUserId("johny");
        this.credentials.setPassword("abcd1234");

        this.identity.login();

        assertTrue(this.identity.isLoggedIn());

        IdentityManager identityManager = this.partitionManager.createIdentityManager();
        RelationshipManager relationshipManager = this.partitionManager.createRelationshipManager();

        Thread.sleep(1000);

        user = BasicModel.getUser(identityManager, "johny");
        role = BasicModel.getRole(identityManager, "admin");

        assertTrue(BasicModel.hasRole(relationshipManager, user, role));
    }
}
