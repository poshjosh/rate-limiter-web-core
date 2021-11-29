# rate limiter web core

Light-weight rate limiting library for java web apps, based on https://github.com/poshjosh/rate-limiter

## Quick Start

__Annotate the resource endpoint you want to rate imit__

```java
import com.looseboxes.ratelimiter.annotation.RateLimit;

@Controller
@RequestMapping("/api")
class GreetingResource {

    // Only 99 calls to this path is allowed per minute
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTES, group="greeting")
    @GetMapping("/greet")
    String greet() {
        return "Hello World";
    }
}
```

__Configure rate limiting__

```java
package com.looseboxes.ratelimiter.web.spring;

import com.looseboxes.ratelimiter.web.core.RateLimiterConfigurationRegistry;
import com.looseboxes.ratelimiter.web.core.RateLimiterConfigurer;
import com.looseboxes.ratelimiter.web.core.util.Matcher;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class RateLimiterConfigurerImpl implements RateLimiterConfigurer<HttpServletRequest> {

    @Override
    public void configure(RateLimiterConfigurationRegistry<HttpServletRequest> registry) {

        //
        // Register rate recorded listeners
        //

        // If you do not register a listener, the default listener throws an exception
        registry.registerRateRecordedListener(rateRecordedEvent -> {

            // Handle rate recorded event

            // For example log whether a limit was exceeded
            System.out.println("Limit exceeded: " + rateRecordedEvent.isLimitExceeded());
        });

        //
        // Register request matchers
        //

        // The default behaviour is to return the relative request URI
        // Here are other examples:

        // Apply these matchers to all rate limiters belonging to this group
        final String rateLimiterGroup = "greeting";

        // Rate limit by utm_source parameter
        registry.registerRequestMatcher(rateLimiterGroup, new Matcher<HttpServletRequest>() {
            @Override
            public boolean matches(HttpServletRequest request) {
                return true;
            }
            @Override
            public Object getId(HttpServletRequest request) {
                return request.getParameter("utm_source");
            }
        });

        // Alternatively, rate limit users from a single source: utm_source=ERRING-SOURCE
        registry.registerRequestMatcher(rateLimiterGroup, new Matcher<HttpServletRequest>() {
            private final String paramName = "utm_source";
            @Override
            public boolean matches(HttpServletRequest request) {
                return "ERRING-SOURCE".equals(request.getParameter(paramName));
            }
            @Override
            public Object getId(HttpServletRequest request) {
                return request.getParameter(paramName);
            }
        });
    }
}
```

## Ways and Means

There are 2 ways to rate limit a web application:

### 1. Use the `@RateLimit` and/or `@RateLimitGroup` annotation

- The `@RateLimit` annotation may be placed on a super class.

- The `@RateLimit` annotation must be placed together with path related annotations e.g:
  Springframeworks's `@RequestMapping`, `@Get` etc or JAX-RS `@Path` etc

- The `@RateLimitGroup` annotation may span multiple class or multiple methods but not both.

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
  
### 2. Define rate limit properties

Example class that implements the required properties.

```java
package com.example.web;

import com.looseboxes.ratelimiter.rates.Logic;
import com.looseboxes.ratelimiter.util.RateConfig;
import com.looseboxes.ratelimiter.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfigList;
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
        return Collections.singletonMap("greeting", getRateLimitConfigList());
    }

    private RateLimitConfig getRateLimitConfigList() {
        RateLimitConfig rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setLimits(getRateLimits());
        rateLimitConfig.setLogic(Logic.OR);
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
every request the web application handles. 



