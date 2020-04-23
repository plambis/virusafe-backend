package io.virusafe.service.userdetails;

import io.virusafe.domain.command.PersonalInformationUpdateCommand;
import io.virusafe.domain.entity.UserDetails;
import io.virusafe.repository.UserDetailsRepositoryFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.TooManyMethods")
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final String MISSING_USER_TEMPLATE = "User with guid {0} is not registered!";

    private final UserDetailsRepositoryFacade userDetailsRepositoryFacade;

    /**
     * Construct a new UserDetailsService, using the autowired beans.
     *
     * @param userDetailsRepositoryFacade
     */
    @Autowired
    public UserDetailsServiceImpl(final UserDetailsRepositoryFacade userDetailsRepositoryFacade) {
        this.userDetailsRepositoryFacade = userDetailsRepositoryFacade;
    }

    @Override
    public Optional<UserDetails> findByUserGuid(final String userGuid) {
        return userDetailsRepositoryFacade.findByUserGuid(userGuid);
    }

    @Override
    public Optional<UserDetails> findByPhoneNumber(final String phoneNumber) {
        return userDetailsRepositoryFacade.findByPhoneNumber(phoneNumber);
    }

    @Override
    public Optional<UserDetails> findByPhoneNumberAndValidPin(final String phoneNumber, final String pin,
                                                              final LocalDateTime time) {
        return userDetailsRepositoryFacade.findByPhoneNumberAndValidPin(phoneNumber, pin, time);
    }

    @Override
    public void save(final UserDetails userDetails) {
        userDetailsRepositoryFacade.save(userDetails);
    }

    @Override
    public void updatePersonalInformation(final String userGuid,
                                          final PersonalInformationUpdateCommand personalInformationUpdateCommand) {
        UserDetails userDetails = findByUserGuid(userGuid)
                .orElseThrow(
                        () -> new NoSuchElementException(MessageFormat.format(MISSING_USER_TEMPLATE, userGuid))
                );

        userDetails.setIdentificationNumberPlain(personalInformationUpdateCommand.getIdentificationNumber());
        userDetails.setIdentificationType(personalInformationUpdateCommand.getIdentificationType());
        userDetails.setAge(personalInformationUpdateCommand.getAge());
        userDetails.setGender(personalInformationUpdateCommand.getGender());
        userDetails.setPreExistingConditions(personalInformationUpdateCommand.getPreExistingConditions());

        save(userDetails);
    }

    @Override
    public void updatePushToken(final String userGuid, final String pushToken) {
        UserDetails userDetails = findByUserGuid(userGuid)
                .orElseThrow(
                        () -> new NoSuchElementException(MessageFormat.format(MISSING_USER_TEMPLATE, userGuid))

                );

        userDetails.setPushToken(pushToken);
        save(userDetails);
    }

    @Override
    public void updateTokenDetails(final String userGuid, final String tokenSecret, final String refreshTokenHash) {
        UserDetails userDetails = findByUserGuid(userGuid)
                .orElseThrow(
                        () -> new NoSuchElementException(MessageFormat.format(MISSING_USER_TEMPLATE, userGuid))
                );

        userDetails.setTokenSecret(tokenSecret);
        userDetails.setRefreshToken(refreshTokenHash);
        save(userDetails);
    }

    @Override
    public Optional<UserDetails> findByRefreshToken(final String refreshTokenHash) {
        return userDetailsRepositoryFacade.findByRefreshToken(refreshTokenHash);
    }

    @Override
    public void deleteByUserGuid(final String userGuid) {
        UserDetails userDetails = findByUserGuid(userGuid)
                .orElseThrow(() -> new NoSuchElementException(MessageFormat
                        .format(MISSING_USER_TEMPLATE, userGuid)));
        removeUserDetailsPersonalInfoFields(userDetails);
        save(userDetails);
    }

    private void removeUserDetailsPersonalInfoFields(final UserDetails userDetails) {
        userDetails.setIdentificationNumber(null);
        userDetails.setAge(null);
        userDetails.setGender(null);
        userDetails.setPreExistingConditions(null);
    }

    @Override
    public Set<String> findPushTokensForUserGuids(final Set<String> userGuids) {
        Set<String> guids = userDetailsRepositoryFacade.findAllPushTokensByUserGuid(userGuids);
        return guids.stream().filter(Predicate.not(Objects::isNull))
                .filter(Predicate.not(String::isEmpty)).collect(Collectors.toSet());
    }
}
