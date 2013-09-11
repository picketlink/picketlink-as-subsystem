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

package org.picketlink.as.subsystem.idm;

import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.internal.DefaultPartitionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.picketlink.as.subsystem.PicketLinkMessages.*;

/**
 * <p> {@link Bean} implementation to define and customize the behaviour for {@link IdentityManagerFactory} instances.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PartitionManagerBeanDefinition implements Bean<PartitionManager> {

    private BeanManager beanManager;
    private InjectionTarget<PartitionManager> injectionTarget;
    private List<IdentityConfiguration> configuration;

    @SuppressWarnings("unchecked")
    public PartitionManagerBeanDefinition(BeanManager beanManager) {
        this.beanManager = beanManager;
        AnnotatedType<? extends PartitionManager> at = this.beanManager.createAnnotatedType(DefaultPartitionManager.class);
        this.injectionTarget = (InjectionTarget<PartitionManager>) beanManager.createInjectionTarget(at);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.context.spi.Contextual#create(javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public PartitionManager create(CreationalContext<PartitionManager> creationalContext) {
        this.configuration = resolveIdentityConfiguration();

        PartitionManager identity = new DefaultPartitionManager(this.configuration);

        this.injectionTarget.inject(identity, creationalContext);
        this.injectionTarget.postConstruct(identity);

        return identity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.context.spi.Contextual#destroy(java.lang.Object, javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public void destroy(PartitionManager instance, CreationalContext<PartitionManager> creationalContext) {
        this.injectionTarget.preDestroy(instance);
        this.injectionTarget.dispose(instance);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getTypes()
     */
    @Override
    public Set<Type> getTypes() {
        Set<Type> types = new HashSet<Type>();

        types.add(PartitionManager.class);

        return types;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getQualifiers()
     */
    @SuppressWarnings("serial")
    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> qualifiers = new HashSet<Annotation>();

        qualifiers.add(new AnnotationLiteral<Default>() {
        });
        qualifiers.add(new AnnotationLiteral<Any>() {
        });

        return qualifiers;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getScope()
     */
    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getName()
     */
    @Override
    public String getName() {
        return "IdentityManagerFactory";
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getStereotypes()
     */
    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getBeanClass()
     */
    @Override
    public Class<?> getBeanClass() {
        return DefaultPartitionManager.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#isAlternative()
     */
    @Override
    public boolean isAlternative() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#isNullable()
     */
    @Override
    public boolean isNullable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getInjectionPoints()
     */
    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return this.injectionTarget.getInjectionPoints();
    }

    /**
     * <p> Resolves the {@link IdentityConfiguration} instance to be used during the {@link PicketBoxManager} creation.
     * </p>
     *
     * @return
     */
    @SuppressWarnings({"unchecked", "serial"})
    private List<IdentityConfiguration> resolveIdentityConfiguration() {
        List<IdentityConfiguration> configurations = new ArrayList<IdentityConfiguration>();

        Set<Bean<?>> beans = this.beanManager.getBeans(IdentityConfiguration.class, new AnnotationLiteral<Any>() {});

        if (beans.isEmpty()) {
            throw MESSAGES.idmNoConfigurationProvided();
        }

        for (Bean<?> bean : beans) {
            Bean<IdentityConfiguration> configBean = (Bean<IdentityConfiguration>) bean;
            CreationalContext<IdentityConfiguration> createCreationalContext = this.beanManager.createCreationalContext(configBean);
            configurations.add(configBean.create(createCreationalContext));

        }

        return configurations;
    }

}