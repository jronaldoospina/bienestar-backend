package com.example.bienestar.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bienestar.models.Cita;
import com.example.bienestar.services.CitaService;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    @Autowired
    private CitaService citaService;

    @GetMapping("/psicologo/{psicologoId}/calendario")
    public ResponseEntity<?> obtenerCitasPorFecha(@PathVariable String psicologoId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                  @RequestHeader("X-User-Rol") String rol) {
        if (!"PSICOLOGO".equals(rol) && !"ADMIN".equals(rol)) {
            return ResponseEntity.status(403).body("Acceso denegado");
        }
        try {
            return ResponseEntity.ok(citaService.obtenerCitasPorPsicologoYFecha(psicologoId, fecha));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/psicologo/{psicologoId}/futuras")
    public ResponseEntity<?> obtenerCitasFuturasPsicologo(@PathVariable String psicologoId,
                                                          @RequestHeader("X-User-Rol") String rol,
                                                          @RequestHeader("X-User-Id") String userId) {
        if ("PSICOLOGO".equals(rol) && !psicologoId.equals(userId)) {
            return ResponseEntity.status(403).body("No autorizado");
        }
        try {
            return ResponseEntity.ok(citaService.obtenerCitasFuturasPorPsicologo(psicologoId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> crearCita(@RequestBody Map<String, Object> datos,
                                       @RequestHeader("X-User-Rol") String rol) {
        if (!"PSICOLOGO".equals(rol) && !"ADMIN".equals(rol)) {
            return ResponseEntity.status(403).body("Solo psicólogos o administradores pueden agendar citas");
        }
        try {
            String estudianteId = (String) datos.get("estudianteId");
            String psicologoId = (String) datos.get("psicologoId");
            String fechaHoraStr = (String) datos.get("fechaHora");
            String motivo = (String) datos.get("motivo");
            Integer duracionMinutos = datos.get("duracionMinutos") != null ?
                    Integer.parseInt(datos.get("duracionMinutos").toString()) : null;

            if (fechaHoraStr.endsWith("Z")) {
                fechaHoraStr = fechaHoraStr.substring(0, fechaHoraStr.length() - 1);
            }
            if (fechaHoraStr.contains(".")) {
                fechaHoraStr = fechaHoraStr.split("\\.")[0];
            }
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            Cita nueva = citaService.crearCita(estudianteId, psicologoId, fechaHora, motivo, duracionMinutos);
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear cita: " + e.getMessage());
        }
    }

    @PutMapping("/{citaId}/reprogramar")
    public ResponseEntity<?> reprogramarCita(@PathVariable String citaId,
                                             @RequestBody Map<String, String> datos,
                                             @RequestHeader("X-User-Rol") String rol,
                                             @RequestHeader("X-User-Id") String userId) {
        if (!"PSICOLOGO".equals(rol)) {
            return ResponseEntity.status(403).body("Solo psicólogos pueden reprogramar citas");
        }
        try {
            LocalDateTime nuevaFecha = LocalDateTime.parse(datos.get("nuevaFechaHora"));
            String motivo = datos.get("motivo");
            Cita actualizada = citaService.reprogramarCita(citaId, nuevaFecha, motivo, userId);
            return ResponseEntity.ok(actualizada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{citaId}/solicitar-reasignacion")
    public ResponseEntity<?> solicitarReasignacion(@PathVariable String citaId,
                                                   @RequestBody(required = false) Map<String, String> datos,
                                                   @RequestHeader("X-User-Rol") String rol,
                                                   @RequestHeader("X-User-Id") String userId) {
        if (!"ESTUDIANTE".equals(rol)) {
            return ResponseEntity.status(403).body("Solo estudiantes pueden solicitar reasignación");
        }
        try {
            String motivo = datos != null ? datos.get("motivo") : null;
            citaService.solicitarReasignacion(citaId, userId, motivo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{citaId}/solicitar-cancelacion")
    public ResponseEntity<?> solicitarCancelacion(@PathVariable String citaId,
                                                  @RequestHeader("X-User-Rol") String rol,
                                                  @RequestHeader("X-User-Id") String userId) {
        if (!"ESTUDIANTE".equals(rol)) {
            return ResponseEntity.status(403).body("Solo estudiantes pueden solicitar cancelación");
        }
        try {
            citaService.solicitarCancelacion(citaId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{citaId}/aceptar-cancelacion")
    public ResponseEntity<?> aceptarCancelacion(@PathVariable String citaId,
                                                @RequestHeader("X-User-Rol") String rol,
                                                @RequestHeader("X-User-Id") String userId) {
        if (!"PSICOLOGO".equals(rol)) {
            return ResponseEntity.status(403).body("No autorizado");
        }
        try {
            citaService.aceptarCancelacion(citaId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{citaId}/rechazar-cancelacion")
    public ResponseEntity<?> rechazarCancelacion(@PathVariable String citaId,
                                                 @RequestHeader("X-User-Rol") String rol,
                                                 @RequestHeader("X-User-Id") String userId) {
        if (!"PSICOLOGO".equals(rol)) {
            return ResponseEntity.status(403).body("No autorizado");
        }
        try {
            citaService.rechazarCancelacion(citaId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{citaId}/aceptar-reasignacion")
    public ResponseEntity<?> aceptarReasignacion(@PathVariable String citaId,
                                                 @RequestBody Map<String, String> datos,
                                                 @RequestHeader("X-User-Rol") String rol,
                                                 @RequestHeader("X-User-Id") String userId) {
        if (!"PSICOLOGO".equals(rol)) {
            return ResponseEntity.status(403).body("No autorizado");
        }
        try {
            LocalDateTime nuevaFecha = LocalDateTime.parse(datos.get("nuevaFechaHora"));
            citaService.aceptarReasignacion(citaId, userId, nuevaFecha);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{citaId}/rechazar-reasignacion")
    public ResponseEntity<?> rechazarReasignacion(@PathVariable String citaId,
                                                  @RequestHeader("X-User-Rol") String rol,
                                                  @RequestHeader("X-User-Id") String userId) {
        if (!"PSICOLOGO".equals(rol)) {
            return ResponseEntity.status(403).body("No autorizado");
        }
        try {
            citaService.rechazarReasignacion(citaId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/estudiante/{estudianteId}/futuras")
    public ResponseEntity<?> obtenerCitasFuturasEstudiante(@PathVariable String estudianteId,
                                                           @RequestHeader("X-User-Rol") String rol,
                                                           @RequestHeader("X-User-Id") String userId) {
        if ("ESTUDIANTE".equals(rol) && !estudianteId.equals(userId)) {
            return ResponseEntity.status(403).body("Solo puedes ver tus propias citas");
        }
        try {
            return ResponseEntity.ok(citaService.obtenerCitasFuturasEstudiante(estudianteId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{citaId}")
    public ResponseEntity<?> eliminarCita(@PathVariable String citaId,
                                          @RequestHeader("X-User-Rol") String rol,
                                          @RequestHeader("X-User-Id") String userId) {
        if (!"PSICOLOGO".equals(rol) && !"ADMIN".equals(rol)) {
            return ResponseEntity.status(403).body("No autorizado");
        }
        try {
            citaService.eliminarCita(citaId, userId, rol);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}