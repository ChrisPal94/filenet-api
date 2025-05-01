package com.api.filenet.controller;

import com.api.filenet.service.FilenetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FilenetController {

  private final FilenetService filenetService;

  public FilenetController(FilenetService filenetService) {
    this.filenetService = filenetService;
  }

  @GetMapping("/api/conectar")
  public String conectarFilenet() {
    return filenetService.connectAndFetch();
  }

  @GetMapping("/api/documentos/total")
  public String contarDocumentos() {
    return filenetService.contarDocumentos();
  }

  @GetMapping("/api/carpetas/total")
  public String listarNombresDeCarpetas() {
    return filenetService.listarNombresDeCarpetas();
  }
}
