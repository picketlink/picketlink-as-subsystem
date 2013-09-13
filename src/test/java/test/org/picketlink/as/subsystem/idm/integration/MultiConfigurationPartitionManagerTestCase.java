package test.org.picketlink.as.subsystem.idm.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;
import test.org.picketlink.as.subsystem.module.idm.SaleAgent;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 *
 * @author pedroigor
 */
@RunWith(Arquillian.class)
@Ignore
public class MultiConfigurationPartitionManagerTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(MultiConfigurationPartitionManagerTestCase.class.getClassLoader().getResource("deployment/jboss-deployment-structure-idm.xml"), "jboss-deployment-structure.xml")
                .addClass(MultiConfigurationPartitionManagerTestCase.class)
                .addClass(SaleAgent.class);

        return deployment;
    }

    @Resource(mappedName="picketlink/MultiConfigBasedPartitionManager")
    private PartitionManager partitionManager;

    @Test
    public void testPartitionManager() throws Exception {
        Realm defaultRealm = this.partitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (defaultRealm == null) {
            defaultRealm = new Realm(Realm.DEFAULT_REALM);

            this.partitionManager.add(defaultRealm);
        }

        IdentityManager identityManager = this.partitionManager.createIdentityManager();

        User user = new User("johny");

        identityManager.add(user);

        Password password = new Password("abcd1234");

//        identityManager.updateCredential(user, password);
//
//        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);
//
//        identityManager.validateCredentials(credentials);
//
//        assertEquals(Status.VALID, credentials.getStatus());

        Role role = new Role("admin");

        identityManager.add(role);

        RelationshipManager relationshipManager = this.partitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        user = BasicModel.getUser(identityManager, "johny");
        role = BasicModel.getRole(identityManager, "admin");

        Thread.sleep(1000);

        assertTrue(BasicModel.hasRole(relationshipManager, user, role));
    }

}
