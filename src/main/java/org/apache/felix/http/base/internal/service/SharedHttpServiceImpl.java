/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.holder.FilterHolder;
import org.apache.felix.http.base.internal.handler.holder.HttpServiceServletHolder;
import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.NamespaceException;

public final class SharedHttpServiceImpl
{
    private final HandlerRegistry handlerRegistry;

    private final Map<String, ServletHolder> aliasMap = new HashMap<String, ServletHolder>();

    public SharedHttpServiceImpl(final HandlerRegistry handlerRegistry)
    {
        if (handlerRegistry == null)
        {
            throw new IllegalArgumentException("HandlerRegistry cannot be null!");
        }

        this.handlerRegistry = handlerRegistry;
    }

    /**
     * Register a filter
     */
    public boolean registerFilter(@Nonnull final FilterHolder holder)
    {
        this.handlerRegistry.addFilter(holder);
        return true;
    }

    /**
     * Register a servlet
     */
    public void registerServlet(@Nonnull final String alias,
            @Nonnull final ExtServletContext httpContext,
            @Nonnull final Servlet servlet,
            @Nonnull final ServletInfo servletInfo) throws ServletException, NamespaceException
    {
        final ServletHolder holder = new HttpServiceServletHolder(0, httpContext, servletInfo, servlet);

        synchronized (this.aliasMap)
        {
            if (this.aliasMap.containsKey(alias))
            {
                throw new NamespaceException("Alias " + alias + " is already in use.");
            }
            this.handlerRegistry.addServlet(holder);

            this.aliasMap.put(alias, holder);
        }
    }

    /**
     * @see org.osgi.service.http.HttpService#unregister(java.lang.String)
     */
    public Servlet unregister(final String alias)
    {
        synchronized (this.aliasMap)
        {
            final ServletHolder holder = this.aliasMap.remove(alias);
            if (holder == null)
            {
                throw new IllegalArgumentException("Nothing registered at " + alias);
            }

            final Servlet s = holder.getServlet();
            this.handlerRegistry.removeServlet(0, holder.getServletInfo(), true);
            return s;
        }
    }

    public void unregisterServlet(final Servlet servlet, final boolean destroy)
    {
        if (servlet != null)
        {
            synchronized (this.aliasMap)
            {
                final Iterator<Map.Entry<String, ServletHolder>> i = this.aliasMap.entrySet().iterator();
                while (i.hasNext())
                {
                    final Map.Entry<String, ServletHolder> entry = i.next();
                    if (entry.getValue().getServlet() == servlet)
                    {
                        this.handlerRegistry.removeServlet(0, entry.getValue().getServletInfo(), destroy);

                        i.remove();
                        break;
                    }

                }
            }
        }
    }

    public void unregisterFilter(final FilterHolder filter, final boolean destroy)
    {
        if (filter != null)
        {
            this.handlerRegistry.removeFilter(filter.getContextServiceId(), filter.getFilterInfo(), destroy);
        }
    }
}