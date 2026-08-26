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
  
