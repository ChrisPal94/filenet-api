package com.api.filenet.controller;

import com.api.filenet.dto.CustomResponse;
import com.api.filenet.exceptions.ErrorException;
import com.api.filenet.service.FilenetService;
import com.filenet.api.core.Document;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  @RestController
  @RequestMapping("/api/fn")
  public class FileNetUploadController {

    @PostMapping("/upload")
    public ResponseEntity<String> subirDocumento(
      @RequestParam("file") MultipartFile file,
      @RequestParam("path") String rutaCarpeta,
      @RequestParam("nombre") String nombreDocumento,
      @RequestParam Map<String, String> metadata
    ) {
      try {
        metadata.remove("path");
        metadata.remove("nombre");
        metadata.remove("file"); // aunque file ya va como MultipartFile

        filenetService.subirDocumentoDesdeApi(
          file,
          rutaCarpeta,
          nombreDocumento,
          metadata
        );
        return ResponseEntity.ok("Documento subido correctamente.");
      } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body(
          "Error al subir documento: " + e.getMessage()
        );
      }
    }
  }

  @RestController
  @RequestMapping("/api/documento")
  public class DocumentoController {

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerDocumento(@PathVariable String id) {
      try {
        Document doc = filenetService.obtenerDocumentoPorId(id); // Obtiene el documento

        // Obtener título y clase como ejemploa
        String titulo = doc.getProperties().getStringValue("DocumentTitle");
        String clase = doc.getClassName();

        // Obtener las ubicaciones del documento
        List<String> paths = filenetService.obtenerUbicacionDocumento(id);

        // Retornar como JSON
        Map<String, Object> response = new HashMap<>();
        response.put("id", doc.get_Id().toString());
        response.put("titulo", titulo);
        response.put("clase", clase);
        response.put("ubicaciones", paths);

        return ResponseEntity.ok(response);
      } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          "Error al obtener documento: " + e.getMessage()
        );
      }
    }
  }

  @RestController
  @RequestMapping("/api/fn")
  public class FileNetController {

    private final FilenetService filenetService;

    public FileNetController(FilenetService filenetService) {
      this.filenetService = filenetService;
    }

    @GetMapping("/download")
    public ResponseEntity<Object> descargarDocumentoDesdeFileNet(
      @RequestParam String path,
      @RequestParam String nombre
    ) {
      try {
        // Obtener contenido y tipo MIME desde el servicio
        Pair<InputStream, String> resultado = filenetService.obtenerContenido(
          path,
          nombre
        );
        InputStream inputStream = resultado.getLeft();
        String contentType = resultado.getRight();

        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
          .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + nombre + "\""
          )
          .contentType(MediaType.parseMediaType(contentType))
          .body(resource);
      } catch (ErrorException e) {
        // Retorna 404 con mensaje personalizado
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          "❌ Documento no encontrado: " + nombre
        );
      } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          "❌ Error al procesar la solicitud."
        );
      }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> eliminarDocumento(
      @RequestParam String path,
      @RequestParam String nombre
    ) {
      try {
        filenetService.eliminarDocumento(path, nombre);
        return ResponseEntity.ok("📁 Documento eliminado correctamente");
      } catch (ErrorException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          "❌ Documento no encontrado"
        );
      } catch (Exception e) {
        e.printStackTrace(); // 🔍 Muestra el error en consola
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          "⚠️ Error al eliminar el documento: " + e.getMessage()
        );
      }
    }

    @DeleteMapping("/delete-folder")
    public ResponseEntity<String> eliminarCarpeta(@RequestParam String path) {
      try {
        String resultado = filenetService.eliminarCarpetaConContenido(path);
        return ResponseEntity.ok(resultado);
      } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          "❌ Error al eliminar la carpeta: " + e.getMessage()
        );
      }
    }

    @GetMapping("/properties/{className}")
    public List<Map<String, Object>> getProperties(
      @PathVariable String className
    ) {
      return filenetService.getCustomPropertiesAll(className);
    }

    @GetMapping("/choices/{className}")
    public CustomResponse getChoices(@PathVariable String className) {
      return filenetService.getCustomProperties(className);
    }

    @GetMapping("/search")
    public ResponseEntity<?> buscarDocumentos(
      @RequestParam(required = false) String nombreDocumento,
      @RequestParam(required = false) String fechaDocumento,
      @RequestParam(required = false) String enteRegulatorio,
      @RequestParam(required = false) String anio,
      @RequestParam(required = false) String tipoDocumento
    ) {
      try {
        List<Map<String, Object>> resultados = filenetService.buscarDocumentos(
          nombreDocumento,
          fechaDocumento,
          enteRegulatorio,
          anio,
          tipoDocumento
        );
        return ResponseEntity.ok(resultados);
      } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body(
          "❌ Error en la búsqueda: " + e.getMessage()
        );
      }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> descargarDocumento(
      @PathVariable("id") String docId
    ) {
      return filenetService.descargarDocumento(docId);
    }
  }
}
