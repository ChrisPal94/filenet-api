package com.api.filenet.dto;

import java.util.List;

public class CustomResponse {

  private boolean success;
  private List<PropertyChoicesDTO> choices;

  public CustomResponse(boolean success, List<PropertyChoicesDTO> choices) {
    this.success = success;
    this.choices = choices;
  }

  public boolean isSuccess() {
    return success;
  }

  public List<PropertyChoicesDTO> getChoices() {
    return choices;
  }
}
