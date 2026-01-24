/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Intent;
import static org.jdrupes.builder.api.Intent.Consume;
import static org.jdrupes.builder.api.Intent.Expose;
import static org.jdrupes.builder.api.Intent.Supply;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ProviderSelection;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;

/// The Class DefaultBoundResourceQuery.
///
public class DefaultProviderSelection implements ProviderSelection {

    private final AbstractProject project;
    private final Set<Intent> preSelected;
    private Predicate<ResourceProvider> filter = _ -> true;
    private Consumer<ResourceProvider> onBeforeUse = _ -> {
    };

    /* default */ DefaultProviderSelection(AbstractProject project) {
        this.project = project;
        this.preSelected = null;
    }

    /* default */ DefaultProviderSelection(AbstractProject project,
            Set<Intent> intends) {
        this.project = project;
        this.preSelected = intends;
    }

    @Override
    public ProviderSelection filter(Predicate<ResourceProvider> filter) {
        this.filter = this.filter.and(filter);
        return this;
    }

    @Override
    public DefaultProviderSelection without(ResourceProvider provider) {
        filter = filter.and(p -> !p.equals(provider));
        return this;
    }

    @Override
    public DefaultProviderSelection
            without(Class<? extends ResourceProvider> providerType) {
        filter = filter.and(p -> !providerType.isAssignableFrom(p.getClass()));
        return this;
    }

    @Override
    public DefaultProviderSelection
            onBeforeUse(Consumer<ResourceProvider> hook) {
        this.onBeforeUse = hook;
        return this;
    }

    @Override
    public Stream<ResourceProvider> select(Set<Intent> intents) {
        if (preSelected != null) {
            if (!intents.isEmpty()) {
                throw new IllegalArgumentException(
                    "Duplicates selection of intents.");
            }
            intents = preSelected;
        }
        return project.dependencies(intents).filter(filter)
            .peek(onBeforeUse::accept);
    }

    @Override
    public <T extends Resource> Stream<T>
            resources(ResourceRequest<T> requested) {
        AtomicReference<ResourceRequest<T>> projectRequest
            = new AtomicReference<>();
        AtomicReference<ResourceRequest<T>> othersRequest
            = new AtomicReference<>();
        return select(requested.uses()).map(p -> {
            if (p instanceof Project) {
                return project.context().resources(p,
                    projectRequest.updateAndGet(
                        r -> r != null ? r : forwardedRequest(requested)));
            } else {
                return project.context().resources(p,
                    othersRequest.updateAndGet(
                        // Clean using, ordinary providers don't care
                        r -> r != null ? r
                            : requested.using(EnumSet.noneOf(Intent.class))));
            }
        }).flatMap(s -> s);
    }

    private <T extends Resource> ResourceRequest<T>
            forwardedRequest(ResourceRequest<T> requested) {
        Set<Intent> uses = preSelected != null ? preSelected : requested.uses();
        Set<Intent> mapped = EnumSet.copyOf(uses);
        if (uses.stream().filter(
            EnumSet.of(Consume, Expose)::contains).findAny().isPresent()) {
            mapped.remove(Consume);
            mapped.addAll(EnumSet.of(Supply, Expose));
        }
        return requested.using(mapped);
    }
}
