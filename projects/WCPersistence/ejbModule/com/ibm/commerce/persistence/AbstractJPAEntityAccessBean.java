package com.ibm.commerce.persistence;

/*
 *-----------------------------------------------------------------
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

public abstract class AbstractJPAEntityAccessBean implements Serializable {
	private EntityManager iEntityManager;
	protected Object iEntity;

	public static AbstractJPAEntityAccessBean createAccessBean(Object entity, Class accessBeanClass) {
		try {
			AbstractJPAEntityAccessBean accessBean = (AbstractJPAEntityAccessBean) accessBeanClass.newInstance();
			accessBean.setEntity(entity);
			return accessBean;
		}
		catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Collection createAccessBeanCollection(Collection entities, Class accessBeanClass) {
		Collection collection = new ArrayList(entities.size());
		for (Object entity : entities) {
			collection.add(createAccessBean(entity, accessBeanClass));
		}
		return collection;
	}
	
	public static Collection createEntityCollection(Collection accessBeans) {
		Collection collection = new ArrayList(accessBeans.size());
		for (Object accessBean : accessBeans) {
			collection.add(((AbstractJPAEntityAccessBean) accessBean).getEntity());
		}
		return collection;
	}
	
	protected EntityManager getEntityManager() {
		if (iEntityManager == null) {
			try {
				Context initialContext = new InitialContext();
				EntityManagerProvider entityManagerProvider = (EntityManagerProvider) initialContext.lookup("ejblocal:com.ibm.commerce.persistence.EntityManagerProvider");
				iEntityManager = entityManagerProvider.getEntityManager();
			}
			catch (NamingException e) {
				throw new RuntimeException(e);
			}
		}
		return iEntityManager;
	}
	
	protected AbstractJPAEntityAccessBean createAccessBean(Object entity) {
		return createAccessBean(entity, this.getClass());
	}
	
	protected Enumeration createAccessBeanEnumeration(Collection entities) {
		return new JPAAccessBeanEnumeration(entities, this.getClass());
	}
	
	protected Iterator createAccessBeanIterator(Collection entities) {
		return new JPAAccessBeanIterator(entities, this.getClass());
	}
	
	protected Collection createAccessBeanCollection(Collection entities) {
		return createAccessBeanCollection(entities, this.getClass());
	}
	
	public void remove() {
		try {
			instantiateEntity();
			if (iEntity != null) {
				getEntityManager().remove(iEntity);
			}
		}
		catch (Exception e) {
			if (e instanceof PersistenceException) {
				throw (PersistenceException) e;
			}
			else {
				throw new PersistenceException(e);
			}
		}
	}
	
	public void detach() {
		if (iEntity != null) {
			try {
				//caiduan
//				getEntityManager().refresh(iEntity);
//				getEntityManager().detach(iEntity);
				iEntity = null;
			}
			catch (TransactionRequiredException e) {
				// ignore
			}
		}
	}
	
	protected void setEntity(Object entity) {
		iEntity = entity;
	}
	
	public Object getEntity() {
		return iEntity;
	}
	
	abstract public void instantiateEntity();
	
	private static class JPAAccessBeanEnumeration implements Enumeration {
		private Iterator iEntityIterator;
		private Class iAccessBeanClass;
		
		public JPAAccessBeanEnumeration(Collection entities, Class accessBeanClass) {
			iEntityIterator = entities.iterator();
			iAccessBeanClass = accessBeanClass;
		}
		
		@Override
		public boolean hasMoreElements() {
			return iEntityIterator.hasNext();
		}

		@Override
		public Object nextElement() {
			return createAccessBean(iEntityIterator.next(), iAccessBeanClass);
		}
	}
	
	private static class JPAAccessBeanIterator implements Iterator {
		private Iterator iEntityIterator;
		private Class iAccessBeanClass;
		
		public JPAAccessBeanIterator(Collection entities, Class accessBeanClass) {
			iEntityIterator = entities.iterator();
			iAccessBeanClass = accessBeanClass;
		}

		@Override
		public boolean hasNext() {
			return iEntityIterator.hasNext();
		}

		@Override
		public Object next() {
			return createAccessBean(iEntityIterator.next(), iAccessBeanClass);
		}

		@Override
		public void remove() {
			iEntityIterator.remove();
		}
	}
}
