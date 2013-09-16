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
import test.org.picketlink.as.subsystem.module.idm.SaleAgent;

import javax.annotation.Resource;

import static org.junit.Assert.*;
import static org.picketlink.idm.credential.Credentials.*;

/**
 *
 * @author pedroigor
 */
@RunWith(Arquillian.class)
public class JPAEntityModuleBasedPartitionManagerTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(JPAEntityModuleBasedPartitionManagerTestCase.class.getClassLoader().getResource("deployment/emf-jndi-persistence.xml"), "META-INF/persistence.xml")
                .addAsManifestResource(JPAEntityModuleBasedPartitionManagerTestCase.class.getClassLoader().getResource("deployment/jboss-deployment-structure-idm.xml"), "jboss-deployment-structure.xml")
                .addClass(JPAEntityModuleBasedPartitionManagerTestCase.class)
                .addClass(SaleAgent.class);

        return deployment;
    }

    @Resource(mappedName="picketlink/JPACustomEntityBasedPartitionManager")
    private PartitionManager jpaCustomEntityBasedPartitionManager;

    @Test
    public void testPartitionManager() throws Exception {
        Realm defaultRealm = this.jpaCustomEntityBasedPartitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (defaultRealm == null) {
            defaultRealm = new Realm(Realm.DEFAULT_REALM);

            this.jpaCustomEntityBasedPartitionManager.add(defaultRealm);
        }

        IdentityManager identityManager = this.jpaCustomEntityBasedPartitionManager.createIdentityManager();

        SaleAgent user = new SaleAgent("johny");

        identityManager.add(user);

        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Status.VALID, credentials.getStatus());

        Role role = new Role("admin");

        identityManager.add(role);

        RelationshipManager relationshipManager = this.jpaCustomEntityBasedPartitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        user = identityManager.createIdentityQuery(SaleAgent.class).setParameter(SaleAgent.LOGIN_NAME, "johny").getResultList().get(0);
        role = BasicModel.getRole(identityManager, "admin");

        assertTrue(BasicModel.hasRole(relationshipManager, user, role));

        user.setAttribute(new Attribute<String>("testAttribute", "value"));

        identityManager.update(user);

        Thread.sleep(1000);

        user = identityManager.createIdentityQuery(SaleAgent.class).setParameter(SaleAgent.LOGIN_NAME, "johny").getResultList().get(0);

        assertNotNull(user.getAttribute("testAttribute"));
        assertEquals("value", user.getAttribute("testAttribute").getValue());
    }

}
