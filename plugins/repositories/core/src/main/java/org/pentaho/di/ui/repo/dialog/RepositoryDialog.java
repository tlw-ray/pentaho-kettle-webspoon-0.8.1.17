/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.repo.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.json.simple.JSONObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleAuthException;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.ui.core.database.dialog.DatabaseDialog;
import org.pentaho.di.ui.core.dialog.ThinDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.repo.controller.RepositoryConnectController;
import org.pentaho.di.ui.repo.endpoints.RepositoryEndpoint;
import org.pentaho.di.ui.repo.model.LoginModel;
import org.pentaho.di.ui.repo.model.RepositoryModel;
import org.pentaho.platform.settings.ServerPort;
import org.pentaho.platform.settings.ServerPortRegistry;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RepositoryDialog extends ThinDialog {

  private LogChannelInterface log =
    KettleLogStore.getLogChannelInterfaceFactory().create( RepositoryDialog.class );

  private static Class<?> PKG = RepositoryDialog.class;

  public static final String HELP_URL =
      Const.getDocUrl( BaseMessages.getString( PKG, "RepositoryDialog.Dialog.Help" ) );

  private static final int WIDTH = 630;
  private static final int HEIGHT = 630;
  private static final int OPTIONS = SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM;
  private static final String CREATION_TITLE = BaseMessages.getString( PKG, "RepositoryDialog.Dialog.NewRepo.Title" );
  private static final String CREATION_WEB_CLIENT_PATH = "#/add";
  private static final String MANAGER_TITLE = BaseMessages.getString( PKG, "RepositoryDialog.Dialog.Manager.Title" );
  private static final String LOGIN_TITLE = BaseMessages.getString( PKG, "RepositoryDialog.Dialog.Login.Title" );
  private static final String LOGIN_WEB_CLIENT_PATH = "#/connect";
  private static final String OSGI_SERVICE_PORT = "OSGI_SERVICE_PORT";
  private Image LOGO;


  private RepositoryConnectController controller;
  private Shell shell;
  private boolean result = false;

  public RepositoryDialog( Shell shell, RepositoryConnectController controller ) {
    super( shell, WIDTH, HEIGHT );
    this.controller = controller;
    this.shell = shell;
    this.LOGO = GUIResource.getInstance().getImageLogoSmall();
  }

  private boolean open() {
    return open( null );
  }

  private boolean open( RepositoryMeta repositoryMeta ) {
    return open( repositoryMeta, false, null );
  }

  private boolean open( RepositoryMeta repositoryMeta, boolean relogin, String errorMessage ) {

    new BrowserFunction( browser, "closeWindow" ) {
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

    new BrowserFunction( browser, "bfAddRepository" ) {
      @Override public Object function( Object[] arguments ) {
        String json = (String) arguments[0];
        RepositoryModel model = (RepositoryModel) jsonToObject( json, RepositoryModel.class );
        if ( controller.createRepository( model.getId(), controller.modelToMap( model ) ) != null ) {
          return true;
        } else {
          return BaseMessages.getString( PKG, "RepositoryConnection.Error.InvalidServer" );
        }
      }
    };

    new BrowserFunction( browser, "bfGetRepositories" ) {
      @Override public Object function( Object[] arguments ) {
        return controller.getRepositories();
      }
    };

    new BrowserFunction( browser, "bfHelp" ) {
      @Override public Object function( Object[] objects ) {
        return controller.help();
      }
    };

    new BrowserFunction( browser, "bfUpdate" ) {
      @Override public Object function( Object[] arguments ) {
        String json = (String) arguments[0];
        RepositoryModel model = (RepositoryModel) jsonToObject( json, RepositoryModel.class );
        JSONObject jsonObject = new JSONObject();
        if ( controller.updateRepository( model.getId(), controller.modelToMap( model ) ) ) {
          jsonObject.put( RepositoryConnectController.SUCCESS, true );
        } else {
          jsonObject.put( RepositoryConnectController.MESSAGE,
            BaseMessages.getString( PKG, "RepositoryConnection.Error.InvalidServer" ) );
          jsonObject.put( RepositoryConnectController.SUCCESS, false );
        }
        return jsonObject.toString();
      }
    };

    new BrowserFunction( browser, "bfLogin" ) {
      @Override public Object function( Object[] arguments ) {
        String json = (String) arguments[0];
        LoginModel loginModel = (LoginModel) jsonToObject( json, LoginModel.class );
        JSONObject jsonObject = new JSONObject();
        try {
          if ( controller.isRelogin() ) {
            controller
              .reconnectToRepository( loginModel.getRepositoryName(), loginModel.getUsername(), loginModel.getPassword() );
          } else {
            controller
              .connectToRepository( loginModel.getRepositoryName(), loginModel.getUsername(), loginModel.getPassword() );
          }
          jsonObject.put( RepositoryConnectController.SUCCESS, true );
        } catch ( Exception e ) {
          if ( e.getMessage().contains( RepositoryEndpoint.ERROR_401 ) || e instanceof KettleAuthException ) {
            jsonObject.put( RepositoryConnectController.MESSAGE,
              BaseMessages.getString( RepositoryEndpoint.class, "RepositoryConnection.Error.InvalidCredentials" ) );
          } else {
            jsonObject.put( RepositoryConnectController.MESSAGE,
              BaseMessages.getString( RepositoryEndpoint.class, "RepositoryConnection.Error.InvalidServer" ) );
          }
          jsonObject.put( RepositoryConnectController.SUCCESS, false );
        }
        return jsonObject.toString();
      }
    };

    new BrowserFunction( browser, "bfGetRepository" ) {
      @Override public Object function( Object[] objects ) {
        return controller.getRepository( (String) objects[ 0 ] );
      }
    };

    new BrowserFunction( browser, "bfCheckDuplicate" ) {
      @Override public Object function( Object[] arguments ) {
        String displayName = (String) arguments[0];
        return controller.checkDuplicate( displayName );
      }
    };

    new BrowserFunction( browser, "bfGetDatabases" ) {
      @Override public Object function( Object[] arguments ) {
        return controller.getDatabases();
      }
    };

    new BrowserFunction( browser, "bfCreateNewConnection" ) {
      @Override public Object function( Object[] arguments ) {
        DatabaseDialog databaseDialog = new DatabaseDialog( shell, new DatabaseMeta() );
        databaseDialog.open();
        DatabaseMeta databaseMeta = databaseDialog.getDatabaseMeta();
        JSONObject jsonObject = new JSONObject();
        if ( databaseMeta != null ) {
          if ( !controller.isDatabaseWithNameExist( databaseMeta, true ) ) {
            controller.addDatabase( databaseMeta );
          } else {
            DatabaseDialog.showDatabaseExistsDialog( shell, databaseMeta );
          }
          jsonObject.put( "name", databaseMeta.getName() );
        } else {
          jsonObject.put( "name", "None" );
        }
        return jsonObject.toJSONString();
      }
    };

    new BrowserFunction( browser, "bfEditConnection" ) {
      @Override public Object function( Object[] arguments ) {
        String database = (String) arguments[0];
        DatabaseMeta databaseMeta = controller.getDatabase( database );
        String originalName = databaseMeta.getName();
        DatabaseDialog databaseDialog = new DatabaseDialog( shell, databaseMeta );
        databaseDialog.open();
        JSONObject jsonObject = new JSONObject();
        if ( !controller.isDatabaseWithNameExist( databaseMeta, false ) ) {
          controller.save();
          jsonObject.put( "name", databaseMeta.getName() );
        } else {
          DatabaseDialog.showDatabaseExistsDialog( shell, databaseMeta );
          databaseMeta.setName( originalName );
          databaseMeta.setDisplayName( originalName );
          jsonObject.put( "name", originalName );
        }
        return jsonObject.toJSONString();
      }
    };

    new BrowserFunction( browser, "bfDeleteConnection" ) {
      @Override public Object function( Object[] arguments ) {
        String database = (String) arguments[0];
        controller.removeDatabase( database );
        return true;
      }
    };

    new BrowserFunction( browser, "bfRemove" ) {
      @Override public Object function( Object[] arguments ) {
        String displayName = (String) arguments[0];
        return controller.deleteRepository( displayName );
      }
    };

    new BrowserFunction( browser, "bfBrowse" ) {
      @Override public Object function( Object[] objects ) {
        DirectoryDialog directoryDialog = new DirectoryDialog( shell );
        String location = directoryDialog.open();
        if ( location == null ) {
          location = "";
        }
        browser.evaluate(
          "var location = document.getElementById( 'location' );"
          + "var scope = angular.element( location ).scope( 'file' );"
          + String.format( "scope.$apply(function(){ scope.vm.connection.location = '%s';});", location )
        );
        return "";
      }
    };

    controller.setCurrentRepository( repositoryMeta );
    controller.setRelogin( relogin );

    while ( !dialog.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return result;
  }

  public void openManager() {
    super.createDialog( MANAGER_TITLE, getRepoURL( "" ), OPTIONS, LOGO );
    open();
  }

  public void openCreation() {
    super.createDialog( CREATION_TITLE, getRepoURL( CREATION_WEB_CLIENT_PATH ), OPTIONS, LOGO );
    open();
  }

  public boolean openRelogin( RepositoryMeta repositoryMeta, String errorMessage ) {
    super.createDialog( LOGIN_TITLE, getRepoURL( LOGIN_WEB_CLIENT_PATH ) + "/" + repositoryMeta.getName(), OPTIONS, LOGO );
    return open( repositoryMeta, true, errorMessage );
  }

  public boolean openLogin( RepositoryMeta repositoryMeta ) {
    super.createDialog( LOGIN_TITLE, getRepoURL( LOGIN_WEB_CLIENT_PATH ) + "/" + repositoryMeta.getName(), OPTIONS, LOGO );
    return open( repositoryMeta );
  }

  private void setResult( boolean result ) {
    this.result = result;
  }

  private static Integer getOsgiServicePort() {
    // if no service port is specified try getting it from
    ServerPort osgiServicePort = ServerPortRegistry.getPort( OSGI_SERVICE_PORT );
    if ( osgiServicePort != null ) {
      return osgiServicePort.getAssignedPort();
    }
    return null;
  }

  private static String getClientPath() {
    Properties properties = new Properties();
    try {
      InputStream inputStream =
        RepositoryDialog.class.getClassLoader().getResourceAsStream( "project.properties" );
      properties.load( inputStream );
    } catch ( IOException e ) {
      e.printStackTrace();
    }
    return properties.getProperty( "CLIENT_PATH" );
  }

  private static String getRepoURL( String path ) {
    return System.getProperty( "KETTLE_CONTEXT_PATH", "" ) + "/osgi" + getClientPath() + path;
  }

  private static Object jsonToObject( String json, Class<?> _class ) {
    ObjectMapper mapper = new ObjectMapper();
    Object model = null;
    try {
      model = mapper.readValue( json, _class );
    } catch ( JsonParseException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch ( JsonMappingException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch ( IOException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return model;
  }
}
