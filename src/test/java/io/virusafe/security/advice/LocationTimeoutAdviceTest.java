package io.virusafe.security.advice;

import io.virusafe.configuration.RateLimitConfiguration;
import io.virusafe.domain.entity.RateLimit;
import io.virusafe.domain.entity.RateLimitType;
import io.virusafe.domain.entity.UserDetails;
import io.virusafe.exception.RateLimitTimeoutException;
import io.virusafe.security.advice.calculator.service.RateLimitBucketCalculator;
import io.virusafe.security.principal.UserPrincipal;
import io.virusafe.service.ratelimit.RateLimitService;
import io.virusafe.service.userdetails.UserDetailsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationTimeoutAdviceTest {

    private static final String DEFAULT_USER_GUID = "userGuid";
    private static final long DEFAULT_BUCKET_SIZE = 1800L;
    private static final long DEFAULT_REQUEST_LIMIT = 10L;
    private static final RateLimitType RATE_LIMIT_TYPE = RateLimitType.GPS_LOCATION;
    private static final RateLimit EMPTY_RATE_LIMIT = RateLimit.builder().build();

    @Mock
    private RateLimitConfiguration rateLimitConfiguration;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private RateLimitBucketCalculator rateLimitBucketCalculator;

    private Clock clock = Clock.fixed(
            Instant.parse("2020-09-05T00:00:00.00Z"),
            ZoneId.of("UTC")
    );

    private LocalDateTime calculationTime = LocalDateTime.of(
            2020, 9, 5, 0, 0, 0, 0);

    private UserPrincipal userPrincipal = UserPrincipal.builder().userGuid(DEFAULT_USER_GUID).build();

    private LocationTimeoutAdvice locationTimeoutAdvice;

    @BeforeEach
    public void setUp() {
        when(rateLimitConfiguration.getLocationUpdateLimit()).thenReturn(DEFAULT_REQUEST_LIMIT);
        when(rateLimitConfiguration.getLocationUpdateBucketSize()).thenReturn(DEFAULT_BUCKET_SIZE);
        locationTimeoutAdvice = new LocationTimeoutAdvice(rateLimitConfiguration, userDetailsService, rateLimitService,
                clock, rateLimitBucketCalculator);
    }

    @Test
    void testAccessAllowedWhenNoUserExists() throws Throwable {
        when(userDetailsService.findByUserGuid(userPrincipal.getUserGuid()))
                .thenReturn(Optional.empty());

        locationTimeoutAdvice.locationTimeout(proceedingJoinPoint, userPrincipal);

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    void testAccessAllowedWhenExistingBucketTimedOut() throws Throwable {
        // Bucket expired before our current time.
        LocalDateTime expiredBucketTime = LocalDateTime.of(
                2020, 9, 4, 23, 59, 59, 0);
        Long existingBucketCount = 2L;
        // 30 minutes from our base time
        LocalDateTime expectedBucketTime = LocalDateTime.of(
                2020, 9, 5, 0, 30, 0, 0);
        Long expectedBucketCount = 1L;

        Optional<UserDetails> dbUser = Optional.of(
                UserDetails.builder()
                        .userGuid(DEFAULT_USER_GUID)
                        .build()
        );

        when(userDetailsService.findByUserGuid(userPrincipal.getUserGuid()))
                .thenReturn(dbUser);
        when(rateLimitService.findRateLimitByUserAndType(dbUser.get(), RATE_LIMIT_TYPE))
                .thenReturn(EMPTY_RATE_LIMIT);
        when(rateLimitBucketCalculator.calculate(calculationTime, EMPTY_RATE_LIMIT,
                DEFAULT_BUCKET_SIZE, ChronoUnit.SECONDS))
                .thenReturn(RateLimit.builder().userDetails(dbUser.get()).type(RATE_LIMIT_TYPE)
                        .lastUpdateTime(expiredBucketTime).bucketCount(existingBucketCount).build());

        locationTimeoutAdvice.locationTimeout(proceedingJoinPoint, userPrincipal);

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    void testAccessAllowedForRequestWithinBucket() throws Throwable {
        // Existing bucket is after the system current time.
        LocalDateTime existingBucketTime = LocalDateTime.of(
                2020, 9, 5, 0, 11, 4, 0);
        Long existingBucketCount = 2L;

        // Bucket count incremented
        Long expectedBucketCount = 3L;

        Optional<UserDetails> dbUser = Optional.of(
                UserDetails.builder()
                        .userGuid(DEFAULT_USER_GUID)
                        .build()
        );

        when(userDetailsService.findByUserGuid(userPrincipal.getUserGuid()))
                .thenReturn(dbUser);
        when(rateLimitService.findRateLimitByUserAndType(dbUser.get(), RATE_LIMIT_TYPE))
                .thenReturn(EMPTY_RATE_LIMIT);
        when(rateLimitBucketCalculator.calculate(calculationTime, EMPTY_RATE_LIMIT,
                DEFAULT_BUCKET_SIZE, ChronoUnit.SECONDS))
                .thenReturn(RateLimit.builder().userDetails(dbUser.get()).type(RATE_LIMIT_TYPE)
                        .lastUpdateTime(existingBucketTime).bucketCount(existingBucketCount).build());

        locationTimeoutAdvice.locationTimeout(proceedingJoinPoint, userPrincipal);

        verify(proceedingJoinPoint).proceed();
    }


    @Test
    void testAccessAllowedFirstRequestWithinBucket() throws Throwable {
        // Existing bucket is after the system current time.
        LocalDateTime existingBucketTime = LocalDateTime.of(
                2020, 9, 5, 0, 11, 4, 0);
        // No request data saved for the bucket yet.
        Long existingBucketCount = null;

        // Newly initialized bucket
        Long expectedBucketCount = 1L;

        Optional<UserDetails> dbUser = Optional.of(
                UserDetails.builder()
                        .userGuid(DEFAULT_USER_GUID)
                        .build()
        );

        when(userDetailsService.findByUserGuid(userPrincipal.getUserGuid()))
                .thenReturn(dbUser);
        when(rateLimitService.findRateLimitByUserAndType(dbUser.get(), RATE_LIMIT_TYPE))
                .thenReturn(EMPTY_RATE_LIMIT);
        when(rateLimitBucketCalculator.calculate(calculationTime, EMPTY_RATE_LIMIT,
                DEFAULT_BUCKET_SIZE, ChronoUnit.SECONDS))
                .thenReturn(RateLimit.builder().userDetails(dbUser.get()).type(RATE_LIMIT_TYPE)
                        .lastUpdateTime(existingBucketTime).bucketCount(expectedBucketCount).build());

        locationTimeoutAdvice.locationTimeout(proceedingJoinPoint, userPrincipal);

        verify(proceedingJoinPoint).proceed();
    }


    @Test
    void testRateLimitExceptionThrownIfRateLimitExceededForABucket() throws Throwable {
        // Existing bucket is after the system current time.
        LocalDateTime existingBucketTime = LocalDateTime.of(
                2020, 9, 5, 0, 11, 4, 0);
        // Rate limit met so next request will exceed it.
        Long existingBucketCount = 10L;
        Long expectedBucketCount = 11L;

        Optional<UserDetails> dbUser = Optional.of(
                UserDetails.builder()
                        .userGuid(DEFAULT_USER_GUID)
                        .build()
        );

        when(userDetailsService.findByUserGuid(userPrincipal.getUserGuid()))
                .thenReturn(dbUser);
        when(rateLimitService.findRateLimitByUserAndType(dbUser.get(), RATE_LIMIT_TYPE))
                .thenReturn(EMPTY_RATE_LIMIT);
        when(rateLimitBucketCalculator.calculate(calculationTime, EMPTY_RATE_LIMIT,
                DEFAULT_BUCKET_SIZE, ChronoUnit.SECONDS))
                .thenReturn(RateLimit.builder().userDetails(dbUser.get()).type(RATE_LIMIT_TYPE)
                        .lastUpdateTime(existingBucketTime).bucketCount(expectedBucketCount).build());

        Assertions.assertThrows(RateLimitTimeoutException.class,
                () -> locationTimeoutAdvice.locationTimeout(proceedingJoinPoint, userPrincipal));
    }
}