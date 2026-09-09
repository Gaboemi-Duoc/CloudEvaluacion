package alumno.gaboemi.cloudevaluacion.config;

import alumno.gaboemi.cloudevaluacion.security.CognitoJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public SecurityConfig(SecretGatewayFilter secretGatewayFilter) {
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecretGatewayFilter secretGatewayFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Añadimos nuestro filtro del secreto antes del filtro de autenticación
                .addFilterBefore(secretGatewayFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll() // Por si deseas dejar el health libre
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new CognitoJwtAuthenticationConverter()))
                );

        return http.build();
    }
	
}