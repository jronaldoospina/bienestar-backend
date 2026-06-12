package com.example.bienestar.repos;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.bienestar.models.Cita;

@Repository
public interface CitaRepo extends MongoRepository<Cita, String> {
    List<Cita> findByEstudianteId(String estudianteId);
    List<Cita> findByPsicologoId(String psicologoId);
    List<Cita> findByEstado(String estado);
}