package io.virusafe.security.advice;

import io.virusafe.configuration.RateLimitConfiguration;
import io.virusafe.domain.entity.RateLimitType;
import io.virusafe.security.advice.calculator.service.RateLimitBucketCalculator;
import io.virusafe.security.principal.UserPrincipal;
import io.virusafe.service.ratelimit.RateLimitService;
import io.virusafe.service.userdetails.UserDetailsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Proximity rate limit advice
 */
@Component
@Aspect
public class ProximityRateLimitAdvice extends RateLimitAdvice {

    /**
     * Construct proximity rate limit advice using beans
     *
     * @param rateLimitConfiguration
     * @param userDetailsService
     * @param rateLimitService
     * @param systemClock
     * @param rateLimitBucketCalculator
     */
    @Autowired
    public ProximityRateLimitAdvice(final RateLimitConfiguration rateLimitConfiguration,
                                    final UserDetailsService userDetailsService,
                                    final RateLimitService rateLimitService,
                                    final Clock systemClock,
                                    final RateLimitBucketCalculator rateLimitBucketCalculator) {
        super(rateLimitConfiguration, userDetailsService, rateLimitService, systemClock, rateLimitBucketCalculator);
    }

    /**
     * Around advice over all methods annotated with ProximityRateLimit
     *
     * @param joinPoint
     * @param userPrincipal
     * @return
     * @throws Throwable
     */
    @Around("@annotation(io.virusafe.security.advice.ProximityRateLimit) && args(userPrincipal,..)")
    public Object proximityRateLimit(final ProceedingJoinPoint joinPoint, final UserPrincipal userPrincipal)
            throws Throwable {
        return rateLimit(joinPoint, userPrincipal);
    }

    @Override
    protected long getRateLimit() {
        return rateLimitConfiguration.getProximityUpdateLimit();
    }

    @Override
    protected long getBucketSize() {
        return rateLimitConfiguration.getProximityUpdateBucketSize();
    }

    @Override
    protected RateLimitType getRateLimitType() {
        return RateLimitType.PROXIMITY;
    }

}
