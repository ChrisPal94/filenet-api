package com.api.filenet.service;

import com.filenet.api.collection.FolderSet;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.property.Properties;
import com.filenet.api.util.UserContext;
import javax.security.auth.Subject;

public class CreateFolder {

  public static void main(String[] args) {
    // Replace with your FileNet connection details
    String uri = "http://52.118.253.28:9080/wsi/FNCEWS40MTOM/";
    String username = "usrvfnadm";
    String password = "UcSG.241014$";
    String objectStoreName = "UCSG-1";
    String newFolderName = "NewFolder";
    String parentFolderPath = "/Prueba_UCSG"; // Root folder

    Connection conn = null;
    Subject sub = null;

    try {
      conn = Factory.Connection.getConnection(uri);
      sub = UserContext.createSubject(conn, username, password, null);
      UserContext.get().pushSubject(sub);

      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore os = Factory.ObjectStore.fetchInstance(
        domain,
        objectStoreName,
        null
      );

      // Get the parent folder
      Folder parentFolder = Factory.Folder.fetchInstance(
        os,
        parentFolderPath,
        null
      );

      // Create the new folder
      Folder newFolder = Factory.Folder.createInstance(os, null);
      newFolder.getProperties().putValue("FolderName", newFolderName);
      newFolder.set_Parent(parentFolder);
      newFolder.save(null);

      System.out.println(
        "Folder '" +
        newFolderName +
        "' created successfully in '" +
        parentFolderPath +
        "'."
      );
    } catch (EngineRuntimeException e) {
      System.err.println("An error occurred: " + e.getMessage());
    } finally {
      if (sub != null) {
        UserContext.get().popSubject();
      }
      if (conn != null) {
        // No explicit connection close method in FileNet API
      }
    }
  }
}
