/*
 * JDrupes Builder
 * Copyright (C) 2025 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jdrupes.builder.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.jdrupes.builder.api.Proxyable;

/// A [InvocationHandler] that simply forwards all invocations to the
/// proxied object.
///
public class ForwardingHandler implements InvocationHandler {

    private final Object proxied;

    /// Instantiates a new forwarding handler.
    ///
    /// @param proxied the proxied object
    ///
    public ForwardingHandler(Object proxied) {
        this.proxied = proxied;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if ("equals".equals(method.getName())
            && Proxy.isProxyClass(args[0].getClass())
            && args[0] instanceof Proxyable other) {
            return method.invoke(proxied, other.backing());
        }
        try {
            return method.invoke(proxied, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
