package fr.epita.yeea2.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class GoogleAccessTokenAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private String accessToken;

    public GoogleAccessTokenAuthenticationToken(String accessToken) {
        super(null);
        this.accessToken = accessToken;
        this.principal = null;
        setAuthenticated(false);
    }

    public GoogleAccessTokenAuthenticationToken(Object principal, String accessToken,
                                                Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.accessToken = accessToken;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return accessToken;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
