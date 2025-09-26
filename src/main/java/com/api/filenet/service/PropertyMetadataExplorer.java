package com.api.filenet.service;

import com.filenet.api.admin.Choice;
import com.filenet.api.admin.ChoiceList;
import com.filenet.api.collection.PropertyDescriptionList;
import com.filenet.api.constants.TypeID;
import com.filenet.api.core.*;
import com.filenet.api.meta.*;
import com.filenet.api.util.UserContext;
import javax.security.auth.Subject;

public class PropertyMetadataExplorer {

  public static void main(String[] args) {
    String uri = "http://52.118.253.28:9080/wsi/FNCEWS40MTOM/";
    String username = "usrvfnadm";
    String password = "adminfn654";
    String objectStoreName = "UCSG-DEV";

    Connection conn = Factory.Connection.getConnection(uri);
    Subject subject = UserContext.createSubject(conn, username, password, null);
    UserContext.get().pushSubject(subject);

    try {
      Domain domain = Factory.Domain.fetchInstance(conn, null, null);
      ObjectStore os = Factory.ObjectStore.fetchInstance(
        domain,
        objectStoreName,
        null
      );

      String className = "resolucionesDoc";

      ClassDescription cd = Factory.ClassDescription.fetchInstance(
        os,
        className,
        null
      );

      PropertyDescriptionList pds = cd.get_PropertyDescriptions();

      System.out.println(
        "Propiedades personalizadas de la clase " + className + ":"
      );

      for (Object pdObj : pds) {
        PropertyDescription pd = (PropertyDescription) pdObj;

        // Mostrar solo propiedades personalizadas (no del sistema)
        if (!pd.get_IsSystemOwned()) {
          String propName = pd.get_SymbolicName();
          if (
            !propName.startsWith("DocumentTitle") &&
            !propName.startsWith("EntryTemplate") &&
            !propName.startsWith("Clb") &&
            !propName.startsWith("ComponentBindingLabel") &&
            !propName.startsWith("IgnoreRedirect")
          ) {
            TypeID type = pd.get_DataType();
            System.out.println(" - " + propName + " (" + type.getValue() + ")");

            // Verificar si tiene ChoiceList asociada
            ChoiceList choiceList = pd.get_ChoiceList();

            if (choiceList != null) {
              System.out.println("   Valores permitidos:");
              for (Object choiceObj : choiceList.get_ChoiceValues()) {
                Choice choice = (Choice) choiceObj;
                System.out.println(
                  "    * " +
                  choice.get_DisplayName() +
                  " = " +
                  choice.get_ChoiceStringValue()
                );
              }
            }
          }
        }
      }
    } finally {
      UserContext.get().popSubject();
    }
  }
}
