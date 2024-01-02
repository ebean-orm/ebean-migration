package org.migration.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Roland Praml, FOCONIS AG
 */
@Entity
public class M3 {
  @Id
  private int id;
  private String acol;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getAcol() {
    return acol;
  }

  public void setAcol(String acol) {
    this.acol = acol;
  }
}
