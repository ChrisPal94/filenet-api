package com.api.filenet.dto;

import java.util.List;

public class PropertyChoicesDTO {

  private String name;
  private List<ChoiceDTO> choices;

  public PropertyChoicesDTO(String name, List<ChoiceDTO> choices) {
    this.name = name;
    this.choices = choices;
  }

  public String getName() {
    return name;
  }

  public List<ChoiceDTO> getChoices() {
    return choices;
  }
}
