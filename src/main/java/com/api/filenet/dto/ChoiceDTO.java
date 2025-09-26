package com.api.filenet.dto;

public class ChoiceDTO {

  private String displayName;
  private String value;

  public ChoiceDTO(String displayName, String value) {
    this.displayName = displayName;
    this.value = value;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getValue() {
    return value;
  }
}
