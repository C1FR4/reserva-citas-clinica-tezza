package com.clinica.clinica.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                     Object handler) throws Exception {
                var session = request.getSession(false);
                if (session != null && "PACIENTE".equals(session.getAttribute("rol"))) {
                    response.setHeader("Cache-Control", "no-store");
                    return true;
                }
                response.sendRedirect("/InicioSesion.html");
                return false;
            }
        }).addPathPatterns("/PantallaPrincipal.html", "/ReservarCita.html",
                "/MisCitas.html", "/DetalleReserva.html");
    }
}
