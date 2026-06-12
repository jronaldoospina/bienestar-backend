package com.example.bienestar.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bienestar.models.Usuario;
import com.example.bienestar.services.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        var usuarioOpt = authService.login(email, password);
        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "nombre", u.getNombre(),
                "rol", u.getRol(),
                "email", u.getEmail()
            ));
        } else {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }
}