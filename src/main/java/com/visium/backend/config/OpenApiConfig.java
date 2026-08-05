package com.visium.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de la documentacion OpenAPI (Swagger UI).
 *
 * <p>Define el esquema de seguridad "bearerAuth" para que Swagger muestre el boton "Authorize": ahi
 * se pega el token JWT obtenido en POST /auth/login y se envia como {@code Authorization: Bearer
 * <token>} en cada request.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI visiumOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("VISIUM Backend API")
                .version("0.2.0")
                .description(
                    "API de administracion de opticas VISIUM.\n\n"
                        + "- La mayoria de endpoints requieren un token JWT en el header"
                        + " `Authorization: Bearer <token>` (boton **Authorize**).\n"
                        + "- El token se obtiene en `POST /auth/login` (endpoint publico).\n"
                        + "- Los roles con permisos se indican en la descripcion de cada"
                        + " endpoint."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}

