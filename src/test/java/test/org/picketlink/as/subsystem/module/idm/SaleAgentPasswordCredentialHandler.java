package test.org.picketlink.as.subsystem.module.idm;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.credential.handler.PasswordCredentialHandler;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.basic.Agent;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.spi.IdentityContext;

import java.util.Collections;
import java.util.List;

/**
 * @author pedroigor
 */
public class SaleAgentPasswordCredentialHandler extends PasswordCredentialHandler {

    @Override
    protected Account getAccount(final IdentityContext context, final UsernamePasswordCredentials credentials) {
        List<? extends Account> result = findByLoginName(context, SaleAgent.class, credentials.getUsername());

        if (result.isEmpty()) {
            return super.getAccount(context, credentials);
        }

        return result.get(0);
    }

    private <A extends Agent> List<A> findByLoginName(IdentityContext context, Class<A> type, String loginName) {
        IdentityManager identityManager = getIdentityManager(context);
        IdentityQuery<A> query = identityManager.createIdentityQuery(type);

        query.setParameter(Agent.LOGIN_NAME, loginName);

        List<A> result = query.getResultList();

        if (result.isEmpty()) {
            return Collections.emptyList();
        }

        return result;
    }
}
