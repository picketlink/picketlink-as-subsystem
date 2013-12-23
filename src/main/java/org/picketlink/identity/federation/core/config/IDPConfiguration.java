/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.picketlink.identity.federation.core.config;

import org.picketlink.config.federation.IDPType;
import org.picketlink.config.federation.TrustType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p> This class is responsible to store all information about a given Identity Provider deployment. The state is populated with values from the
 * subsystem configuration. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 12, 2012
 */
public class IDPConfiguration extends IDPType implements ProviderConfiguration {

    private String federationAlias;
    private String alias;
    private String securityDomain;
    private final Map<String, String> trustDomainAlias = new HashMap<String, String>();

    public IDPConfiguration() {
        this.setTrust(new TrustType());
        this.getTrust().setDomains("");
    }

    @Override
    public String getAlias() {
        return this.alias;
    }

    @Override
    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public void setSecurityDomain(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void addTrustDomain(String domain, String certAlias) {
        if (this.getTrust().getDomains() != null && this.getTrust().getDomains().indexOf(domain) == -1) {
            if (!this.getTrust().getDomains().isEmpty()) {
                this.getTrust().setDomains(this.getTrust().getDomains() + ",");
            }

            this.getTrust().setDomains(this.getTrust().getDomains() + domain);
        }

        if (certAlias != null && !certAlias.trim().isEmpty()) {
            this.trustDomainAlias.put(domain, certAlias);
        } else {
            this.trustDomainAlias.put(domain, domain);
        }
    }

    public void removeTrustDomain(String domain) {
        if (getTrust().getDomains() == null) {
            this.getTrust().setDomains("");
        }

        if (getTrust().getDomains() != null && !getTrust().getDomains().isEmpty()) {
            String[] domains = this.getTrust().getDomains().split(",");

            getTrust().setDomains("");

            for (String currentDomain : domains) {
                if (!domain.equals(currentDomain) && !"".equals(currentDomain.trim())) {
                    getTrust().setDomains(currentDomain + ",");
                }
            }
        }
    }

    public Map<String, String> getTrustDomainAlias() {
        return Collections.unmodifiableMap(this.trustDomainAlias);
    }
}
