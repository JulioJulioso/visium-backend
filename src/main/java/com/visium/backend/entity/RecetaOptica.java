package com.visium.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Receta optica generada en una consulta. Una consulta genera como maximo una receta en el MVP.
 * Tabla: recetas_opticas
 */
@Entity
@Table(name = "recetas_opticas")
@Getter
@Setter
@NoArgsConstructor
public class RecetaOptica {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consulta_id", unique = true)
  private Consulta consulta;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paciente_id", nullable = false)
  private Paciente paciente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sucursal_id", nullable = false)
  private Sucursal sucursal;

  @Column(name = "fecha_emision", nullable = false)
  private LocalDate fechaEmision;

  @Column(name = "vigencia_hasta")
  private LocalDate vigenciaHasta;

  @Column(precision = 4, scale = 2)
  private BigDecimal adicion;

  @Column(name = "distancia_pupilar", precision = 5, scale = 2)
  private BigDecimal distanciaPupilar;

  @Column(columnDefinition = "TEXT")
  private String indicaciones;

  @Column(columnDefinition = "TEXT")
  private String observaciones;

  // Una receta tiene detalles (normalmente OD y OI)
  @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecetaOpticaDetalle> detalles = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void alCrear() {
    Instant ahora = Instant.now();
    if (fechaEmision == null) {
      fechaEmision = LocalDate.now();
    }
    createdAt = ahora;
    updatedAt = ahora;
  }

  @PreUpdate
  void alActualizar() {
    updatedAt = Instant.now();
  }

  // Ayuda a agregar un detalle manteniendo la relacion bidireccional
  public void agregarDetalle(RecetaOpticaDetalle detalle) {
    detalles.add(detalle);
    detalle.setReceta(this);
  }
}
