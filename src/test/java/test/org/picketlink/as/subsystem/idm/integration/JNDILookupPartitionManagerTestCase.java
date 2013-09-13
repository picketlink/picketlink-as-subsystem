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
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
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
public class JNDILookupPartitionManagerTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(JNDILookupPartitionManagerTestCase.class.getClassLoader().getResource("deployment/emf-jndi-persistence.xml"), "META-INF/persistence.xml")
                .addAsManifestResource(JNDILookupPartitionManagerTestCase.class.getClassLoader().getResource("deployment/jboss-deployment-structure-idm.xml"), "jboss-deployment-structure.xml")
                .addClass(JNDILookupPartitionManagerTestCase.class)
                .addClass(SaleAgent.class);

        return deployment;
    }

    @Resource(mappedName="picketlink/FileBasedPartitionManager")
    private PartitionManager fileBasedPartitionManager;

    @Resource(mappedName="picketlink/JPADSBasedPartitionManager")
    private PartitionManager jpaDSBasedPartitionManager;

    @Resource(mappedName="picketlink/JPAEMFBasedPartitionManager")
    private PartitionManager jpaEMFBasedPartitionManager;

    @Resource(mappedName="picketlink/JPACustomEntityBasedPartitionManager")
    private PartitionManager jpaCustomEntityBasedPartitionManager;

//    @Resource(mappedName="picketlink/LDAPBasedPartitionManager")
//    private PartitionManager ldapBasedPartitionManager;

    @Test
    public void testFileBasedPartitionManager() throws Exception {
        Realm defaultRealm = this.fileBasedPartitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (defaultRealm == null) {
            defaultRealm = new Realm(Realm.DEFAULT_REALM);

            this.fileBasedPartitionManager.add(defaultRealm);
        }

        IdentityManager identityManager = this.fileBasedPartitionManager.createIdentityManager();

        User user = new User("johny");

        identityManager.add(user);

        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Status.VALID, credentials.getStatus());

        Role role = new Role("admin");

        identityManager.add(role);

        RelationshipManager relationshipManager = this.fileBasedPartitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        user = BasicModel.getUser(identityManager, "johny");
        role = BasicModel.getRole(identityManager, "admin");

        Thread.sleep(1000);

        assertTrue(BasicModel.hasRole(relationshipManager, user, role));
    }

    @Test
    public void testJPADSBasedPartitionManager() throws Exception {
        Realm defaultRealm = this.jpaDSBasedPartitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (defaultRealm == null) {
            defaultRealm = new Realm(Realm.DEFAULT_REALM);

            this.jpaDSBasedPartitionManager.add(defaultRealm);
        }

        IdentityManager identityManager = this.jpaDSBasedPartitionManager.createIdentityManager();

        User user = new User("johny");

        identityManager.add(user);

        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Status.VALID, credentials.getStatus());

        Role role = new Role("admin");

        identityManager.add(role);

        RelationshipManager relationshipManager = this.jpaDSBasedPartitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        user = BasicModel.getUser(identityManager, "johny");
        role = BasicModel.getRole(identityManager, "admin");

        assertTrue(BasicModel.hasRole(relationshipManager, user, role));
    }

    @Test
    public void testJPAEMFBasedPartitionManager() throws Exception {
        Realm defaultRealm = this.jpaEMFBasedPartitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (defaultRealm == null) {
            defaultRealm = new Realm(Realm.DEFAULT_REALM);

            this.jpaEMFBasedPartitionManager.add(defaultRealm);
        }

        IdentityManager identityManager = this.jpaEMFBasedPartitionManager.createIdentityManager();

        User user = new User("johny");

        identityManager.add(user);

        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Status.VALID, credentials.getStatus());

        Role role = new Role("admin");

        identityManager.add(role);

        RelationshipManager relationshipManager = this.jpaEMFBasedPartitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        user = BasicModel.getUser(identityManager, "johny");
        role = BasicModel.getRole(identityManager, "admin");

        assertTrue(BasicModel.hasRole(relationshipManager, user, role));
    }

    @Test
    public void testJPACustomEntityBasedPartitionManager() throws Exception {
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
    }

//    @Test
//    public void testLDAPBasedPartitionManager() throws Exception {
//        IdentityManager identityManager = this.ldapBasedPartitionManager.createIdentityManager();
//
//        User user = new User("johny");
//
//        identityManager.add(user);
//
//        Password password = new Password("abcd1234");
//
//        identityManager.updateCredential(user, password);
//
//        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);
//
//        identityManager.validateCredentials(credentials);
//
//        assertEquals(Status.VALID, credentials.getStatus());
//    }

}
