package org.pentaho.di.ui.spoon;

import org.eclipse.rap.json.JsonObject;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.ClientFileLoader;
import org.eclipse.rap.rwt.remote.AbstractOperationHandler;
import org.eclipse.rap.rwt.remote.Connection;
import org.eclipse.rap.rwt.remote.RemoteObject;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.rap.rwt.widgets.WidgetUtil;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.pentaho.di.ui.core.gui.GUIResource;

public class WebSpoonCTabFolder extends CTabFolder {

  private RemoteObject remoteObject;

  public WebSpoonCTabFolder( Composite parent, int style ) {
    super( parent, style );
    ResourceManager resourceManager = RWT.getResourceManager();
    ClientFileLoader clientFileLoader = RWT.getClient().getService( ClientFileLoader.class );
    clientFileLoader.requireJs( resourceManager.getLocation( "js/clipboard.js" ) );
    clientFileLoader.requireJs( resourceManager.getLocation( "js/jquery.min.js" ) );
    clientFileLoader.requireJs( resourceManager.getLocation( "js/notify.js" ) );

    Connection connection = RWT.getUISession().getConnection();
    remoteObject = connection.createRemoteObject( "webSpoon.Clipboard" );
    remoteObject.set( "parent", WidgetUtil.getId( this ) );
    remoteObject.set( "self", remoteObject.getId() );
    remoteObject.setHandler( new AbstractOperationHandler() {
      @Override
      public void handleNotify(String event, JsonObject properties) {
        if ( event.equals( "paste" ) ) {
          GUIResource.getInstance().toClipboard( properties.get( "text" ).asString() );
          Spoon.getInstance().paste();
        } else if ( event.equals( "cut" ) ) {
          Spoon.getInstance().cut();
        }
      }
    } );
    remoteObject.listen( "paste", true );
    remoteObject.listen( "copy", true );
    remoteObject.listen( "cut", true );
  }

  public void dispose() {
    remoteObject.destroy();
  }

  public void toClipboard( String text ) {
    remoteObject.set( "text", text );
  }

  public void downloadCanvasImage( String rwtId, String name ) {
    JsonObject obj = new JsonObject();
    obj.add( "rwtId", rwtId );
    obj.add( "name", name );
    remoteObject.call( "downloadCanvasImage", obj );
  }
}
