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
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTES)
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
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class RateLimiterConfigurerImpl implements RateLimiterConfigurer<HttpServletRequest> {

    @Override
    public void configure(RateLimiterConfigurationRegistry<HttpServletRequest> registry) {

        // If you do not register a listener, the default listener throws an exception
        //
        registry.registerRateRecordedListener(rateRecordedEvent -> {
            
            // Handle rate recorded event
            
            // For example log whether a limit was exceeded
            System.out.println("Limit exceeded: " + rateRecordedEvent.isLimitExceeded()); 
        });

        // The default behaviour is to return the relative request URI
        //
        registry.registerRequestToIdConverter(request -> {
            
            // Convert the request to an identity

            // Examples:

            // To rate limit requests identified by a specific header
//            return request.getHeader("<HEADER_NAME>");

            // To rate limit users identified by session ID
//            return request.getSession().getId();

            // If tracking requests by utm_source, then we can rate all users from a particular source 
            return request.getParameter("utm_source");
        });
    }
}
```

## Ways and Means

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
  
- The `@RateLimitGroup` annotation may span multiple class or multiple methods but not both.
  
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
        return Collections.singletonMap("default", getRateLimitConfigList());
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


