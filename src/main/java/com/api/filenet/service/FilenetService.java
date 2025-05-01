package com.api.filenet.service;

import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.UserContext;
import java.util.Iterator;
import javax.security.auth.Subject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class FilenetService {

  private static final String CE_URI =
    "http://52.118.253.28:9080/wsi/FNCEWS40MTOM/";
  private static final String USERNAME = "usrvfnadm";
  private static final String PASSWORD = "UcSG.241014$";
  private static final String OBJECT_STORE_NAME = "UCSG-1";

  public String connectAndFetch() {
    try {
      Connection conn = Factory.Connection.getConnection(CE_URI);
      Subject subject = UserContext.createSubject(
        conn,
        USERNAME,
        PASSWORD,
        null
      );
      UserContext.get().pushSubject(subject);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        OBJECT_STORE_NAME,
        null
      );

      return "Conectado a Object Store: " + store.get_DisplayName();
    } catch (Exception e) {
      e.printStackTrace();
      return "Error conectando a FileNet: " + e.getMessage();
    }
  }

  public String contarDocumentos() {
    try {
      Connection conn = Factory.Connection.getConnection(CE_URI);
      Subject subject = UserContext.createSubject(
        conn,
        USERNAME,
        PASSWORD,
        null
      );
      UserContext.get().pushSubject(subject);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        OBJECT_STORE_NAME,
        null
      );

      SearchSQL sql = new SearchSQL();
      sql.setQueryString("SELECT Id FROM Document"); // sin COUNT(*)

      SearchScope scope = new SearchScope(store);
      RepositoryRowSet rows = scope.fetchRows(sql, null, null, null);

      int count = 0;
      Iterator<?> iterator = rows.iterator();
      while (iterator.hasNext()) {
        iterator.next();
        count++;
      }

      return "Total de documentos: " + count;
    } catch (Exception e) {
      e.printStackTrace();
      return "Error contando documentos: " + e.getMessage();
    }
  }

  public String listarNombresDeCarpetas() {
    JSONArray carpetas = new JSONArray();

    try {
      Connection conn = Factory.Connection.getConnection(CE_URI);
      Subject subject = UserContext.createSubject(
        conn,
        USERNAME,
        PASSWORD,
        null
      );
      UserContext.get().pushSubject(subject);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        OBJECT_STORE_NAME,
        null
      );

      SearchSQL sql = new SearchSQL();
      sql.setQueryString("SELECT This FROM Folder");

      SearchScope scope = new SearchScope(store);
      RepositoryRowSet rows = scope.fetchRows(sql, null, null, null);

      Iterator<?> iterator = rows.iterator();
      while (iterator.hasNext()) {
        RepositoryRow row = (RepositoryRow) iterator.next();
        Folder folder = (Folder) row.getProperties().getObjectValue("This");

        String nombre = folder.get_FolderName();
        if (nombre == null || nombre.trim().isEmpty()) {
          nombre = "raiz";
        }

        String ruta = construirRutaCompleta(folder);
        String id = folder.get_Id().toString();
        String path = folder.get_PathName();

        JSONObject carpeta = new JSONObject();
        carpeta.put("nombre", nombre);
        carpeta.put("ruta", ruta);
        carpeta.put("id", id);
        carpeta.put("path", path);

        carpetas.put(carpeta);
      }

      JSONObject resultado = new JSONObject();
      resultado.put("carpetas", carpetas);
      return resultado.toString();
    } catch (Exception e) {
      e.printStackTrace();
      JSONObject error = new JSONObject();
      error.put("error", "Error listando carpetas: " + e.getMessage());
      return error.toString();
    }
  }

  private String construirRutaCompleta(Folder folder) throws Exception {
    StringBuilder ruta = new StringBuilder();
    Folder current = folder;
    while (current != null) {
      String nombre = current.get_FolderName();
      if (nombre == null || nombre.trim().isEmpty()) {
        nombre = "raiz";
      }
      ruta.insert(0, "/" + nombre);
      current = current.get_Parent();
    }
    return ruta.toString();
  }
}
