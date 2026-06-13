package com.runway.user.repository;

import com.runway.user.domain.User;
import com.runway.user.domain.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByUserAndProvider(User user, String provider);
}
