import java.awt.*;
import java.awt.event.*;
//import java.applet.Applet;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import javax.comm.*;


public class PdeBase implements ActionListener {
  static Properties properties;
  static Frame frame;
  static String encoding;
  static Image icon;

  boolean errorState;
  PdeEditor editor;

  WindowAdapter windowListener;

  Menu sketchbookMenu;
  File sketchbookFolder;
  String sketchbookPath;

  boolean recordingHistory;
  Menu historyMenu;
  ActionListener historyMenuListener = 
    new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  editor.retrieveHistory(e.getActionCommand());
	}
      };

  Menu serialMenu;
  MenuItem saveMenuItem;
  MenuItem saveAsMenuItem;
  MenuItem beautifyMenuItem;
  CheckboxMenuItem externalEditorItem;

  static final String WINDOW_TITLE = "Proce55ing";

  // the platforms
  static final int WINDOWS = 1;
  static final int MACOS9  = 2;
  static final int MACOSX  = 3;
  static final int LINUX   = 4;
  static int platform;

  static final String platforms[] = {
    "", "windows", "macos9", "macosx", "linux"
  };


  static public void main(String args[]) {
    //System.getProperties().list(System.out);
    //System.out.println(System.getProperty("java.class.path"));

    // should be static though the mac is acting sketchy
    if (System.getProperty("mrj.version") != null) {  // running on a mac
      //System.out.println(UIManager.getSystemLookAndFeelClassName());
      //System.out.println(System.getProperty("mrj.version"));
      //System.out.println(System.getProperty("os.name"));
      platform = (System.getProperty("os.name").equals("Mac OS X")) ?
	MACOSX : MACOS9;
	
    } else {
      //System.out.println("unknown OS");
      //System.out.println(System.getProperty("os.name"));
      String osname = System.getProperty("os.name");
      //System.out.println("osname is " + osname);
      if (osname.indexOf("Windows") != -1) {
	platform = WINDOWS;

      } else if (osname.equals("Linux")) {  // true for the ibm vm
	platform = LINUX;

      } else {
	platform = WINDOWS;  // probably safest
	System.out.println("unhandled osname: " + osname);
      }
    }

    PdeBase app = new PdeBase();
  }

  public PdeBase() {
    frame = new Frame(WINDOW_TITLE) {
	// hack for #@#)$(* macosx
	public Dimension getMinimumSize() {
	  return new Dimension(300, 300);
	}
      };

    try {
      icon = Toolkit.getDefaultToolkit().getImage("lib/icon.gif");
      frame.setIconImage(icon);
    } catch (Exception e) { } // fail silently

    windowListener = new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	//System.exit(0);
	editor.doQuit();
      }
    };
    frame.addWindowListener(windowListener);

    properties = new Properties();
    try {
      //properties.load(new FileInputStream("lib/pde.properties"));
      //#URL where = getClass().getResource("PdeBase.class");
      //System.err.println(where);
      //System.getProperties().list(System.err);
      //System.err.println("userdir = " + System.getProperty("user.dir"));

      if (PdeBase.platform == PdeBase.MACOSX) {
	//String pkg = "Proce55ing.app/Contents/Resources/Java/";
	//properties.load(new FileInputStream(pkg + "pde.properties"));
	//properties.load(new FileInputStream(pkg + "pde.properties_macosx"));
	properties.load(new FileInputStream("lib/pde.properties"));
	properties.load(new FileInputStream("lib/pde_macosx.properties"));

      } else if (PdeBase.platform == PdeBase.MACOS9) {
	properties.load(new FileInputStream("lib/pde.properties"));
	properties.load(new FileInputStream("lib/pde_macos9.properties"));

      } else {  
	// under win95, current dir not set properly
	// so using a relative url like "lib/" won't work
	properties.load(getClass().getResource("pde.properties").openStream());
	String platformProps = "pde_" + platforms[platform] + ".properties";
	properties.load(getClass().getResource(platformProps).openStream());
      }
      //properties.list(System.out);

    } catch (Exception e) {
      System.err.println("Error reading pde.properties");
      e.printStackTrace();
      //System.exit(1);
    }
    int width = getInteger("window.width", 600);
    int height = getInteger("window.height", 350);

    /*
    encoding = get("encoding");
    boolean beautify = false; 
    boolean convertSemicolons = false;
    String program = get("program"); 
    if (program != null) { 
    //program = getFile(program);
    } else {
      program = get("inline_program");
      convertSemicolons = true;
    } 
    if (program != null) {
      // don't beautify if it's java code
      if (program.indexOf("extends PdePlayer") == -1) {
	// don't convert ; to \n if scheme  
	if (program.charAt(0) != ';') {  
	  if (convertSemicolons) {
	    program = program.replace(';', '\n'); 
	  }
	  // not scheme, but don't beautify if it's python 
	  if (program.charAt(0) != '#') 
	    beautify = true; 
	}  
      }
    } 
    */

    editor = new PdeEditor(this);
    frame.setLayout(new BorderLayout());
    frame.add("Center", editor);

    MenuBar menubar = new MenuBar();
    Menu menu;
    MenuItem item;

    menu = new Menu("File");
    menu.add(new MenuItem("New", new MenuShortcut('N')));
    sketchbookMenu = new Menu("Open");
    //rebuildSketchbookMenu(openMenu);
    menu.add(sketchbookMenu);
    saveMenuItem = new MenuItem("Save", new MenuShortcut('S'));
    saveAsMenuItem = new MenuItem("Save as...", new MenuShortcut('S', true));
    menu.add(saveMenuItem);
    menu.add(saveAsMenuItem);
    //menu.add(new MenuItem("Save", new MenuShortcut('S')));
    //menu.add(new MenuItem("Save as...", new MenuShortcut('S', true)));
    //menu.add(new MenuItem("Rename", new MenuShortcut('S', true)));
    //menu.add(new MenuItem("Duplicate", new MenuShortcut('D')));
    menu.add(new MenuItem("Export to Web", new MenuShortcut('E')));
    item = new MenuItem("Export Application", new MenuShortcut('E', true));
    item.setEnabled(false);
    menu.add(item);
    //menu.add(new MenuItem("Export Application", new MenuShortcut('E', true)));
    menu.addSeparator();
    menu.add(new MenuItem("Proce55ing.net", new MenuShortcut('5')));
    menu.add(new MenuItem("Reference", new MenuShortcut('F')));
    menu.addSeparator();
    menu.add(new MenuItem("Quit", new MenuShortcut('Q')));
    menu.addActionListener(this);
    menubar.add(menu);

    // beautify, open, print, play save were key commands

    // completely un-functional edit menu
    /*
    menu = new Menu("Edit");
    menu.add(new MenuItem("Undo"));
    menu.addSeparator();    
    menu.add(new MenuItem("Cut"));
    menu.add(new MenuItem("Copy"));
    menu.add(new MenuItem("Paste"));
    menu.addSeparator();
    menu.add(new MenuItem("Select all"));
    menu.setEnabled(false);
    menubar.add(menu);
    */

    menu = new Menu("Sketch");
    menu.add(new MenuItem("Run", new MenuShortcut('R')));
    menu.add(new MenuItem("Present", new MenuShortcut('P')));
    menu.add(new MenuItem("Stop", new MenuShortcut('T')));
    menu.addSeparator();

    recordingHistory = getBoolean("history.recording", true);
    if (recordingHistory) {
      historyMenu = new Menu("History");
      menu.add(historyMenu);
      item = new MenuItem("Clear History");
      item.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    if (!editor.historyFile.delete()) {
	      System.err.println("couldn't erase history");
	    }
	    rebuildHistoryMenu(historyMenu, editor.historyFile.getPath());
	  }
	});
      menu.add(item);
      menu.addSeparator();
    }

    beautifyMenuItem = new MenuItem("Beautify", new MenuShortcut('B'));
    //item.setEnabled(false);
    menu.add(beautifyMenuItem);

    //menu.addSeparator();
    serialMenu = new Menu("Serial Port");
    menu.add(serialMenu);
    buildSerialMenu();

    externalEditorItem = new CheckboxMenuItem("Use External Editor");
    externalEditorItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
	//System.out.println(e);
	if (e.getStateChange() == ItemEvent.SELECTED) {
	  editor.setExternalEditor(true);
	} else {
	  editor.setExternalEditor(false);
	}
      }
    });
    menu.add(externalEditorItem);

    menu.addActionListener(this);
    menubar.add(menu);  // add the sketch menu

    frame.setMenuBar(menubar);

    Insets insets = frame.getInsets();
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension screen = tk.getScreenSize();

    // THESE CAN BE REMOVED TO SOME EXTENT
    int frameX = getInteger("window.x", (screen.width - width) / 2);
    int frameY = getInteger("window.y", (screen.height - height) / 2);

    frame.setBounds(frameX, frameY, 
		    width + insets.left + insets.right, 
		    height + insets.top + insets.bottom);
    //frame.reshape(50, 50, width + insets.left + insets.right, 
    //	  height + insets.top + insets.bottom);

    // i don't like this being here, but..
    //((PdeEditor)environment).graphics.frame = frame;
    //((PdeEditor)environment).frame = frame
    frame.pack();  // maybe this should be before the setBounds call

    //System.out.println(frame.getMinimumSize() + " " + frame.getSize());

    editor.frame = frame;  // no longer really used
    editor.init();
    rebuildSketchbookMenu(sketchbookMenu);
    frame.show();  // added back in for pde
  }


  // listener for sketchbk items uses getParent() to figure out
  // the directories above it

  class SketchbookMenuListener implements ActionListener {
    String path;

    public SketchbookMenuListener(String path) {
      this.path = path;
    }

    public void actionPerformed(ActionEvent e) {
      String name = e.getActionCommand();
      editor.skOpen(path + File.separator + name, name);
    }
  }

  public void rebuildSketchbookMenu() {
    rebuildSketchbookMenu(sketchbookMenu);
  }

  public void rebuildSketchbookMenu(Menu menu) {
    menu.removeAll();

    try {
      //MenuItem newSketchItem = new MenuItem("New Sketch");
      //newSketchItem.addActionListener(this);
      //menu.add(newSkechItem);
      //menu.addSeparator();

      sketchbookFolder = new File("sketchbook");
      sketchbookPath = sketchbookFolder.getCanonicalPath();
      if (!sketchbookFolder.exists()) {
	System.err.println("sketchbook folder doesn't exist, " + 
			   "making a new one");
	sketchbookFolder.mkdirs();
      }


      // files for the current user (for now, most likely 'default')

      // header knows what the current user is
      String userPath = sketchbookPath + 
	File.separator + editor.userName;

      File userFolder = new File(userPath);
      if (!userFolder.exists()) {
	System.err.println("sketchbook folder for '" + editor.userName + 
			   "' doesn't exist, creating a new one");
	userFolder.mkdirs();
      }

      SketchbookMenuListener userMenuListener = 
	new SketchbookMenuListener(userPath);

      String entries[] = new File(userPath).list();
      boolean added = false;
      for (int j = 0; j < entries.length; j++) {
	if (entries[j].equals(".") || 
	    entries[j].equals("..") ||
	    entries[j].equals("CVS") ||
	    entries[j].equals(".cvsignore")) continue;
	added = true;
	if (new File(userPath, entries[j] + File.separator + 
		     entries[j] + ".pde").exists()) {
	  MenuItem item = new MenuItem(entries[j]);
	  item.addActionListener(userMenuListener);
	  menu.add(item);
	}
	//submenu.add(entries[j]);
      }
      if (!added) {
	MenuItem item = new MenuItem("No sketches");
	item.setEnabled(false);
	menu.add(item);
      }
      menu.addSeparator();

      // other available subdirectories

      String toplevel[] = sketchbookFolder.list();
      added = false;
      for (int i = 0; i < toplevel.length; i++) {
	if (toplevel[i].equals(editor.userName) ||
	    toplevel[i].equals(".") ||
	    toplevel[i].equals("..") ||
	    toplevel[i].equals("CVS") ||
	    toplevel[i].equals(".cvsignore")) continue;

	added = true;
	Menu subMenu = new Menu(toplevel[i]);
	File subFolder = new File(sketchbookFolder, toplevel[i]);
	String subPath = subFolder.getCanonicalPath();
	SketchbookMenuListener subMenuListener = 
	  new SketchbookMenuListener(subPath);

	entries = subFolder.list();
	if (entries != null) {
	  for (int j = 0; j < entries.length; j++) {
	    if (entries[j].equals(".") || 
		entries[j].equals("..") ||
		entries[j].equals("CVS") ||
		entries[j].equals(".cvsignore")) continue;
	    //subMenu.add(entries[j]);
	    if (new File(subFolder, entries[j] + File.separator + 
			 entries[j] + ".pde").exists()) {
	      MenuItem item = new MenuItem(entries[j]);
	      item.addActionListener(subMenuListener);
	      subMenu.add(item);
	    }
	  }
	}

	menu.add(subMenu);
      }
      if (added) menu.addSeparator();

      MenuItem item = new MenuItem("Refresh");
      item.addActionListener(this);
      menu.add(item);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /*
  class HistoryMenuListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      editor.selectHistory(e.getActionCommand);
    }
  }
  */

  public void rebuildHistoryMenu(String path) {
    rebuildHistoryMenu(historyMenu, path);
  }

  public void rebuildHistoryMenu(Menu menu, String path) {
    if (!recordingHistory) return;

    menu.removeAll();
    File hfile = new File(path);
    if (!hfile.exists()) return;

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))));
      String line = null;

      int historyCount = 0;
      String historyList[] = new String[100];

      try {
	while ((line = reader.readLine()) != null) {
	//while (line = reader.readLine()) {
	//while (true) { line = reader.readLine();
	  //if (line == null) continue;
	  //System.out.println("line: " + line);
	  if (line.equals(PdeEditor.HISTORY_SEPARATOR)) {
	    // next line is the good stuff
	    line = reader.readLine();
	    int version = 
	      Integer.parseInt(line.substring(0, line.indexOf(' ')));
	    if (version == 1) {
	      String whysub = line.substring(2);  // after "1 "
	      String why = whysub.substring(0, whysub.indexOf(" -"));
	      //System.out.println("'" + why + "'");

	      String readable = line.substring(line.lastIndexOf("-") + 2);
	      if (historyList.length == historyCount) {
		String temp[] = new String[historyCount*2];
		System.arraycopy(historyList, 0, temp, 0, historyCount);
		historyList = temp;
	      }
	      historyList[historyCount++] = why + " - " + readable;

	    } // otherwise don't know what to do
	  }
	}
	//System.out.println(line);
      } catch (IOException e) {
	e.printStackTrace();
      }

      // add the items to the menu in reverse order
      /*
      ActionListener historyMenuListener = 
	new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      editor.retrieveHistory(e.getActionCommand());
	    }
	  };
      */

      for (int i = historyCount-1; i >= 0; --i) {
	MenuItem mi = new MenuItem(historyList[i]);
	mi.addActionListener(historyMenuListener);
	menu.add(mi);
      }

      reader.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  class SerialMenuListener implements ItemListener /*, ActionListener*/ {
    //public SerialMenuListener() { }

    public void itemStateChanged(ItemEvent e) {
      int count = serialMenu.getItemCount();
      for (int i = 0; i < count; i++) {
	((CheckboxMenuItem)serialMenu.getItem(i)).setState(false);
      }
      CheckboxMenuItem item = (CheckboxMenuItem)e.getSource();
      item.setState(true);
      String name = item.getLabel();
      //System.out.println(item.getLabel());
      PdeBase.properties.put("serial.port", name);
      //System.out.println("set to " + get("serial.port"));
    }
    
    /*
    public void actionPerformed(ActionEvent e) {
      System.out.println(e.getSource());
      String name = e.getActionCommand();
      PdeBase.properties.put("serial.port", name);
      System.out.println("set to " + get("serial.port"));
      //editor.skOpen(path + File.separator + name, name);
      // need to push "serial.port" into PdeBase.properties
    }
    */
  }

  protected void buildSerialMenu() {
    // get list of names for serial ports
    // have the default port checked (if present)

    SerialMenuListener listener = new SerialMenuListener();
    String defaultName = get("serial.port", "unspecified");
    //boolean found;

    try {
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
	CommPortIdentifier portId = 
	  (CommPortIdentifier) portList.nextElement();

	if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
	  //if (portId.getName().equals(port)) {
	  String name = portId.getName();
	  CheckboxMenuItem mi = 
	    new CheckboxMenuItem(name, name.equals(defaultName));
	  //mi.addActionListener(listener);
	  mi.addItemListener(listener);
	  serialMenu.add(mi);
	}
      }
    } catch (Exception e) {
      System.out.println("exception building serial menu");
      e.printStackTrace();
    }
  }


  public void actionPerformed(ActionEvent event) {
    String command = event.getActionCommand();
    //System.out.println(command);

    if (command.equals("New")) {
      editor.skNew();
      //editor.initiate(Editor.NEW);

    } else if (command.equals("Save")) {
      editor.doSave();

    } else if (command.equals("Save as...")) {
      editor.skSaveAs();

      /*
    } else if (command.equals("Rename")) {
      editor.skDuplicateRename(true);

    } else if (command.equals("Duplicate")) {
      editor.skDuplicateRename(false);
      */

    } else if (command.equals("Export to Web")) {
      editor.skExport();

    } else if (command.equals("Proce55ing.net")) {
      if (platform == WINDOWS) {
	try {
	  Runtime.getRuntime().exec("c:\\progra~1\\intern~1\\iexplore http://Proce55ing.net");
	  //Runtime.getRuntime().exec("start http://Proce55ing.net");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else if ((platform == MACOS9) || (platform == MACOSX)) {
#ifdef MACOS
	try {
	  com.apple.mrj.MRJFileUtils.openURL("http://Proce55ing.net");
	} catch (IOException e) {
	  e.printStackTrace();
	}
#endif

      } else if (platform == LINUX) {
	try {
	  // wild ass guess
	  Runtime.getRuntime().exec("mozilla http://Proce55ing.net");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else {
	System.err.println("unspecified platform");
      }

    } else if (command.equals("Reference")) {
      if (platform == WINDOWS) {
	try {
	  Runtime.getRuntime().exec("cmd /c reference\\index.html");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else if ((platform == MACOSX) || (platform == MACOS9)) {
#ifdef MACOS
	try {
	  com.apple.mrj.MRJFileUtils.openURL("reference/index.html");
	} catch (IOException e) {
	  e.printStackTrace();
	}
#endif

      } else if (platform == LINUX) {
	try {
	  // another wild ass guess
	  Runtime.getRuntime().exec("mozilla reference/index.html");
	} catch (IOException e) {
	  e.printStackTrace();
	}

      } else {
	System.err.println("unspecified platform");
      }

    } else if (command.equals("Quit")) {
      editor.doQuit();
      //editor.initiate(Editor.QUIT);

    } else if (command.equals("Run")) {
      editor.doRun(false);

    } else if (command.equals("Present")) {
      editor.doRun(true);
      //editor.doPresent();

    } else if (command.equals("Stop")) {    
      if (editor.presenting) {
	editor.doClose();
      } else {
	editor.doStop();
      }

    } else if (command.equals("Refresh")) {    
      //System.err.println("got refresh");
      rebuildSketchbookMenu(sketchbookMenu);      

    } else if (command.equals("Beautify")) {
      editor.doBeautify();

      //} else if (command.equals("Use External Editor")) {
      //boolean external = externalEditorItem.getState();
      //external = !external;
      //editor.setExternalEditor(external);

      // disable save, save as menus
      
    }
    //if (command.equals("Save QuickTime movie...")) {
    //  ((PdeEditor)environment).doRecord();
    //} else if (command.equals("Quit")) {
    //  System.exit(0);
    //}
  }


  // does this do anything useful?
  /*
  public void destroy() {
    if (editor != null) {
      editor.terminate();
    }
  }
  */

  /*
  public void paint(Graphics g) {
    if (errorState) {
      g.setColor(Color.red);
      Dimension d = size();
      g.fillRect(0, 0, d.width, d.height);
    }
  }
  */

  // all the information from pde.properties

  static public String get(String attribute) {
    return get(attribute, null);
  }

  static public String get(String attribute, String defaultValue) {
    //String value = (properties != null) ?
    //properties.getProperty(attribute) : applet.getParameter(attribute);
    String value = properties.getProperty(attribute);

    return (value == null) ? 
      defaultValue : value;
  }

  static public boolean getBoolean(String attribute, boolean defaultValue) {
    String value = get(attribute, null);
    return (value == null) ? defaultValue : 
      (new Boolean(value)).booleanValue();

    /*
      supposedly not needed, because anything besides 'true'
      (ignoring case) will just be false.. so if malformed -> false
    if (value == null) return defaultValue;

    try {
      return (new Boolean(value)).booleanValue();
    } catch (NumberFormatException e) {
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    */
  }

  static public int getInteger(String attribute, int defaultValue) {
    String value = get(attribute, null);
    if (value == null) return defaultValue;

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) { 
      // ignored will just fall through to returning the default
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    //if (value == null) return defaultValue;
    //return (value == null) ? defaultValue : 
    //Integer.parseInt(value);
  }

  static public Color getColor(String name, Color otherwise) {
    Color parsed = null;
    String s = get(name, null);
    if ((s != null) && (s.indexOf("#") == 0)) {
      try {
	int v = Integer.parseInt(s.substring(1), 16);
	parsed = new Color(v);
      } catch (Exception e) {
      }
    }
    if (parsed == null) return otherwise;
    return parsed;
  }

  static public Font getFont(String which, Font otherwise) {
    //System.out.println("getting font '" + which + "'");
    String str = get(which);
    if (str == null) return otherwise;  // ENABLE LATER
    StringTokenizer st = new StringTokenizer(str, ",");
    return new Font(st.nextToken(), 
		    st.nextToken().equals("bold") ? Font.BOLD : Font.PLAIN,
		    Integer.parseInt(st.nextToken()));
  }

  static public SimpleAttributeSet getStyle(String what, 
					    SimpleAttributeSet otherwise) {
    String str = get("editor.program." + which + ".style");
    if (str == null) return otherwise;  // ENABLE LATER
    StringTokenizer st = new StringTokenizer(str, ",");

    SimpleAttributeSet style = new SimpleAttributeSet();

    StyleConstants.setFontFamily(style, st.nextToken());

    String s = st.nextToken();
    StyleConstants.setBold(style, s.indexOf("bold") != -1);
    StyleConstants.setItalic(style, s.indexOf("italic") != -1);

    StyleConstants.setSize(style, Integer.parseInt(st.nextToken()));

    s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1);
    StyleConstants.setForeground(style, new Color(Integer.parseInt(s, 16)));

    s = st.nextToken();
    if (s.indexOf("#") == 0) s = s.substring(1);
    StyleConstants.setBackground(style, new Color(Integer.parseInt(s, 16)));

    return style;
  }


  // used by PdeEditorButtons, but probably more later
  static public Image getImage(String name, Component who) {
    Image image = null;
    //if (isApplet()) {
    //image = applet.getImage(applet.getCodeBase(), name);
    //} else {
    Toolkit tk = Toolkit.getDefaultToolkit();

    if (PdeBase.platform == PdeBase.MACOSX) {
      //String pkg = "Proce55ing.app/Contents/Resources/Java/";
      //image = tk.getImage(pkg + name);
      image = tk.getImage("lib/" + name);
    } else if (PdeBase.platform == PdeBase.MACOS9) {
      image = tk.getImage("lib/" + name);
    } else {
      image = tk.getImage(who.getClass().getResource(name));
    }

    //image =  tk.getImage("lib/" + name);
    //URL url = PdeApplet.class.getResource(name);
    //image = tk.getImage(url);
    //}
    //MediaTracker tracker = new MediaTracker(applet);
    MediaTracker tracker = new MediaTracker(who); //frame);
    tracker.addImage(image, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) { }      
    return image;
  }


  // this could be pruned further
  // also a similar version inside PdeEditor 
  // (at least the binary portion)
  static public String getFile(String filename) {
    if (filename.length() == 0) {
      return null;
    }
    URL url;
    InputStream stream = null;
    String openMe;
    byte temp[] = new byte[65536];  // 64k, 16k was too small

    try {
      // if running as an application, get file from disk
      stream = new FileInputStream(filename);

    } catch (Exception e1) { try {
      url = frame.getClass().getResource(filename);
      stream = url.openStream();

    } catch (Exception e2) { try {
      // Try to open the param string as a URL
      url = new URL(filename);
      stream = url.openStream();
	
    } catch (Exception e3) {
      return null;
    } } }

    try {
      int offset = 0;
      while (true) {
	int byteCount = stream.read(temp, offset, 1024);
	if (byteCount <= 0) break;
	offset += byteCount;
      }
      byte program[] = new byte[offset];
      System.arraycopy(temp, 0, program, 0, offset);

      //return languageEncode(program);
      // convert the bytes based on the current encoding
      try {
	if (encoding == null)
	  return new String(program);
	return new String(program, encoding);
      } catch (UnsupportedEncodingException e) {
	e.printStackTrace();
	encoding = null;
	return new String(program);
      }

    } catch (Exception e) {
      System.err.println("problem during download");
      e.printStackTrace();
      return null;
    }
  }

  /*
  static public boolean hasFullPrivileges() {
    //if (applet == null) return true;  // application
    //return false;
    return !isApplet();
  }
  */
}

