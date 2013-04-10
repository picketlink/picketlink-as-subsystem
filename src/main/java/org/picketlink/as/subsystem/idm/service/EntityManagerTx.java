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
package org.picketlink.as.subsystem.idm.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EntityManagerTx implements InvocationHandler {

    private final EntityManager em;

    public EntityManagerTx(EntityManager em) {
        this.em = em;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        UserTransaction tx = null;

        if (isTxRequired(method, args)) {
            tx = getUserTransaction();

            if (tx.getStatus() == Status.STATUS_NO_TRANSACTION) {
                tx.begin();
            } else {
                tx = null;
            }

            em.joinTransaction();
        }

        try {
            return method.invoke(em, args);
        } finally {
            if (tx != null) {
                tx.commit();
            }
        }
    }

    private boolean isTxRequired(Method method, Object[] args) {
        String n = method.getName();
        if (n.equals("flush")) {
            return true;
        } else if (n.equals("getLockMode")) {
            return true;
        } else if (n.equals("lock")) {
            return true;
        } else if (n.equals("merge")) {
            return true;
        } else if (n.equals("persist")) {
            return true;
        } else if (n.equals("refresh")) {
            return true;
        } else if (n.equals("remove")) {
            return true;
        } else {
            return false;
        }
    }

    private UserTransaction getUserTransaction() {
        try {
            return (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
        } catch (NamingException e) {
            throw new PersistenceException(e);
        }
    }

}
