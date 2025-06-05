package fr.epita.yeea2.config;

import fr.epita.yeea2.service.AppUserDetailsService;
import fr.epita.yeea2.service.CustomOAuth2UserService;
import fr.epita.yeea2.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

//    private AppUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final UserDetailsService userDetailsService;

    @Bean
    public GoogleAccessTokenAuthenticationProvider googleAccessTokenAuthenticationProvider() {
        return new GoogleAccessTokenAuthenticationProvider();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authenticationManager,
                                           JwtService jwtService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/**", "/oauth2/**") .permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/google/profile", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // persists the user
                        )
                        .successHandler(oAuth2SuccessHandler) // inject success handler for OAuth2
                )
                .addFilterBefore(
                        new GoogleAccessTokenAuthenticationFilter(authenticationManager, jwtService),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            GoogleAccessTokenAuthenticationProvider googleProvider,
            JwtAuthenticationProvider jwtProvider, // ADD THIS
            AppUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) throws Exception {

        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);

        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);


        auth.authenticationProvider(googleProvider);
        auth.authenticationProvider(jwtProvider); // 🔥 MUST be added

        return auth.build();
    }



}

