package pe.instruccionfacil.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import pe.instruccionfacil.repository.UsuarioRepository;

@Configuration
@EnableMethodSecurity
public class SeguridadConfig {
    public static final String EMISOR = "instruccionfacil";

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
        var autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("role");
        autoridades.setAuthorityPrefix("ROLE_");
        var conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);

        return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/health", "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/registro", "/api/v1/auth/login", "/api/v1/webhooks/mercado-pago").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> error(res, 401, "Autenticación requerida"))
                .accessDeniedHandler((req, res, ex) -> error(res, 403, "Acceso denegado")))
            .oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint((req, res, ex) -> error(res, 401, "Token inválido o vencido"))
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)))
            .build();
    }

    private static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + message + "\"}");
    }

    @Bean
    SecretKey jwtKey(@Value("${app.jwt-secret}") String secreto, Environment environment) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (secreto.isBlank()) {
            if (!environment.acceptsProfiles(Profiles.of("local", "docker"))) {
                throw new IllegalArgumentException("APP_JWT_SECRET es obligatorio con el perfil Oracle");
            }
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("APP_JWT_SECRET debe tener al menos 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, UsuarioRepository usuarios) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).build();
        OAuth2TokenValidator<Jwt> cuentaHabilitada = jwt -> {
            try {
                if (usuarios.estaHabilitado(Long.parseLong(jwt.getSubject()))) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (NumberFormatException ex) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Cuenta inválida", null));
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Cuenta inhabilitada", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(EMISOR), cuentaHabilitada));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
