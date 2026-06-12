package com.example.bienestar.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bienestar.models.Usuario;
import com.example.bienestar.repos.UsuarioRepo;
import com.example.bienestar.utils.PasswordEncoderUtils;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepo usuarioRepo;

    @Autowired
    private PasswordEncoderUtils passwordEncoder;

    public Optional<Usuario> login(String email, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepo.findByEmail(email);
        if (usuarioOpt.isPresent() && passwordEncoder.matches(password, usuarioOpt.get().getPassword())) {
            return usuarioOpt;
        }
        return Optional.empty();
    }
}