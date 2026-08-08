package com.sebastianhauss.videoplatform.auth;

import com.sebastianhauss.videoplatform.domain.user.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * The authenticated principal placed in the {@code SecurityContext}. Extends
 * Spring's {@code User} but additionally carries the application user's id, so
 * controllers can derive the caller's identity from the JWT (via
 * {@code @AuthenticationPrincipal}) instead of trusting a client-supplied
 * {@code userId} request parameter.
 */
public class AppUserPrincipal extends org.springframework.security.core.userdetails.User {

    private final transient UUID id;

    public AppUserPrincipal(User user) {
        super(
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        this.id = user.getId();
    }

    public UUID getId() {
        return id;
    }
}
