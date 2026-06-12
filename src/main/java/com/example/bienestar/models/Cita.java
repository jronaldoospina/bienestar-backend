package com.example.bienestar.models;

import java.time.LocalDateTime;

public class Cita {
    private String id;
    private String estudianteId;
    private String psicologoId;
    private LocalDateTime fechaHora;
    private Integer duracionMinutos;
    private String motivo;
    private String estado;
    private boolean solicitudCancelacion;
    private boolean solicitudReasignacion;
    private String motivoReasignacion;
    private LocalDateTime fechaReasignacionSolicitada;
    private String notificacion;

    public Cita() {}

    public Cita(String id, String estudianteId, String psicologoId, LocalDateTime fechaHora, Integer duracionMinutos, String motivo, String estado) {
        this.id = id;
        this.estudianteId = estudianteId;
        this.psicologoId = psicologoId;
        this.fechaHora = fechaHora;
        this.duracionMinutos = duracionMinutos;
        this.motivo = motivo;
        this.estado = estado;
        this.solicitudCancelacion = false;
        this.solicitudReasignacion = false;
    }

    // Getters y Setters (todos)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }
    public String getPsicologoId() { return psicologoId; }
    public void setPsicologoId(String psicologoId) { this.psicologoId = psicologoId; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isSolicitudCancelacion() { return solicitudCancelacion; }
    public void setSolicitudCancelacion(boolean solicitudCancelacion) { this.solicitudCancelacion = solicitudCancelacion; }
    public boolean isSolicitudReasignacion() { return solicitudReasignacion; }
    public void setSolicitudReasignacion(boolean solicitudReasignacion) { this.solicitudReasignacion = solicitudReasignacion; }
    public String getMotivoReasignacion() { return motivoReasignacion; }
    public void setMotivoReasignacion(String motivoReasignacion) { this.motivoReasignacion = motivoReasignacion; }
    public LocalDateTime getFechaReasignacionSolicitada() { return fechaReasignacionSolicitada; }
    public void setFechaReasignacionSolicitada(LocalDateTime fechaReasignacionSolicitada) { this.fechaReasignacionSolicitada = fechaReasignacionSolicitada; }
    public String getNotificacion() { return notificacion; }
    public void setNotificacion(String notificacion) { this.notificacion = notificacion; }
}