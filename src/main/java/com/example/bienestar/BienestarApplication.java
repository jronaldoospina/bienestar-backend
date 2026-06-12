package com.example.bienestar;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.example.bienestar.models.Usuario;
import com.example.bienestar.repos.UsuarioRepo;
import com.example.bienestar.utils.PasswordEncoderUtils;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.bienestar")
public class BienestarApplication {

    public static void main(String[] args) {
        SpringApplication.run(BienestarApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UsuarioRepo usuarioRepo, PasswordEncoderUtils encoder) {
        return args -> {
            // Verificar si no hay ningún administrador
            boolean adminExists = usuarioRepo.findAll().stream().anyMatch(u -> "ADMIN".equals(u.getRol()));
            if (!adminExists) {
                Usuario admin = new Usuario();
                admin.setId("11111111");
                admin.setNombre("Admin Principal");
                admin.setEmail("admin@unicesar.edu.co");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRol("ADMIN");
                admin.setTelefono("3000000000");
                usuarioRepo.save(admin);
                System.out.println("✅ Administrador creado automáticamente: admin@unicesar.edu.co / admin123");
            }
        };
    }
}