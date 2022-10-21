package edu.lehigh.libraries.purchase_request.workflow_proxy_server.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final RequestMatcher
        PUBLIC_URLS = new AntPathRequestMatcher("/resources/**"),
        PROTECTED_URLS = new NegatedRequestMatcher(PUBLIC_URLS);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .cors().configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues())
            .and()
            .csrf().disable()
            .authorizeHttpRequests().requestMatchers(PROTECTED_URLS).authenticated()
            .and()
            .authorizeHttpRequests().requestMatchers(PUBLIC_URLS).permitAll()
            .and()
            .httpBasic().realmName("purchase-request-workflow")
            .and()
            .formLogin().disable()
            .logout().disable();
        return httpSecurity.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() { 
            @Autowired
            private ClientRepository userRepository;

            @Override
            public UserDetails loadUserByUsername(String clientName) {
                Client client = Optional.ofNullable(userRepository.findByClientName(clientName))
                    .orElseThrow(() -> new UsernameNotFoundException("Cannot find client: " + clientName));
                return new WorkflowClientDetails(client);
            }
        };
    }

    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

}
