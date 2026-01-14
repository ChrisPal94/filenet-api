package com.api.filenet.service;

import com.api.filenet.config.FilenetProperties;
import com.api.filenet.dto.ChoiceDTO;
import com.api.filenet.dto.CustomResponse;
import com.api.filenet.dto.PropertyChoicesDTO;
import com.api.filenet.exceptions.ErrorException;
import com.filenet.api.admin.Choice;
import com.filenet.api.admin.ChoiceList;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.EngineCollection;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.collection.PropertyDescriptionList;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.core.Connection;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.exception.ExceptionCode;
import com.filenet.api.meta.ClassDescription;
import com.filenet.api.meta.PropertyDescription;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FilenetService {

  // @Value("${filenet.ce.uri}")
  // private String ceUri;

  private final FilenetProperties properties;

  public FilenetService(FilenetProperties properties) {
    this.properties = properties;
  }

  // private static final String CE_URI =
  //   "http://52.118.253.28:9080/wsi/FNCEWS40MTOM/";
  // private static final String USERNAME = "usrvfnadm";
  // private static final String PASSWORD = "UcSG.241014$";
  // private static final String OBJECT_STORE_NAME = "UCSG-1";
  //private static final String CLASE_DOCUMENTO = "Document";
  //private static final String DOCUMENT_TITLE = "DocumentTitle";

  private static final Logger logger = LoggerFactory.getLogger(
    FilenetService.class
  );

  public String connectAndFetch() {
    try {
      Connection conn = Factory.Connection.getConnection(properties.getCeUri());
      Subject subject = UserContext.createSubject(
        conn,
        properties.getUsername(),
        properties.getPassword(),
        null
      );
      UserContext.get().pushSubject(subject);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
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
      Connection conn = Factory.Connection.getConnection(properties.getCeUri());
      Subject subject = UserContext.createSubject(
        conn,
        properties.getUsername(),
        properties.getPassword(),
        null
      );
      UserContext.get().pushSubject(subject);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
        null
      );

      SearchSQL sql = new SearchSQL();
      sql.setQueryString("SELECT Id FROM Document");

      SearchScope scope = new SearchScope(store);
      RepositoryRowSet rows = scope.fetchRows(sql, null, null, null);

      int count = 0;
      Iterator<?> iterator = rows.iterator();
      while (iterator.hasNext()) {
        iterator.next();
        count++;
      }

      return "Total de documentos: " + count;
    } catch (ErrorException e) {
      e.printStackTrace();
      return "Error contando documentos: " + e.getMessage();
    }
  }

  public String listarNombresDeCarpetas() {
    JSONArray carpetas = new JSONArray();

    try {
      Connection conn = Factory.Connection.getConnection(properties.getCeUri());
      Subject subject = UserContext.createSubject(
        conn,
        properties.getUsername(),
        properties.getPassword(),
        null
      );
      UserContext.get().pushSubject(subject);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
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

  private String construirRutaCompleta(Folder folder) {
    if (folder == null) {
      throw new ErrorException("Folder no puede ser null");
    }
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

  @SuppressWarnings("unchecked")
  public void subirDocumentoDesdeApi(
    MultipartFile file,
    String rutaCarpeta,
    String nombreDocumento,
    Map<String, String> metadata
  ) throws ErrorException {
    try {
      // Conexión y autenticación
      Connection conn = Factory.Connection.getConnection(properties.getCeUri());
      Subject subject = UserContext.createSubject(
        conn,
        properties.getUsername(),
        properties.getPassword(),
        null
      );
      UserContext.get().pushSubject(subject);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
        null
      );

      Folder folderDestino = crearCarpeta(store, rutaCarpeta);
      logger.info("Destino OK: {}", folderDestino.get_PathName());

      Document documentoExistente = buscarDocumentoPorNombre(
        folderDestino,
        nombreDocumento
      );

      Document doc;

      if (documentoExistente != null) {
        logger.info("📝 Documento ya existe, se creará nueva versión");

        // Check-out para versionar
        documentoExistente.checkout(
          ReservationType.EXCLUSIVE,
          null,
          null,
          null
        );
        documentoExistente.save(RefreshMode.REFRESH);

        doc = (Document) documentoExistente.get_Reservation();
      } else {
        logger.info("🆕 Documento nuevo");
        doc = Factory.Document.createInstance(
          store,
          properties.getClaseDocumento(),
          null
        );
      }

      // Crear contenido
      ContentTransfer ct = Factory.ContentTransfer.createInstance();
      ct.setCaptureSource(file.getInputStream());
      ct.set_RetrievalName(file.getOriginalFilename());

      ContentElementList cel = Factory.ContentElement.createList();
      cel.add(ct);

      // Asignar propiedades
      doc.set_ContentElements(cel);
      doc.set_MimeType(file.getContentType());
      doc
        .getProperties()
        .putValue(properties.getDocumentTitle(), nombreDocumento);

      // 5. Agregar metadatos personalizados
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        String propName = entry.getKey();
        String propValue = entry.getValue();
        try {
          if ("FechaDocumento".equals(propName)) {
            // Ajusta el formato al que envías desde el front (ej: yyyy-MM-dd)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fecha = sdf.parse(propValue);
            doc.getProperties().putValue(propName, fecha);
          } else {
            // el resto se cargan como String
            doc.getProperties().putValue(propName, propValue);
          }
        } catch (Exception ex) {
          System.out.println(
            "⚠️ Propiedad no encontrada o tipo inválido: " + propName
          );
        }
        /*try {
          doc.getProperties().putValue(propName, propValue);
        } catch (EngineRuntimeException ex) {
          System.out.println("⚠️ Propiedad no encontrada: " + propName);
        }*/
      }

      doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
      doc.save(RefreshMode.REFRESH);

      // Archivar el documento en la carpeta
      folderDestino
        .file(
          doc,
          AutoUniqueName.AUTO_UNIQUE,
          nombreDocumento,
          DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE
        )
        .save(RefreshMode.REFRESH);

      logger.info("Documento guardado con ID: {}", doc.get_Id());
      logger.info("Archivado en carpeta: {}", folderDestino.get_PathName());
    } catch (Exception e) {
      throw new ErrorException(
        "Error al subir y archivar el documento: " + e.getMessage(),
        e
      );
    }
  }

  private static Document buscarDocumentoPorNombre(
    Folder carpeta,
    String nombreDocumento
  ) {
    try {
      DocumentSet docs = carpeta.get_ContainedDocuments();
      Iterator<?> iter = docs.iterator();

      while (iter.hasNext()) {
        Document doc = (Document) iter.next();
        String titulo = doc.getProperties().getStringValue("DocumentTitle");
        if (titulo != null && titulo.equalsIgnoreCase(nombreDocumento)) {
          return doc;
        }
      }
    } catch (Exception e) {
      logger.warn("Error buscando documento: {}", e.getMessage());
    }
    return null;
  }

  private static Folder crearCarpeta(ObjectStore store, String rutaCarpeta)
    throws ErrorException {
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
        logger.info(String.format("📁 Carpeta ya existe: %s", pathActual));
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
        logger.info("🆕 Carpeta creada: {}", pathActual);
      }

      parent = currentFolder; // Actualizamos la carpeta padre para la siguiente iteración
    }

    return parent; // Retornamos la última carpeta creada o existente
  }

  public Document obtenerDocumentoPorId(String idDocumento)
    throws ErrorException {
    // Conectarse a FileNet
    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    // Obtener el dominio y el object store
    Domain domain = Factory.Domain.fetchInstance(conn, null, null);
    ObjectStore store = Factory.ObjectStore.fetchInstance(
      domain,
      properties.getObjectStoreName(),
      null
    );

    // Buscar el documento por su ID
    Id id = new Id(idDocumento); // Convertir el String al tipo Id
    Document doc = Factory.Document.fetchInstance(store, id, null);

    // Imprimir algunos datos para verificar
    logger.info("Documento encontrado:");
    logger.info("ID: {}", doc.get_Id());
    logger.info(
      "Nombre: {}",
      doc.getProperties().getStringValue(properties.getDocumentTitle())
    );
    logger.info("Clase: {}", doc.get_ClassDescription().get_DisplayName());

    return doc;
  }

  public List<String> obtenerUbicacionDocumento(String idDocumento)
    throws ErrorException {
    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    Domain domain = Factory.Domain.fetchInstance(conn, null, null);
    ObjectStore store = Factory.ObjectStore.fetchInstance(
      domain,
      properties.getObjectStoreName(),
      null
    );

    String query =
      "SELECT Head FROM ReferentialContainmentRelationship " +
      "WHERE Tail = Object('" +
      idDocumento +
      "')";

    SearchSQL sql = new SearchSQL(query);
    SearchScope scope = new SearchScope(store);

    IndependentObjectSet results = scope.fetchObjects(sql, null, null, false);

    boolean found = false;
    Iterator<?> it = results.iterator();
    while (it.hasNext()) {
      ReferentialContainmentRelationship rel =
        (ReferentialContainmentRelationship) it.next();
      Folder folder = (Folder) rel.get_Head();
      logger.info("Documento archivado en carpeta: {}", folder.get_PathName());
      found = true;
    }

    if (!found) {
      logger.info("El documento no está archivado en ninguna carpeta.");
    }
    return List.of();
  }

  public Pair<InputStream, String> obtenerContenido(
    String rutaCarpeta,
    String nombreDocumento
  ) throws ErrorException {
    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    Domain domain = Factory.Domain.fetchInstance(conn, null, null);
    ObjectStore store = Factory.ObjectStore.fetchInstance(
      domain,
      properties.getObjectStoreName(),
      null
    );

    Folder folder = fetchFolder(store, rutaCarpeta);
    Document doc = findDocumentInFolder(folder, nombreDocumento);

    return getDocumentContent(doc);
  }

  private Folder fetchFolder(ObjectStore store, String rutaCarpeta)
    throws ErrorException {
    try {
      Folder folder = Factory.Folder.fetchInstance(store, rutaCarpeta, null);
      folder.refresh();
      return folder;
    } catch (Exception e) {
      throw new ErrorException("Error fetching folder: " + e.getMessage(), e);
    }
  }

  private Document findDocumentInFolder(Folder folder, String nombreDocumento)
    throws ErrorException {
    DocumentSet documentos = folder.get_ContainedDocuments();
    for (Iterator<?> it = documentos.iterator(); it.hasNext();) {
      Object obj = it.next();
      if (obj instanceof Document d) {
        try {
          String titulo = d
            .getProperties()
            .getStringValue(properties.getDocumentTitle());
          if (titulo != null && titulo.equals(nombreDocumento)) {
            logger.info("✅ Documento encontrado: {}", titulo);
            return d;
          }
        } catch (Exception e) {
          logger.info(
            "❌ Error al acceder a DocumentTitle: {}",
            e.getMessage()
          );
        }
      }
    }
    throw new ErrorException("Documento no encontrado: " + nombreDocumento);
  }

  private Pair<InputStream, String> getDocumentContent(Document doc)
    throws ErrorException {
    try {
      ContentElementList contents = doc.get_ContentElements();
      if (contents == null || contents.isEmpty()) {
        throw new ErrorException("Documento sin contenido");
      }

      ContentTransfer ct = (ContentTransfer) contents.get(0);
      InputStream is = ct.accessContentStream();

      String mimeType = doc.getProperties().getStringValue("MimeType");
      if (mimeType == null || mimeType.isEmpty()) {
        mimeType = "application/octet-stream";
      }

      return Pair.of(is, mimeType);
    } catch (Exception e) {
      throw new ErrorException(
        "Error retrieving document content: " + e.getMessage(),
        e
      );
    }
  }

  public boolean eliminarDocumento(String rutaCarpeta, String nombreDocumento)
    throws ErrorException {
    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    Domain domain = Factory.Domain.fetchInstance(conn, null, null);
    ObjectStore store = Factory.ObjectStore.fetchInstance(
      domain,
      properties.getObjectStoreName(),
      null
    );

    Folder folder = Factory.Folder.fetchInstance(store, rutaCarpeta, null);
    folder.refresh();

    EngineCollection documentos = folder.get_ContainedDocuments();
    Document docAEliminar = null;

    Iterator<?> iterator = documentos.iterator();
    while (iterator.hasNext()) {
      Document doc = (Document) iterator.next();
      String titulo = doc
        .getProperties()
        .getStringValue(properties.getDocumentTitle());
      logger.info("🔍 Documento en carpeta: {}", titulo);
      if (titulo.equals(nombreDocumento)) {
        docAEliminar = doc;
        logger.info("✅ Documento a eliminar encontrado: {}", titulo);
        break;
      }
    }

    if (docAEliminar == null) {
      logger.info("❌ Documento no encontrado para eliminar.");
      return false;
    }

    docAEliminar.delete();
    docAEliminar.save(RefreshMode.NO_REFRESH);
    logger.info("🗑️ Documento eliminado correctamente.");
    return true;
  }

  public String eliminarCarpetaConContenido(String rutaCarpeta)
    throws ErrorException {
    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    Domain domain = Factory.Domain.fetchInstance(conn, null, null);
    ObjectStore store = Factory.ObjectStore.fetchInstance(
      domain,
      properties.getObjectStoreName(),
      null
    );

    try {
      Folder carpeta = Factory.Folder.fetchInstance(store, rutaCarpeta, null);
      carpeta.refresh();

      // Eliminar todos los documentos contenidos
      DocumentSet documentos = carpeta.get_ContainedDocuments();
      for (Iterator<?> it = documentos.iterator(); it.hasNext();) {
        Document doc = (Document) it.next();
        doc.delete();
        doc.save(RefreshMode.NO_REFRESH);
      }

      // Eliminar subcarpetas si hay
      FolderSet subcarpetas = carpeta.get_SubFolders();
      for (Iterator<?> it = subcarpetas.iterator(); it.hasNext();) {
        Folder sub = (Folder) it.next();
        sub.delete();
        sub.save(RefreshMode.NO_REFRESH);
      }

      // Finalmente eliminar la carpeta
      carpeta.delete();
      carpeta.save(RefreshMode.NO_REFRESH);
      return "✅ Carpeta eliminada correctamente.";
    } catch (EngineRuntimeException e) {
      if (e.getExceptionCode().equals(ExceptionCode.E_OBJECT_NOT_FOUND)) {
        return "❌ La carpeta no existe.";
      }
      throw e;
    }
  }

  public List<Map<String, Object>> getCustomPropertiesAll(String className) {
    List<Map<String, Object>> result = new ArrayList<>();

    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    try {
      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
        null
      );

      ClassDescription cd = Factory.ClassDescription.fetchInstance(
        store,
        className,
        null
      );
      PropertyDescriptionList pds = cd.get_PropertyDescriptions();

      for (Object pdObj : pds) {
        PropertyDescription pd = (PropertyDescription) pdObj;

        // Solo propiedades personalizadas (no del sistema)
        if (!pd.get_IsSystemOwned()) {
          String propName = pd.get_SymbolicName();
          if (
            !propName.startsWith("DocumentTitle") &&
            !propName.startsWith("EntryTemplate") &&
            !propName.startsWith("Clb") &&
            !propName.startsWith("ComponentBindingLabel") &&
            !propName.startsWith("IgnoreRedirect")
          ) {
            Map<String, Object> propInfo = new LinkedHashMap<>();
            propInfo.put("name", propName);
            propInfo.put("type", pd.get_DataType().toString());

            // Si tiene ChoiceList asociada, lo agregamos
            ChoiceList choiceList = pd.get_ChoiceList();
            if (choiceList != null) {
              List<Map<String, String>> choices = new ArrayList<>();
              for (Object choiceObj : choiceList.get_ChoiceValues()) {
                Choice choice = (Choice) choiceObj;
                Map<String, String> c = new HashMap<>();
                c.put("displayName", choice.get_DisplayName());
                c.put("value", choice.get_ChoiceStringValue());
                choices.add(c);
              }
              propInfo.put("choices", choices);
            }

            result.add(propInfo);
          }
        }
      }
    } finally {
      UserContext.get().popSubject();
    }

    return result;
  }

  public CustomResponse getCustomProperties(String className) {
    List<PropertyChoicesDTO> propertiesWithChoices = new ArrayList<>();

    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    try {
      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore store = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
        null
      );

      ClassDescription cd = Factory.ClassDescription.fetchInstance(
        store,
        className,
        null
      );
      PropertyDescriptionList pds = cd.get_PropertyDescriptions();

      for (Object pdObj : pds) {
        PropertyDescription pd = (PropertyDescription) pdObj;

        if (!pd.get_IsSystemOwned()) {
          ChoiceList choiceList = pd.get_ChoiceList();

          // Solo devolver propiedades que tengan ChoiceList
          if (choiceList != null) {
            List<ChoiceDTO> choices = new ArrayList<>();
            for (Object choiceObj : choiceList.get_ChoiceValues()) {
              Choice choice = (Choice) choiceObj;
              choices.add(
                new ChoiceDTO(
                  choice.get_DisplayName(),
                  choice.get_ChoiceStringValue()
                )
              );
            }

            propertiesWithChoices.add(
              new PropertyChoicesDTO(pd.get_SymbolicName(), choices)
            );
          }
        }
      }
    } finally {
      UserContext.get().popSubject();
    }

    return new CustomResponse(true, propertiesWithChoices);
  }

  public List<Map<String, Object>> buscarDocumentos(
    String nombreDocumento,
    String fechaDocumento,
    String enteRegulatorio,
    String anio,
    String tipoDocumento
  ) throws Exception {
    List<Map<String, Object>> resultados = new ArrayList<>();

    Connection conn = Factory.Connection.getConnection(properties.getCeUri());
    Subject subject = UserContext.createSubject(
      conn,
      properties.getUsername(),
      properties.getPassword(),
      null
    );
    UserContext.get().pushSubject(subject);

    try {
      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore os = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
        null
      );

      // Construir query
      StringBuilder query = new StringBuilder(
        "SELECT d.DocumentTitle, d.FechaDocumento, d.Nombre, d.EnteRegulatorio, d.Id, d.Anio, d.TipodeDocumentoResolucion " +
        "FROM resolucionesDoc d WHERE 1=1"
      );

      if (nombreDocumento != null && !nombreDocumento.isEmpty()) {
        query
          .append(" AND d.DocumentTitle LIKE '%")
          .append(nombreDocumento)
          .append("%'");
      }
      if (fechaDocumento != null && !fechaDocumento.isEmpty()) {
        query
          .append(" AND d.FechaDocumento = DATE('")
          .append(fechaDocumento)
          .append("')");
      }
      if (enteRegulatorio != null && !enteRegulatorio.isEmpty()) {
        query
          .append(" AND d.EnteRegulatorio LIKE '%")
          .append(enteRegulatorio)
          .append("%'");
      }
      if (anio != null && !anio.isEmpty()) {
        query.append(" AND d.Anio LIKE '%").append(anio).append("%'");
      }
      if (tipoDocumento != null && !tipoDocumento.isEmpty()) {
        query
          .append(" AND d.TipodeDocumentoResolucion LIKE '%")
          .append(tipoDocumento)
          .append("%'");
      }

      SearchSQL sql = new SearchSQL(query.toString());
      SearchScope scope = new SearchScope(os);

      RepositoryRowSet rowSet = scope.fetchRows(
        sql,
        null,
        null,
        Boolean.valueOf(true)
      );
      Iterator<?> it = rowSet.iterator();

      while (it.hasNext()) {
        RepositoryRow row = (RepositoryRow) it.next();
        Map<String, Object> doc = new HashMap<>();
        doc.put(
          "DocumentTitle",
          row.getProperties().getStringValue("DocumentTitle")
        );
        doc.put(
          "FechaDocumento",
          row.getProperties().getDateTimeValue("FechaDocumento")
        );
        String nombre = row.getProperties().getStringValue("Nombre");
        doc.put("Nombre", nombre);
        doc.put(
          "EnteRegulatorio",
          row.getProperties().getStringValue("EnteRegulatorio")
        );
        doc.put("Anio", row.getProperties().getStringValue("Anio"));
        doc.put(
          "TipoDocumento",
          row.getProperties().getStringValue("TipodeDocumentoResolucion")
        );
        String id = row.getProperties().getIdValue("Id").toString();
        doc.put("Id", id);

        // 🔹 Obtener extensión del contenido
        Document documento = Factory.Document.fetchInstance(
          os,
          new Id(id),
          null
        );
        ContentElementList contenidos = documento.get_ContentElements();
        if (contenidos != null && !contenidos.isEmpty()) {
          ContentTransfer ct = (ContentTransfer) contenidos.get(0);
          String retrievalName = ct.get_RetrievalName(); // nombre original con extensión
          if (retrievalName != null && retrievalName.contains(".")) {
            String extension = retrievalName.substring(
              retrievalName.lastIndexOf(".") + 1
            );

            // Guardar extensión sola
            doc.put("Extension", extension);

            // Guardar nombre + extensión
            doc.put("NombreCompleto", nombre + "." + extension);
          } else {
            doc.put("Extension", "desconocido");
            doc.put("NombreCompleto", nombre);
          }
        } else {
          doc.put("Extension", "sin contenido");
          doc.put("NombreCompleto", nombre);
        }

        resultados.add(doc);
      }
    } finally {
      UserContext.get().popSubject();
    }

    return resultados;
  }

  public ResponseEntity<Resource> descargarDocumento(String docId) {
    try {
      Connection conn = Factory.Connection.getConnection(properties.getCeUri());
      Subject subject = UserContext.createSubject(
        conn,
        properties.getUsername(),
        properties.getPassword(),
        null
      );
      UserContext.get().pushSubject(subject);
      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore os = Factory.ObjectStore.fetchInstance(
        domain,
        properties.getObjectStoreName(),
        null
      );

      // Buscar el documento por ID
      Document doc = Factory.Document.fetchInstance(os, new Id(docId), null);

      // Obtener el contenido
      ContentElementList contentList = doc.get_ContentElements();
      if (contentList == null || contentList.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      ContentTransfer ct = (ContentTransfer) contentList.get(0);
      InputStream inputStream = ct.accessContentStream();

      // Preparar respuesta HTTP
      InputStreamResource resource = new InputStreamResource(inputStream);

      return ResponseEntity.ok()
        .header(
          HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename=\"" + doc.get_Name() + "\""
        )
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().body(null);
    } finally {
      UserContext.get().popSubject();
    }
  }
}
