/*
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 * **************************************************************************
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
 */

package org.pentaho.di.ui.core.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class ShowHelpDialog extends Dialog {

  private String dialogTitle;
  private String url;
  private String header;

  private Browser wBrowser;
  private FormData fdBrowser;

  private Shell shell;
  private PropsUI props;

  private int headerHeight = 55;
  private int headerLabelPosition = 10;

  private LocationListener locationListener;

  public ShowHelpDialog( Shell parent, String dialogTitle, String url, String header ) {
    super( parent, SWT.NONE );
    props = PropsUI.getInstance();
    this.dialogTitle = dialogTitle;
    this.header = header;
    this.url = url;
  }

  public ShowHelpDialog( Shell parent, String dialogTitle, String url, LocationListener locationListener ) {
    this( parent, dialogTitle, url, "" );
    this.locationListener = locationListener;
    headerHeight = 0;
  }

  protected Shell createShell( Shell parent ) {
    return new Shell( parent, SWT.RESIZE | SWT.MAX | SWT.MIN | SWT.DIALOG_TRIM );
  }

  public void open() {
    Program.launch( url );
  }

  public void dispose() {
    shell.dispose();
  }

  private void ok() {
    dispose();
  }

}
