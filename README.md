# rate limiter web core

Light-weight rate limiting library for java web apps, based on
[rate-limiter](https://github.com/poshjosh/rate-limiter).

Please first read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).

## Quick Start

__Annotate the resource you want to rate imit__

```java
import com.looseboxes.ratelimiter.annotation.RateLimit;

@Controller
@RequestMapping("/api")
class GreetingResource {

    // Only 99 calls to this path is allowed per minute
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTE)
    @RateLimitGroup("limitBySession")
    @GetMapping("/greet")
    String greet() {
        return "Hello World!";
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

    // Register RateExceededListeners
    // ------------------------------

    // If you do not register a listener, the default listener throws an exception
    registry.registerRateExceededListener(rateExceededEvent -> {

      // For example, log the limit that was exceeded
      System.out.println("Limit exceeded: " + rateExceededEvent.getExceededLimit());
    });

    // Register request matchers
    // -------------------------

    // The default behaviour is to return the relative request URI
    // Here are other examples:

    // Rate limit by session id
    registry.registerRequestMatcher("limitBySession", (request, resultIfNone) -> {
      return request.getSession().getId();
    });

    final String paramName = "utm_source";

    // Rate limit by request parameter utm_source
    registry.registerRequestMatcher("limitByParamUtmSource", (request, resultIfNone) -> {
      return request.getParameter(paramName);
    });

    // Rate limit users from a specific utm_source e.g facebook
    registry.registerRequestMatcher("limitByParamUtmSourceIsFacebook", (request, resultIfNone) -> {
      final String paramValue = request.getParameter(paramName);
      if("facebook".equals(paramValue)) {
        return paramValue;
      }else{
        return resultIfNone;
      }
    });
  }
}
```

## Ways and Means

There are 2 ways to rate limit a web application:

### 1. Use the `@RateLimit` and/or `@RateLimitGroup` annotation

For the specification of these annotations, please read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).
In addition, the following applies:

- The `@RateLimit` annotation must be placed together with path related annotations e.g:
  Springframeworks's `@RequestMapping`, `@Get` etc or JAX-RS `@Path` etc

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
        return "Hello World!";
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
        return "Hello World!";
    }
}
```
  
### 2. Define rate limit properties

Example class that implements the required properties.

```java
package com.example.web;

import com.looseboxes.ratelimiter.rates.Logic;
import com.looseboxes.ratelimiter.util.RateConfig;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RateLimitPropertiesImpl implements RateLimitProperties {

  @Override public List<String> getResourcePackages() {
    return Collections.singletonList("com.example.web.resources");
  }

  @Override public Map<String, RateConfigList> getRateLimitConfigs() {
    return Collections.singletonMap("limitBySession", getRateLimitConfigList());
  }

  private RateConfigList getRateLimitConfigList() {
    RateConfigList rateConfigList = new RateConfigList();
    rateConfigList.setLimits(getRateLimits());
    rateConfigList.setLogic(Logic.OR);
    return rateConfigList;
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

Enjoy! :wink:
