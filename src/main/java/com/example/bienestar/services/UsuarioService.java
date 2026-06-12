package com.example.bienestar.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bienestar.models.Usuario;
import com.example.bienestar.repos.UsuarioRepo;
import com.example.bienestar.utils.PasswordEncoderUtils;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepo usuarioRepo;

    @Autowired
    private PasswordEncoderUtils passwordEncoder;

    private boolean esCorreoInstitucional(String email) {
        return email != null && email.toLowerCase().endsWith("@unicesar.edu.co");
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepo.findAll();
    }

    public Optional<Usuario> obtenerPorId(String id) {
        return usuarioRepo.findById(id);
    }

    public Usuario crearUsuario(Usuario usuario) {
        if (!esCorreoInstitucional(usuario.getEmail())) {
            throw new RuntimeException("El correo debe ser institucional (@unicesar.edu.co)");
        }
        if (usuarioRepo.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // La duración de la cita se define al agendar, no es atributo del psicólogo.
        // Por lo tanto, eliminamos las líneas que usaban getDuracionCitaMinutos() y setDuracionCitaMinutos()
        /*
        if ("PSICOLOGO".equals(usuario.getRol()) && usuario.getDuracionCitaMinutos() == null) {
            usuario.setDuracionCitaMinutos(60);
        }
        */

        return usuarioRepo.save(usuario);
    }

    public Usuario actualizarUsuario(String id, Usuario usuarioActualizado) {
        Optional<Usuario> existingOpt = usuarioRepo.findById(id);
        if (existingOpt.isPresent()) {
            Usuario existing = existingOpt.get();
            if (usuarioActualizado.getNombre() != null)
                existing.setNombre(usuarioActualizado.getNombre());
            if (usuarioActualizado.getEmail() != null) {
                if (!esCorreoInstitucional(usuarioActualizado.getEmail()))
                    throw new RuntimeException("El correo debe ser institucional (@unicesar.edu.co)");
                existing.setEmail(usuarioActualizado.getEmail());
            }
            if (usuarioActualizado.getTelefono() != null)
                existing.setTelefono(usuarioActualizado.getTelefono());
            if (usuarioActualizado.getEspecialidad() != null)
                existing.setEspecialidad(usuarioActualizado.getEspecialidad());
            if (usuarioActualizado.getPassword() != null && !usuarioActualizado.getPassword().isEmpty()) {
                existing.setPassword(passwordEncoder.encode(usuarioActualizado.getPassword()));
            }
            return usuarioRepo.save(existing);
        }
        return null;
    }

    public boolean eliminarUsuario(String id) {
        if (usuarioRepo.existsById(id)) {
            usuarioRepo.deleteById(id);
            return true;
        }
        return false;
    }
}