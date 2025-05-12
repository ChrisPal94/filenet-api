package com.api.filenet.service;

import com.filenet.api.collection.*;
import com.filenet.api.constants.*;
import com.filenet.api.core.*;
import com.filenet.api.util.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.security.auth.Subject;

public class FilenetTestRunner {

  private static final String CE_URI =
    "http://52.118.253.28:9080/wsi/FNCEWS40MTOM/";
  private static final String USERNAME = "usrvfnadm";
  private static final String PASSWORD = "UcSG.241014$";
  private static final String OBJECT_STORE_NAME = "UCSG-1";
  private static final String CLASE_DOCUMENTO = "Document";

  public static void main(String[] args) {
    try {
      subirDocumentoDePrueba();
    } catch (Exception e) {
      System.err.println("Error al subir documento: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  public static void subirDocumentoDePrueba() throws Exception {
    String rutaCarpeta = "/TEST/D/E/F";
    String nombreDocumento = "archivo-prueba.txt";
    String contenido = "Hola desde FileNet";

    // Conexión
    Connection conn = Factory.Connection.getConnection(CE_URI);
    Subject subject = UserContext.createSubject(conn, USERNAME, PASSWORD, null);
    UserContext.get().pushSubject(subject);

    Domain domain = Factory.Domain.fetchInstance(conn, null, null);
    ObjectStore store = Factory.ObjectStore.fetchInstance(
      domain,
      OBJECT_STORE_NAME,
      null
    );

    // Crear carpeta anidada
    Folder folderDestino = crearCarpeta(store, rutaCarpeta);
    System.out.println("Destino OK: " + folderDestino.get_PathName());

    // Crear documento
    Document doc = Factory.Document.createInstance(
      store,
      CLASE_DOCUMENTO,
      null
    );

    InputStream is = new ByteArrayInputStream(
      contenido.getBytes(StandardCharsets.UTF_8)
    );
    ContentTransfer ct = Factory.ContentTransfer.createInstance();
    ct.setCaptureSource(is);
    ct.set_RetrievalName(nombreDocumento);

    ContentElementList cel = Factory.ContentElement.createList();
    cel.add(ct);

    doc.set_ContentElements(cel);
    doc.set_MimeType("text/plain");
    doc.getProperties().putValue("DocumentTitle", nombreDocumento);

    doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
    doc.save(RefreshMode.REFRESH);

    folderDestino
      .file(
        doc,
        AutoUniqueName.AUTO_UNIQUE,
        nombreDocumento,
        DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE
      )
      .save(RefreshMode.REFRESH);

    System.out.println(
      "✅ Documento subido y archivado con ID: " + doc.get_Id()
    );
  }

  private static Folder crearCarpeta(ObjectStore store, String rutaCarpeta)
    throws Exception {
    String[] partes = rutaCarpeta.split("/");
    Folder parent = Factory.Folder.fetchInstance(store, "/", null); // raíz

    // Nos aseguramos de que la carpeta raíz esté completamente cargada
    parent.refresh();

    for (String parte : partes) {
      if (parte == null || parte.trim().isEmpty()) continue;

      // Construir la ruta actual sin agregar barras innecesarias
      String pathActual = parent.get_PathName();
      if (!pathActual.endsWith("/")) {
        pathActual += "/";
      }
      pathActual += parte;

      Folder currentFolder;

      try {
        // Intentamos obtener la carpeta si ya existe
        currentFolder = Factory.Folder.fetchInstance(store, pathActual, null);
        currentFolder.refresh(); // Aseguramos que la carpeta tenga todas las propiedades cargadas
        System.out.println("📁 Carpeta ya existe: " + pathActual);
      } catch (Exception e) {
        // Si no existe, la creamos
        Folder nueva = Factory.Folder.createInstance(store, "Folder");
        nueva.getProperties().putValue(PropertyNames.FOLDER_NAME, parte);

        // Establecemos la carpeta 'parent' como la carpeta padre
        nueva.set_Parent(parent);

        // Guardamos la nueva carpeta
        nueva.save(null);
        nueva.refresh(); // Refrescamos la nueva carpeta para asegurarnos de que sus propiedades están actualizadas
        currentFolder = nueva;
        System.out.println("🆕 Carpeta creada: " + pathActual);
      }

      parent = currentFolder; // Actualizamos la carpeta padre para la siguiente iteración
    }

    return parent; // Retornamos la última carpeta creada o existente
  }
}
