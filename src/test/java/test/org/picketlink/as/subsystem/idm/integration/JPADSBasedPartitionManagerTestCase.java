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
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.picketlink.idm.credential.Credentials.Status;

/**
 * @author pedroigor
 */
@RunWith(Arquillian.class)
public class JPADSBasedPartitionManagerTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = ShrinkWrap
                                        .create(WebArchive.class, "test.war")
                                        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                                        .addAsManifestResource(
                                                                      JPADSBasedPartitionManagerTestCase.class.getClassLoader().getResource(
                                                                                                                                                   "deployment/jboss-deployment-structure-idm.xml"), "jboss-deployment-structure.xml")
                                        .addClass(JPADSBasedPartitionManagerTestCase.class);

        return deployment;
    }

    @Resource(mappedName = "picketlink/JPADSBasedPartitionManager")
    private PartitionManager jpaDSBasedPartitionManager;

    @Test
    public void testPartitionManager() throws Exception {
        Realm defaultRealm = this.jpaDSBasedPartitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (defaultRealm == null) {
            defaultRealm = new Realm(Realm.DEFAULT_REALM);

            this.jpaDSBasedPartitionManager.add(defaultRealm);
        }

        IdentityManager identityManager = this.jpaDSBasedPartitionManager.createIdentityManager();

        User user = BasicModel.getUser(identityManager, "johny");

        if (user != null) {
            identityManager.remove(user);
        }

        user = new User("johny");

        identityManager.add(user);

        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Status.VALID, credentials.getStatus());

        Role role = BasicModel.getRole(identityManager, "admin");

        if (role != null) {
            identityManager.remove(role);
        }

        role = new Role("admin");

        identityManager.add(role);

        RelationshipManager relationshipManager = this.jpaDSBasedPartitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        user = BasicModel.getUser(identityManager, "johny");
        role = BasicModel.getRole(identityManager, "admin");

        assertTrue(BasicModel.hasRole(relationshipManager, user, role));

        user.setAttribute(new Attribute<String>("testAttribute", "value"));

        identityManager.update(user);

        Thread.sleep(1000);

        user = BasicModel.getUser(identityManager, "johny");

        assertNotNull(user.getAttribute("testAttribute"));
        assertEquals("value", user.getAttribute("testAttribute").getValue());
    }
}
