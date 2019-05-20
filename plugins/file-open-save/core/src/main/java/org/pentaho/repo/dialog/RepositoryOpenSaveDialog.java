/*
 * Copyright 2017-2018 Hitachi Vantara. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */

package org.pentaho.repo.dialog;

import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.json.simple.JSONObject;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.dialog.ThinDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.platform.settings.ServerPort;
import org.pentaho.platform.settings.ServerPortRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Properties;

import org.pentaho.repo.controller.RepositoryBrowserController;
import org.pentaho.repo.model.RepositoryFile;
import org.pentaho.repo.model.RepositoryTree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Created by bmorrise on 5/23/17.
 */
public class RepositoryOpenSaveDialog extends ThinDialog {

  public static final String STATE_SAVE = "save";
  public static final String STATE_OPEN = "open";
  public static final String SELECT_FOLDER = "selectFolder";
  private final Image LOGO = GUIResource.getInstance().getImageLogoSmall();
  private static final String OSGI_SERVICE_PORT = "OSGI_SERVICE_PORT";
  private static final int OPTIONS = SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX;
  private static final String THIN_CLIENT_HOST = "THIN_CLIENT_HOST";
  private static final String THIN_CLIENT_PORT = "THIN_CLIENT_PORT";
  private static final String LOCALHOST = "localhost";

  private String objectId;
  private String objectName;
  private String objectDirectory;
  private String objectType;
  private Repository repository;

  private RepositoryBrowserController repositoryBrowserController = new RepositoryBrowserController();

  public RepositoryOpenSaveDialog( Shell shell, int width, int height ) {
    super( shell, width, height );
  }

  public void open( Repository repository, String directory, String state, String title, String filter, String origin ) {
    open( repository, directory, state, title, filter, origin, null, "" );
  }

  public void open( Repository repository, String directory, String state, String title, String filter, String origin,
                    String filename, String fileType ) {
    try {
      directory = URLEncoder.encode( directory, "UTF-8" );
    } catch ( Exception e ) {
      // ignore if fails
    }
    RepositoryBrowserController.repository = repository;
    StringBuilder clientPath = new StringBuilder();
    clientPath.append( getClientPath() );
    clientPath.append( !Utils.isEmpty( state ) ? "#/" + state : "" );
    clientPath.append( !Utils.isEmpty( directory ) ? "?path=" + directory : "?" );
    clientPath.append( !Utils.isEmpty( directory ) ? "&" : "" );
    clientPath.append( !Utils.isEmpty( filter ) ? "filter=" + filter : "" );
    clientPath.append( !Utils.isEmpty( filter ) ? "&" : "" );
    clientPath.append( !Utils.isEmpty( origin ) ? "origin=" + origin : "" );
    clientPath.append( !Utils.isEmpty( origin ) ? "&" : "" );
    clientPath.append( null != filename ? "filename=" + filename : "" );
    clientPath.append( null != filename ? "&" : "" );
    clientPath.append( null != fileType ? "fileType=" + fileType : "" );
    super.createDialog( title != null ? title : StringUtils.capitalize( state ), getRepoURL( clientPath.toString() ),
      OPTIONS, LOGO );
    super.dialog.setMinimumSize( 545, 458 );

    new BrowserFunction( browser, "close" ) {
      @Override public Object function( Object[] arguments ) {
        Runnable execute = () -> {
          browser.dispose();
          dialog.close();
          dialog.dispose();
        };
        display.asyncExec( execute );
        return true;
      }
    };

    new BrowserFunction( browser, "select" ) {
      @Override public Object function( Object[] arguments ) {
        objectId = (String) arguments[ 0 ];
        objectName = (String) arguments[ 1 ];
        objectDirectory = (String) arguments[ 2 ];
        objectType = (String) arguments[ 3 ];

        Runnable execute = () -> {
          browser.dispose();
          dialog.close();
          dialog.dispose();
        };
        display.asyncExec( execute );
        return true;
      }
    };

    new BrowserFunction( browser, "bfCheckForSecurityOrDupeIssues" ) {
      @Override public Object function( Object[] arguments ) {
        String path = (String) arguments[ 0 ];
        String name = (String) arguments[ 1 ];
        String fileName = (String) arguments[ 2 ];
        boolean overwrite = (boolean) arguments[ 3 ];
        JSONObject jsonObject = new JSONObject();
        if ( repositoryBrowserController.checkForSecurityOrDupeIssues( path, name, fileName, overwrite ) ) {
          jsonObject.put( "status", Status.OK.getStatusCode() );
          return jsonObject.toString();
        }
        jsonObject.put( "status", Status.NO_CONTENT.getStatusCode() );
        return jsonObject.toString();
      }
    };

    new BrowserFunction( browser, "bfGetRecentFiles" ) {
      @Override public Object function( Object[] arguments ) {
        List<RepositoryFile> files = repositoryBrowserController.getRecentFiles();
        return "{ \"data\": " + javaToJson( files ) + "}";
      }
    };

    new BrowserFunction( browser, "bfGetDirectoryTree" ) {
      @Override public Object function( Object[] arguments ) {
        RepositoryTree repositoryDirectories = repositoryBrowserController.loadDirectoryTree();
        return "{ \"data\": " + javaToJson( repositoryDirectories ) + "}";
      }
    };

    new BrowserFunction( browser, "bfOpenRecent" ) {
      @Override public Object function( Object[] arguments ) {
        String repo = (String) arguments[ 0 ];
        String id = (String) arguments[ 1 ];
        repositoryBrowserController.openRecentFile( repo, id );
        return true;
      }
    };

    new BrowserFunction( browser, "bfCurrentRepo" ) {
      @Override public Object function( Object[] arguments ) {
        return "{ \"data\": " + javaToJson( repositoryBrowserController.getCurrentRepo() ) + "}";
      }
    };

    new BrowserFunction( browser, "bfGetFolders" ) {
      @Override public Object function( Object[] arguments ) {
        String path = (String) arguments[ 0 ];
        return "{ \"data\": " + javaToJson( repositoryBrowserController.loadFolders( path ) ) + "}";
      }
    };

    new BrowserFunction( browser, "bfLoadFilesAndFolders" ) {
      @Override public Object function( Object[] arguments ) {
        String path = (String) arguments[ 0 ];
        return "{ \"data\": " + javaToJson( repositoryBrowserController.loadFilesAndFolders( path ) ) + "}";
      }
    };

    while ( !dialog.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
  }

  private static String getClientPath() {
    Properties properties = new Properties();
    try {
      InputStream inputStream =
        RepositoryOpenSaveDialog.class.getClassLoader().getResourceAsStream( "project.properties" );
      properties.load( inputStream );
    } catch ( IOException e ) {
      e.printStackTrace();
    }
    return properties.getProperty( "CLIENT_PATH" );
  }

  private String javaToJson( Object obj ) {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = "";
    try {
      json = ow.writeValueAsString( obj );
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return json;
  }

  private static Integer getOsgiServicePort() {
    // if no service port is specified try getting it from
    ServerPort osgiServicePort = ServerPortRegistry.getPort( OSGI_SERVICE_PORT );
    if ( osgiServicePort != null ) {
      return osgiServicePort.getAssignedPort();
    }
    return null;
  }

  private static String getRepoURL( String path ) {
    String host;
    Integer port;
    try {
      host = getKettleProperty( THIN_CLIENT_HOST );
      port = Integer.valueOf( getKettleProperty( THIN_CLIENT_PORT ) );
    } catch ( Exception e ) {
      host = LOCALHOST;
      port = getOsgiServicePort();
    }
    return System.getProperty( "KETTLE_CONTEXT_PATH" ) + "/osgi" + path;
  }

  private static String getKettleProperty( String propertyName ) throws KettleException {
    // loaded in system properties at startup
    return System.getProperty( propertyName );
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId( String objectId ) {
    this.objectId = objectId;
  }

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName( String transName ) {
    this.objectName = objectName;
  }

  public String getObjectDirectory() {
    return objectDirectory;
  }

  public void setObjectDirectory( String objectDirectory ) {
    this.objectDirectory = objectDirectory;
  }

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType( String objectType ) {
    this.objectType = objectType;
  }
}
