package org.picketlink.as.subsystem.idm.config;

import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.config.IdentityStoresConfigurationBuilder;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.config.JPAStoreConfigurationBuilder;

import javax.transaction.TransactionManager;

/**
 * Created with IntelliJ IDEA. User: pedroigor Date: 9/14/13 Time: 10:35 PM To change this template use File | Settings
 * | File Templates.
 */
public class JPAStoreSubsystemConfigurationBuilder extends JPAStoreConfigurationBuilder {

    private String entityMoudule;
    private String entityModuleUnitName;
    private String dataSourceJndiUrl;
    private String entityManagerFactoryJndiName;
    private InjectedValue<TransactionManager> transactionManager;

    public JPAStoreSubsystemConfigurationBuilder(final IdentityStoresConfigurationBuilder builder) {
        super(builder);
    }

    public JPAStoreSubsystemConfigurationBuilder entityModule(String entityModule) {
        this.entityMoudule = entityModule;
        return this;
    }

    public JPAStoreSubsystemConfigurationBuilder entityModuleUnitName(String entityModuleUnitName) {
        this.entityModuleUnitName = entityModuleUnitName;
        return this;
    }

    public JPAStoreSubsystemConfigurationBuilder dataSourceJndiUrl(String dataSourceJndiUrl) {
        this.dataSourceJndiUrl = dataSourceJndiUrl;
        return this;
    }

    public JPAStoreSubsystemConfigurationBuilder entityManagerFactoryJndiName(String entityManagerFactoryJndiName) {
        this.entityManagerFactoryJndiName = entityManagerFactoryJndiName;
        return this;
    }

    public JPAStoreSubsystemConfigurationBuilder transactionManager(InjectedValue<TransactionManager> transactionManager) {
        this.transactionManager = transactionManager;
        return this;
    }

    @Override
    protected JPAIdentityStoreConfiguration create() {
        return new JPAStoreSubsystemConfiguration(
                this.entityMoudule,
                this.entityModuleUnitName,
                this.dataSourceJndiUrl,
                this.entityManagerFactoryJndiName,
                this.transactionManager,
                getMappedEntities(),
                getSupportedTypes(),
                getUnsupportedTypes(),
                getContextInitializers(),
                getCredentialHandlerProperties(),
                getCredentialHandlers(),
                isSupportAttributes(),
                isSupportCredentials());
    }

    @Override
    protected void validate() {
        super.validate();
    }
}
