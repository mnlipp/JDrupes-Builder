---
description: Verify that the implementations follow some general rules
agent: build
---

- **Up-to-date logging:** Every ResourceProvider that has up-to-date
  caching logic (i.e. checks whether resources need regeneration before
  doing work) must log a message at level `fine` or higher when it finds
  that the resources it should provide are up to date. Add respective
  logging if missing. Pattern:
  `logger.atFine().log("Output from %s is up to date", this)` or
  `logger.atFine().log("%s found tag %s to be up to date", this, resource)`.
  
 - **Evaluate for most special type:** Every ResourceProvider may only
   provide resources of the type that it actually provides. E.g. if
   a provider provides resources of ClassTreeType and gets a request
   for resources of a base type (or super type) of ClassTreeType, it
   must "upgrade" the request following this pattern:
   ```java
        if (requested.accepts(ClassTreeType)
            && !requested.isFor(ClassTreeType)) {
            @SuppressWarnings("unchecked")
            var result = (Collection<T>) resources(of(ClassTreeType)).toList();
            return result;
        }
   ```
   Look at the implementation of doProvide to find out what resource type
   a ResourceProvider really provides. The type cannot be derived from the
   generic return type of doProvide.
   