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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.internal.DefaultIdentityManagerFactory;

/**
 * <p>
 * {@link Bean} implementation to define/customize the behaviour for {@link IdentityManagerFactory} instances.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
public class IdentityManagerFactoryBeanDefinition implements Bean<IdentityManagerFactory> {

    private BeanManager beanManager;
    private InjectionTarget<IdentityManagerFactory> injectionTarget;
    private IdentityConfiguration configuration;

    @SuppressWarnings("unchecked")
    public IdentityManagerFactoryBeanDefinition(BeanManager beanManager) {
        this.beanManager = beanManager;
        AnnotatedType<? extends IdentityManagerFactory> at = this.beanManager.createAnnotatedType(DefaultIdentityManagerFactory.class);
        this.injectionTarget = (InjectionTarget<IdentityManagerFactory>) beanManager.createInjectionTarget(at);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.context.spi.Contextual#create(javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public IdentityManagerFactory create(CreationalContext<IdentityManagerFactory> creationalContext) {
        this.configuration = resolveIdentityConfiguration();
        
        IdentityManagerFactory identity = this.configuration.buildIdentityManagerFactory();
        
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
    public void destroy(IdentityManagerFactory instance, CreationalContext<IdentityManagerFactory> creationalContext) {
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

        types.add(IdentityManagerFactory.class);

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
        return DefaultIdentityManagerFactory.class;
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
     * <p>
     * Resolves the {@link IdentityConfiguration} instance to be used during the {@link PicketBoxManager} creation.
     * </p>
     * 
     * @return
     */
    @SuppressWarnings({ "unchecked", "serial" })
    private IdentityConfiguration resolveIdentityConfiguration() {
        Set<Bean<?>> beans = this.beanManager.getBeans(IdentityConfiguration.class, new AnnotationLiteral<Any>() {
        });

        if (beans.isEmpty()) {
            throw new IllegalStateException(
                    "No IdentityConfiguration provided. Maybe you forgot to provide a @Producer method for the IdentityConfiguration.");
        }

        Bean<IdentityConfiguration> bean = (Bean<IdentityConfiguration>) beans.iterator().next();

        CreationalContext<IdentityConfiguration> createCreationalContext = this.beanManager.createCreationalContext(bean);

        return bean.create(createCreationalContext);
    }

}