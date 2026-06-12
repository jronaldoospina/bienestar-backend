package com.example.bienestar.services;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bienestar.models.Cita;
import com.example.bienestar.models.Notificacion;
import com.example.bienestar.models.Usuario;
import com.example.bienestar.repos.UsuarioRepo;

@Service
public class CitaService {

    @Autowired
    private UsuarioRepo usuarioRepo;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    private String generarIdCita() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private String generarIdCitaUnico() {
        String id;
        do {
            id = generarIdCita();
        } while (existeIdCitaEnAlgunUsuario(id));
        return id;
    }

    private boolean existeIdCitaEnAlgunUsuario(String id) {
        List<Usuario> usuarios = usuarioRepo.findAll();
        return usuarios.stream().anyMatch(u -> u.getCitas().stream().anyMatch(c -> c.getId().equals(id)));
    }

    public List<Cita> obtenerCitasPorPsicologoYFecha(String psicologoId, LocalDate fecha) {
        Optional<Usuario> psicologoOpt = usuarioRepo.findById(psicologoId);
        if (psicologoOpt.isEmpty() || !"PSICOLOGO".equals(psicologoOpt.get().getRol())) {
            throw new RuntimeException("Psicólogo no encontrado");
        }
        return psicologoOpt.get().getCitas().stream()
                .filter(c -> c.getFechaHora().toLocalDate().equals(fecha))
                .collect(Collectors.toList());
    }

    public List<Cita> obtenerCitasFuturasPorPsicologo(String psicologoId) {
        Optional<Usuario> psicologoOpt = usuarioRepo.findById(psicologoId);
        if (psicologoOpt.isEmpty() || !"PSICOLOGO".equals(psicologoOpt.get().getRol())) {
            throw new RuntimeException("Psicólogo no encontrado");
        }
        LocalDateTime ahora = LocalDateTime.now();
        return psicologoOpt.get().getCitas().stream()
                .filter(c -> c.getFechaHora().isAfter(ahora) && !"CANCELADA".equals(c.getEstado()))
                .sorted(Comparator.comparing(Cita::getFechaHora))
                .collect(Collectors.toList());
    }

    public Cita crearCita(String estudianteId, String psicologoId, LocalDateTime fechaHora, String motivo, Integer duracionMinutos) {
        Usuario estudiante = usuarioRepo.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        if (!"ESTUDIANTE".equals(estudiante.getRol()))
            throw new RuntimeException("El usuario no es estudiante");

        Usuario psicologo = usuarioRepo.findById(psicologoId)
                .orElseThrow(() -> new RuntimeException("Psicólogo no encontrado"));
        if (!"PSICOLOGO".equals(psicologo.getRol()))
            throw new RuntimeException("El usuario no es psicólogo");

        if (duracionMinutos == null || duracionMinutos <= 0) duracionMinutos = 60;
        LocalDateTime fin = fechaHora.plusMinutes(duracionMinutos);

        boolean conflicto = psicologo.getCitas().stream().anyMatch(c -> {
            LocalDateTime inicio = c.getFechaHora();
            LocalDateTime finExist = inicio.plusMinutes(c.getDuracionMinutos());
            return fechaHora.isBefore(finExist) && fin.isAfter(inicio);
        });
        if (conflicto) throw new RuntimeException("Horario no disponible");

        String nuevoId = generarIdCitaUnico();
        Cita nuevaCita = new Cita(nuevoId, estudianteId, psicologoId, fechaHora, duracionMinutos, motivo, "PROGRAMADA");

        estudiante.getCitas().add(nuevaCita);
        psicologo.getCitas().add(nuevaCita);

        estudiante.getNotificaciones().add(new Notificacion("Tienes una nueva cita el " + fechaHora + " con " + psicologo.getNombre()));

        usuarioRepo.save(estudiante);
        usuarioRepo.save(psicologo);
        return nuevaCita;
    }

    public Cita reprogramarCita(String citaId, LocalDateTime nuevaFechaHora, String motivoReprogramacion, String psicologoId) {
        Usuario psicologo = usuarioRepo.findById(psicologoId)
                .orElseThrow(() -> new RuntimeException("Psicólogo no encontrado"));
        Cita cita = psicologo.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        LocalDateTime ahora = LocalDateTime.now();
        if (cita.getFechaHora().isBefore(ahora)) throw new RuntimeException("No se puede reprogramar una cita pasada");
        if (nuevaFechaHora.isBefore(ahora)) throw new RuntimeException("La nueva fecha no puede ser anterior a hoy");

        int duracion = cita.getDuracionMinutos();
        LocalDateTime nuevoFin = nuevaFechaHora.plusMinutes(duracion);
        boolean conflicto = psicologo.getCitas().stream()
                .filter(c -> !c.getId().equals(citaId))
                .anyMatch(c -> {
                    LocalDateTime inicio = c.getFechaHora();
                    LocalDateTime fin = inicio.plusMinutes(c.getDuracionMinutos());
                    return nuevaFechaHora.isBefore(fin) && nuevoFin.isAfter(inicio);
                });
        if (conflicto) throw new RuntimeException("Horario no disponible para reprogramar");

        cita.setFechaHora(nuevaFechaHora);
        if (motivoReprogramacion != null) cita.setMotivo(motivoReprogramacion);
        cita.setNotificacion("Cita reprogramada para " + nuevaFechaHora);

        Usuario estudiante = usuarioRepo.findById(cita.getEstudianteId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        Cita citaEst = estudiante.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst().orElse(null);
        if (citaEst != null) {
            citaEst.setFechaHora(nuevaFechaHora);
            if (motivoReprogramacion != null) citaEst.setMotivo(motivoReprogramacion);
            citaEst.setNotificacion("Tu cita ha sido reprogramada para " + nuevaFechaHora);
            estudiante.getNotificaciones().add(new Notificacion("Tu cita ha sido reprogramada para " + nuevaFechaHora));
            usuarioRepo.save(estudiante);
        }
        usuarioRepo.save(psicologo);
        return cita;
    }

    public void solicitarCancelacion(String citaId, String estudianteId) {
        Usuario estudiante = usuarioRepo.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        Cita cita = estudiante.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no pertenece al estudiante"));
        if (cita.getEstado().equals("CANCELADA")) throw new RuntimeException("Cita ya cancelada");
        if (cita.getFechaHora().isBefore(LocalDateTime.now())) throw new RuntimeException("No se puede cancelar una cita pasada");

        cita.setEstado("CANCELACION_SOLICITADA");
        cita.setSolicitudCancelacion(true);
        usuarioRepo.save(estudiante);

        Usuario psicologo = usuarioRepo.findById(cita.getPsicologoId()).orElse(null);
        if (psicologo != null) {
            psicologo.getCitas().stream()
                    .filter(c -> c.getId().equals(citaId))
                    .findFirst()
                    .ifPresent(c -> {
                        c.setEstado("CANCELACION_SOLICITADA");
                        c.setSolicitudCancelacion(true);
                    });
            usuarioRepo.save(psicologo);
        }
    }

    // MÉTODO CORREGIDO: actualiza los flags en el psicólogo también
    public void solicitarReasignacion(String citaId, String estudianteId, String motivo) {
        Usuario estudiante = usuarioRepo.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        Cita citaEst = estudiante.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no pertenece al estudiante"));

        if (citaEst.isSolicitudReasignacion())
            throw new RuntimeException("Ya solicitaste reasignación una vez");
        if (!citaEst.getEstado().equals("PROGRAMADA"))
            throw new RuntimeException("Solo se puede reasignar una cita en estado PROGRAMADA");
        if (citaEst.getFechaHora().isBefore(LocalDateTime.now()))
            throw new RuntimeException("No se puede reasignar una cita pasada");

        // Actualizar en el estudiante
        citaEst.setSolicitudReasignacion(true);
        citaEst.setMotivoReasignacion(motivo);
        citaEst.setEstado("REASIGNACION_SOLICITADA");

        // Actualizar la misma cita en el psicólogo
        Usuario psicologo = usuarioRepo.findById(citaEst.getPsicologoId())
                .orElseThrow(() -> new RuntimeException("Psicólogo no encontrado"));
        Cita citaPsi = psicologo.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no encontrada en el psicólogo"));
        citaPsi.setSolicitudReasignacion(true);
        citaPsi.setMotivoReasignacion(motivo);
        citaPsi.setEstado("REASIGNACION_SOLICITADA");

        // Agregar notificación al psicólogo
        psicologo.getNotificaciones().add(new Notificacion(
            "El estudiante " + estudiante.getNombre() + " solicita reasignar la cita del " 
            + citaEst.getFechaHora() + ". Motivo: " + motivo
        ));

        // Guardar ambos
        usuarioRepo.save(estudiante);
        usuarioRepo.save(psicologo);
    }

    public void aceptarCancelacion(String citaId, String psicologoId) {
        Usuario psicologo = usuarioRepo.findById(psicologoId)
                .orElseThrow(() -> new RuntimeException("Psicólogo no encontrado"));
        Cita cita = psicologo.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        if (!cita.isSolicitudCancelacion()) throw new RuntimeException("No hay solicitud de cancelación pendiente");

        cita.setEstado("CANCELADA");
        cita.setSolicitudCancelacion(false);
        cita.setNotificacion("Cancelación aceptada por el psicólogo");

        Usuario estudiante = usuarioRepo.findById(cita.getEstudianteId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        Cita citaEst = estudiante.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst().orElse(null);
        if (citaEst != null) {
            citaEst.setEstado("CANCELADA");
            citaEst.setSolicitudCancelacion(false);
            citaEst.setNotificacion("El psicólogo aceptó la cancelación de tu cita.");
            estudiante.getNotificaciones().add(new Notificacion("Tu cita del " + cita.getFechaHora() + " ha sido cancelada."));
            usuarioRepo.save(estudiante);
        }
        usuarioRepo.save(psicologo);
    }

    public void rechazarCancelacion(String citaId, String psicologoId) {
        Usuario psicologo = usuarioRepo.findById(psicologoId)
                .orElseThrow(() -> new RuntimeException("Psicólogo no encontrado"));
        Cita cita = psicologo.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        if (!cita.isSolicitudCancelacion()) throw new RuntimeException("No hay solicitud de cancelación pendiente");

        cita.setEstado("PROGRAMADA");
        cita.setSolicitudCancelacion(false);
        cita.setNotificacion("El psicólogo rechazó la solicitud de cancelación");

        Usuario estudiante = usuarioRepo.findById(cita.getEstudianteId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        Cita citaEst = estudiante.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst().orElse(null);
        if (citaEst != null) {
            citaEst.setEstado("PROGRAMADA");
            citaEst.setSolicitudCancelacion(false);
            citaEst.setNotificacion("El psicólogo rechazó tu solicitud de cancelación.");
            usuarioRepo.save(estudiante);
        }
        usuarioRepo.save(psicologo);
    }

    public void aceptarReasignacion(String citaId, String psicologoId, LocalDateTime nuevaFechaHora) {
        Usuario psicologo = usuarioRepo.findById(psicologoId)
                .orElseThrow(() -> new RuntimeException("Psicólogo no encontrado"));
        Cita cita = psicologo.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        if (!cita.isSolicitudReasignacion()) throw new RuntimeException("No hay solicitud de reasignación pendiente");

        int duracion = cita.getDuracionMinutos();
        LocalDateTime nuevoFin = nuevaFechaHora.plusMinutes(duracion);
        boolean conflicto = psicologo.getCitas().stream()
                .filter(c -> !c.getId().equals(citaId))
                .anyMatch(c -> {
                    LocalDateTime inicio = c.getFechaHora();
                    LocalDateTime fin = inicio.plusMinutes(c.getDuracionMinutos());
                    return nuevaFechaHora.isBefore(fin) && nuevoFin.isAfter(inicio);
                });
        if (conflicto) throw new RuntimeException("Horario no disponible para la reasignación");

        cita.setFechaHora(nuevaFechaHora);
        cita.setSolicitudReasignacion(false);
        cita.setEstado("PROGRAMADA");
        cita.setNotificacion("Cita reasignada a " + nuevaFechaHora);

        Usuario estudiante = usuarioRepo.findById(cita.getEstudianteId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        Cita citaEst = estudiante.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst().orElse(null);
        if (citaEst != null) {
            citaEst.setFechaHora(nuevaFechaHora);
            citaEst.setSolicitudReasignacion(false);
            citaEst.setEstado("PROGRAMADA");
            citaEst.setNotificacion("Tu cita ha sido reasignada para " + nuevaFechaHora);
            estudiante.getNotificaciones().add(new Notificacion("Tu cita ha sido reasignada para " + nuevaFechaHora));
            usuarioRepo.save(estudiante);
        }
        usuarioRepo.save(psicologo);
    }

    public void rechazarReasignacion(String citaId, String psicologoId) {
        Usuario psicologo = usuarioRepo.findById(psicologoId)
                .orElseThrow(() -> new RuntimeException("Psicólogo no encontrado"));
        Cita cita = psicologo.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        if (!cita.isSolicitudReasignacion()) throw new RuntimeException("No hay solicitud de reasignación pendiente");

        cita.setSolicitudReasignacion(false);
        cita.setEstado("PROGRAMADA");
        cita.setNotificacion("El psicólogo rechazó la solicitud de reasignación");

        Usuario estudiante = usuarioRepo.findById(cita.getEstudianteId())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        Cita citaEst = estudiante.getCitas().stream()
                .filter(c -> c.getId().equals(citaId))
                .findFirst().orElse(null);
        if (citaEst != null) {
            citaEst.setSolicitudReasignacion(false);
            citaEst.setEstado("PROGRAMADA");
            citaEst.setNotificacion("El psicólogo rechazó tu solicitud de reasignación.");
            usuarioRepo.save(estudiante);
        }
        usuarioRepo.save(psicologo);
    }

    public List<Cita> obtenerCitasFuturasEstudiante(String estudianteId) {
        Usuario estudiante = usuarioRepo.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        LocalDateTime ahora = LocalDateTime.now();
        return estudiante.getCitas().stream()
                .filter(c -> c.getFechaHora().isAfter(ahora) && !"CANCELADA".equals(c.getEstado()))
                .sorted(Comparator.comparing(Cita::getFechaHora))
                .collect(Collectors.toList());
    }

    public void eliminarCita(String citaId, String usuarioId, String rol) {
        Cita cita = buscarCitaPorId(citaId);
        if (cita == null) throw new RuntimeException("Cita no encontrada");
        if ("PSICOLOGO".equals(rol) && !cita.getPsicologoId().equals(usuarioId))
            throw new RuntimeException("No autorizado");
        Usuario psicologo = usuarioRepo.findById(cita.getPsicologoId()).orElse(null);
        if (psicologo != null) {
            psicologo.getCitas().removeIf(c -> c.getId().equals(citaId));
            usuarioRepo.save(psicologo);
        }
        Usuario estudiante = usuarioRepo.findById(cita.getEstudianteId()).orElse(null);
        if (estudiante != null) {
            estudiante.getCitas().removeIf(c -> c.getId().equals(citaId));
            usuarioRepo.save(estudiante);
        }
    }

    private Cita buscarCitaPorId(String citaId) {
        List<Usuario> usuarios = usuarioRepo.findAll();
        for (Usuario u : usuarios) {
            for (Cita c : u.getCitas()) {
                if (c.getId().equals(citaId)) return c;
            }
        }
        return null;
    }
}