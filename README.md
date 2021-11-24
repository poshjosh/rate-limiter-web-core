# rate limiter web core

Light-weight rate limiting library for java web apps, based on https://github.com/poshjosh/rate-limiter

## Rate limiting a java web application

There are 2 ways to rate limit a web application:

### 1. Use the `@RateLimit` annotation

__Example using Springframework__

```java
import com.looseboxes.ratelimiter.annotation.RateLimit;

@Controller
@RequestMapping("/api")
class GreetingResource {

    // Only 99 calls to this path is allowed per minute
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTES)
    @GetMapping("/greet")
    String greet() {
        return "Hello World";
    }
}
```

__Example using JAX-RS__

```java
import com.looseboxes.ratelimiter.annotation.RateLimit;

@Path("/api")
class GreetingResource {

    // Only 99 calls to this path is allowed per minute
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTES)
    @GET
    @Path("/greet")
    @Produces("text/plan")
    String greet() {
        return "Hello World";
    }
}
```

- The `@RateLimit` annotation may be placed on a super class.

- The `@RateLimit` annotation must be placed together with path related annotations e.g:
Springframeworks's `@RequestMapping`, `@Get` etc or JAX-RS `@Path` etc
  
### 2. Define rate limit properties

Example class that implements the required properties.

```java
package com.example.web;

import com.looseboxes.ratelimiter.rates.Rates;
import com.looseboxes.ratelimiter.web.core.util.RateConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;import com.looseboxes.ratelimiter.web.core.util.RateLimitConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RateLimitPropertiesImpl implements RateLimitProperties {

    @Override
    public List<String> getResourcePackages() {
        return Collections.singletonList("com.example.web.resources");
    }

    @Override
    public Map<String, RateLimitConfig> getRateLimitConfigs() {
        return Collections.singletonMap("default", getRateLimitConfigList());
    }

    private RateLimitConfig getRateLimitConfigList() {
        RateLimitConfig rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setLimits(getRateLimits());
        rateLimitConfig.setLogic(Rates.Logic.OR);
        return rateLimitConfig;
    }

    private List<RateConfig> getRateLimits() {
        RateConfig config = new RateConfig();
        config.setLimit(2);
        config.setDuration(1);
        config.setTimeUnit(TimeUnit.MINUTES);
        return Collections.singletonList(config);
    }
}
```

_Make sure this class is available for injection into other resources/beans._

The properties the user defines should be used to create a rate limiter which will be automatically applied to
every request the web application handles. This `RateLimiter` is equally available for injection by the user.

If the user wants to manually use the rate limiter created from the properties, then they need to override
the method `com.looseboxes.ratelimiter.web.core.util.RateLimitProperties#getAuto()` and return `false`.
This way, the rate limiter will __not__ be automatically applied to requests processed by the web application.
The user may then inject the rate limiter where ever it is needed. 


