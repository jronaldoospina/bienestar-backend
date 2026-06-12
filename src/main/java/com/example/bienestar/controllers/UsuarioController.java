package com.example.bienestar.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bienestar.models.Usuario;
import com.example.bienestar.services.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<?> obtenerTodos(@RequestHeader("X-User-Rol") String rol) {
        if (!"ADMIN".equals(rol) && !"PSICOLOGO".equals(rol)) {
            return ResponseEntity.status(403).body("Acceso denegado");
        }
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable String id,
                                          @RequestHeader("X-User-Rol") String rol,
                                          @RequestHeader("X-User-Id") String userId) {
        // El propio usuario puede ver su perfil
        if (userId.equals(id)) {
            return usuarioService.obtenerPorId(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        if ("ADMIN".equals(rol)) {
            return usuarioService.obtenerPorId(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        if ("PSICOLOGO".equals(rol)) {
            var usuarioOpt = usuarioService.obtenerPorId(id);
            if (usuarioOpt.isPresent() && "ESTUDIANTE".equals(usuarioOpt.get().getRol())) {
                return ResponseEntity.ok(usuarioOpt.get());
            }
        }
        return ResponseEntity.status(403).body("Acceso denegado");
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Usuario usuario, @RequestHeader("X-User-Rol") String rol) {
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(403).body("Solo administradores pueden crear usuarios");
        }
        try {
            Usuario nuevo = usuarioService.crearUsuario(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody Usuario usuario,
                                        @RequestHeader("X-User-Rol") String rol,
                                        @RequestHeader("X-User-Id") String userId) {
        // El propio usuario puede actualizar sus datos (notificaciones, etc.)
        if (userId.equals(id)) {
            Usuario actualizado = usuarioService.actualizarUsuario(id, usuario);
            return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
        }
        if ("ADMIN".equals(rol)) {
            Usuario actualizado = usuarioService.actualizarUsuario(id, usuario);
            return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
        }
        if ("PSICOLOGO".equals(rol)) {
            var targetOpt = usuarioService.obtenerPorId(id);
            if (targetOpt.isPresent() && "ESTUDIANTE".equals(targetOpt.get().getRol())) {
                Usuario actualizado = usuarioService.actualizarUsuario(id, usuario);
                return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.status(403).body("Solo puedes editar estudiantes");
            }
        }
        return ResponseEntity.status(403).body("No tienes permiso para editar este usuario");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id, @RequestHeader("X-User-Rol") String rol) {
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(403).body("Solo administradores pueden eliminar usuarios");
        }
        if (usuarioService.eliminarUsuario(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}