package com.api.filenet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "filenet")
public class FilenetProperties {

  private final String ceUri;
  private final String username;
  private final String password;
  private final String objectStoreName;
  private final String claseDocumento;
  private final String documentTitle;

  public FilenetProperties(
    String ceUri,
    String username,
    String password,
    String objectStoreName,
    String claseDocumento,
    String documentTitle
  ) {
    this.ceUri = ceUri;
    this.username = username;
    this.password = password;
    this.objectStoreName = objectStoreName;
    this.claseDocumento = claseDocumento;
    this.documentTitle = documentTitle;
  }

  public String getCeUri() {
    return ceUri;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getObjectStoreName() {
    return objectStoreName;
  }

  public String getClaseDocumento() {
    return claseDocumento;
  }

  public String getDocumentTitle() {
    return documentTitle;
  }
}
