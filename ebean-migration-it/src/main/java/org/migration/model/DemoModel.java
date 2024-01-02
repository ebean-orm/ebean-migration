package org.migration.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Roland Praml, FOCONIS AG
 */
@Entity
public class DemoModel {
  @Id
  private int id;

  private String foo;
}
