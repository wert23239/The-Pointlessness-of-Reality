package tsc;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.undo.*;

@SuppressWarnings("unused")
public class TscApp  implements ActionListener {
	static TscApp appListen = new TscApp();
	public static JFrame frame = new JFrame("Noxid's Script Editor");
	//public static JFrame frame = new CloseFrameAction("Noxid's Script Editor");
	public static JPanel mainPanel = new JPanel();
	
	//kitten area
	public static JTextField findField = new JTextField(13);
	public static JButton findButton = new JButton("Go!");
	
	//working area
	public static JTabbedPane tabPanel = new JTabbedPane();
	public static JTextPane definePane = new JTextPane();
	
	//info area
	public static JList commandList;
	public static JTextArea comLabel = new JTextArea("Command:", 2, 18);
	public static JTextArea descLabel = new JTextArea("Description:\n\n\n", 4, 18);
	
	//menu bar
	public static JMenuItem menuArray[]; //the file menu
	public static JMenuItem paste;
	
	//datas?
	public static Vector<TscCommand> commandInf;
	public static Hashtable<String,SimpleAttributeSet> styles = new Hashtable<String, SimpleAttributeSet>(); //maps styles for text formatting
	public static Vector<String> def1 = new Vector<String>(); // define keywords
	public static Vector<String> def2 = new Vector<String>(); // define replacements
	
	private static File lastDir = null;
	private static File defFile = new File("tsc_def.txt");
	private static ArrayList<File> fList = new ArrayList<File>(); //holds the list of files where the scripts were found
	
	//other windos
	public static ScriptSelector selectWindow = null;
	
	/*utilities
	protected static UndoManager undo = new UndoManager(); //allow undo's and redo's
	*/
	public static void main(String[] args)
	{		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		java.net.URL iconURL = TscApp.class.getResource("icon.png");
		Image icon = Toolkit.getDefaultToolkit().createImage(iconURL);
		frame.setIconImage(icon);
		addComponentsToPane(frame.getContentPane());
		initStyles();
		frame.pack();
		frame.setMinimumSize(frame.getSize());
		frame.setVisible(true);
	}
	
	public static void addComponentsToPane(Container pane)
	{
		//make it look like a real application (Carrotlord)
		try {			
		UIManager.setLookAndFeel(
		UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException exceptionInfo) {
		// Ignore this exception.
		} catch (ClassNotFoundException exceptionInfo) {
		// Ignore this exception.
		} catch (InstantiationException exceptionInfo) {
		// Ignore this exception.
		} catch (IllegalAccessException exceptionInfo) {
		// Ignore this exception.
		}
		
		//set the initial directory to the executable's current directory
		lastDir = new File( System.getProperty("user.dir"));
		
		//setup the menu bar
		JMenuBar menuBar;
		JMenu fileMenu, miscMenu;
		
		menuArray = new JMenuItem[7];
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		menuArray[0] = new JMenuItem("Open...");
		menuArray[0].setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.Event.CTRL_MASK));
		menuArray[1] = new JMenuItem("Save...");
		menuArray[1].setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK));
		menuArray[2] = new JMenuItem("Save as...");
		menuArray[2].setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK | java.awt.Event.SHIFT_MASK));
		menuArray[3] = new JMenuItem("Close");
		menuArray[3].setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.Event.CTRL_MASK));
		menuArray[4] = new JMenuItem("Load Stage List");
		for (int i = 0; i < 5; i++)
		{
			fileMenu.add(menuArray[i]);
			menuArray[i].addActionListener(appListen);
		}
		menuBar.add(fileMenu);
		fileMenu = new JMenu("Operation");
		menuArray[5] = new JMenuItem("Generate flag listing");
		menuArray[6] = new JMenuItem("Generate TRA listing");
		for (int i = 0; i < 2; i++)
		{
			fileMenu.add(menuArray[5+i]);
			menuArray[5+i].addActionListener(appListen);
		}
		menuBar.add(fileMenu);
		
		frame.setJMenuBar(menuBar);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		pane.add(mainPanel);
		
		//set up the panels
		JPanel leftPanel = new JPanel();
		JPanel rightPanel = new JPanel();
		
		//setup left side
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.setPreferredSize(new Dimension(400, 400));
		
		//setup right side
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		//rightPanel.setPreferredSize(new Dimension(140, 400));
		rightPanel.setMaximumSize(new Dimension(200, 9001));
		//rightPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		
		mainPanel.add(leftPanel);
		mainPanel.add(rightPanel);
		
		//setup left side even more
		JPanel luPanel = new JPanel();
		luPanel.setPreferredSize(new Dimension(400, 80));
		luPanel.setMaximumSize(new Dimension(8000, 80));
		luPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		//add to left up panel
		//add kitten
		JLabel kittenLabel;
		java.net.URL kittenURL = TscApp.class.getResource("cat.gif");
		ImageIcon kitten = new ImageIcon(kittenURL, "pic");
		if (kitten != null)
		{
			kittenLabel = new JLabel(kitten);
			luPanel.add(kittenLabel);
		}
		//add label and text field
		JLabel findLabel = new JLabel("Find:");
		luPanel.add(findLabel);
		luPanel.add(findField);
		findButton.addActionListener(appListen);
		luPanel.add(findButton);
		leftPanel.add(luPanel);
		
		//setup the right side even more
		
		try {
			commandList = new JList(getCommands());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		commandList.addListSelectionListener(new ListMan());
		//commandList.setPreferredSize(new Dimension(100, 380));
		commandList.setFont(new Font(Font.MONOSPACED, 0, 11));
		JScrollPane listScroll = new JScrollPane(commandList);
		comLabel.setLineWrap(true);
		comLabel.setWrapStyleWord(true);
		comLabel.setEditable(false);
		comLabel.setMaximumSize(new Dimension(200, 32));
		comLabel.setMinimumSize(new Dimension(200, 32));
		descLabel.setWrapStyleWord(true);
		descLabel.setLineWrap(true);
		descLabel.setEditable(false);
		descLabel.setMaximumSize(new Dimension(200, 64));
		descLabel.setMinimumSize(new Dimension(200, 64));
		rightPanel.add(comLabel);
		rightPanel.add(descLabel);
		rightPanel.add(listScroll);
		
		//setup the tab thingy
		//tabPanel.addTab("Definitions", definePane);
		tabPanel.setPreferredSize(new Dimension(400, 320));
		//add listener
		//definePane.addKeyListener(new java.awt.event.KeyAdapter() {
		//	public void keyReleased(java.awt.event.KeyEvent evt) {
		//		textBoxKeyReleased(evt);
		//	}
		//});
		//Remove paste action. Put inside your GUI's constructor:
		tabPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
		                          KeyEvent.CTRL_MASK),"none");

		//Class field declaration follows. Put in main body of GUI class.
		paste = new javax.swing.JMenuItem();

		//Create new shortcut Ctrl+V for custom paste option. Put inside your GUI's constructor:
		paste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
		    paste.setText("Paste");
		    paste.addActionListener(new java.awt.event.ActionListener() {
		        public void actionPerformed(java.awt.event.ActionEvent evt) {
		            pasteActionPerformed(evt);
		        }
		    });
		//editMenu.add(paste);
		leftPanel.add(tabPanel);
		
		//load the definition list
		try {
			String defStr = System.getProperty("user.dir") + "\\tsc_def.txt";
			fList.add(defFile);
			BufferedReader defReader = new BufferedReader(new FileReader(defFile));
			String nextLine;
			nextLine = defReader.readLine();
			String fullText = "";
			while (nextLine != null)
			{
				fullText += nextLine + "\n";
				nextLine = defReader.readLine();
			}
			definePane.setText(fullText);
			
			defReader.close();
		} catch (FileNotFoundException e) { //if there is no definitions file use the default template
			definePane.setText("//Defines in the form ''first=second'',\n//where second replaces first in the saved file.\n//One definition per line, slash indicates comment line.\n");
			//definePane.setText("If this worked, it'd be pretty cool.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//definePane.getDocument().addUndoableEditListener(undo);
		//JPanel defPan = new JPanel(new BorderLayout());
		JScrollPane defScroll = new JScrollPane(definePane);
		//defPan.add(definePane, BorderLayout.CENTER);
		defScroll.setName("Defines");
		tabPanel.add(defScroll);
	}
	
	private static JTextPane getActiveTextPane()
	{
		int index = tabPanel.getSelectedIndex();
		Component c = tabPanel.getComponentAt(tabPanel.getSelectedIndex());
		JViewport sp = (JViewport) c.getComponentAt(c.getX() + 8, c.getY() + 8);
		//JPanel panel = (JPanel) sp.getComponent(0);
		//JTextPane r = (JTextPane) panel.getComponent(0);
		JTextPane r = (JTextPane) sp.getComponent(0);
		String name = r.toString();
		return (JTextPane) r;
	}

	static void initStyles()
	{
		SimpleAttributeSet newStyle;
		
		//event numbers
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, "Monospaced");
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.black);
		StyleConstants.setBold(newStyle, true);
		StyleConstants.setItalic(newStyle, false);
		styles.put("eveNum", newStyle);
		//tsc tags
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, "Monospaced");
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.blue);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		styles.put("tag", newStyle);
		//numbers
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, "Monospaced");
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.decode("0xC42F63"));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		styles.put("number", newStyle);
		//number spacer
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, "Monospaced");
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.GRAY);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		styles.put("spacer", newStyle);
		//text
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, "Monospaced");
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.black);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		styles.put("text", newStyle);
		//overlimit text
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, "Monospaced");
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.gray);
		StyleConstants.setForeground(newStyle, Color.red);
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, false);
		styles.put("overflow", newStyle);
		//inaccessible commands
		newStyle = new SimpleAttributeSet();
		StyleConstants.setFontFamily(newStyle, "Monospaced");
		StyleConstants.setFontSize(newStyle, 12);
		StyleConstants.setBackground(newStyle, Color.white);
		StyleConstants.setForeground(newStyle, Color.decode("0x367A2A"));
		StyleConstants.setBold(newStyle, false);
		StyleConstants.setItalic(newStyle, true);
		styles.put("comment", newStyle);
	}
	
	static Vector<String> getCommands() throws IOException
	{
		BufferedReader commandFile = null;
		StreamTokenizer tokenizer = null;
		Vector<String> result = new Vector<String>();
		try {
			commandFile = new BufferedReader(new FileReader("tsc_list.txt"));
			tokenizer = new StreamTokenizer(commandFile);
		} catch (FileNotFoundException e) {
			result.add("Missing tsc_list");
			return result;
		}
		
		//search for the correct header
		tokenizer.wordChars(0x20, 0x7E);
		
		while ((tokenizer.sval == null) || (tokenizer.sval.equals("[CE_TSC]") == false)) 
			tokenizer.nextToken();
		//read how many commands
		tokenizer.nextToken();
		int numCommand = (int) tokenizer.nval;
		
		tokenizer.resetSyntax();
		tokenizer.whitespaceChars(0, 0x20);
		tokenizer.wordChars(0x20, 0x7E);
		
		commandInf = new Vector<TscCommand>();
		for (int i = 0; i < numCommand; i++)
		{
			TscCommand newCommand = new TscCommand();
			//read command code
			tokenizer.nextToken();
			newCommand.commandCode = tokenizer.sval;
			//read how many parameters it has
			tokenizer.parseNumbers();
			tokenizer.nextToken();
			newCommand.numParam = (int)tokenizer.nval;
			tokenizer.resetSyntax();
			tokenizer.whitespaceChars(0, 0x20);
			tokenizer.wordChars(0x20, 0x7E);
			//read the CE parameters
			newCommand.CE_param = new char[4];
			tokenizer.nextToken();
			tokenizer.sval.getChars(0, 4, newCommand.CE_param, 0);
			//read short name
			tokenizer.nextToken();
			newCommand.name = tokenizer.sval;
			//read description
			tokenizer.nextToken();
			newCommand.description = tokenizer.sval;
			
			commandInf.add(newCommand);
			result.add(newCommand.commandCode + " - " + newCommand.name);
		}
		commandFile.close();
		return result;
	}
	
	void populateDefines()
	{
		if (definePane != null)
		{
			def1.clear();
			def2.clear();
			Scanner sc = new Scanner(definePane.getText());
			while (sc.hasNextLine())
			{
				String next = sc.nextLine();
				if (next.isEmpty())
					continue;
				if (next.charAt(0) == '/') //if this is a comment line
					continue;
				int pos = 0;
				String firstWord = "", secondWord = "";
				char nextChar = next.charAt(pos);
				while (nextChar != '=' && pos < next.length())
				{
					firstWord += nextChar;
					pos++;
					nextChar = next.charAt(pos);
				}
				if (firstWord.isEmpty() || pos >= next.length())
					continue;
				
				secondWord = next.substring(++pos);
				def1.add(firstWord);
				def2.add(secondWord);
			}
		}
	}
	
	String replaceDefines(String text)
	{
		for (int i = 0; i < def1.size(); i++) //for each word in the define list
		{
			text = text.replace(def1.elementAt(i), def2.elementAt(i));
		}
		return text;
	}
	
	public void listAction(String filename, File executable)
	{
		String subDir;
		if (!executable.getName().endsWith(".exe"))
		{
			//bin mapdata, one layer deep
			subDir = "\\Stage\\";
		} else { //exe mapdata, two layers deep
			subDir = "\\data\\Stage\\";
		}
		String sourcePath = executable.getParent() + subDir + "ScriptSource\\" + filename + ".txt";
		File sourceFile = new File(sourcePath);
		if (sourceFile.exists())
		{
			addSourceTab(sourceFile);
			return;
		}
		//failed to find source file, try alt location
		sourcePath = executable.getParent() + subDir + filename + ".txt";
		sourceFile = new File(sourcePath);
		if (sourceFile.exists())
		{
			addSourceTab(sourceFile);
		} else { //source can't be found or doesn't exist
			sourceFile = new File(sourcePath.replace(".txt", ".tsc"));
			addScriptTab(sourceFile);
		}
		
	}
	
	void addScriptTab(File scriptFile)
	{
		
		//set the contents of the pane
		JTextPane newTP = new JTextPane();
		newTP.setText(parseScript(scriptFile));
		try {
			highlightDoc(newTP.getStyledDocument(), 0, -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//add listener
		newTP.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent evt) {
				textBoxKeyReleased(evt);
			}
		});
		//allow it to scroll
		//JPanel thePanel = new JPanel();
		//thePanel.setLayout(new BorderLayout());
		//thePanel.add(newTP);
		JScrollPane scrollable = new JScrollPane(newTP);
		scrollable.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		newTP.setPreferredSize(new Dimension(1000, 400));
		tabPanel.addTab(scriptFile.getName(), scrollable);
		
		//add file to list
		fList.add(scriptFile);
	}

	public static String parseScript(File scriptFile)
	{
		FileChannel inChan;
		FileChannel outChan;
		ByteBuffer dataBuf = null;
		int fileSize = 0;
		
		try {
			FileInputStream inFile = new FileInputStream(scriptFile);
			inChan = inFile.getChannel();
			fileSize = (int) inChan.size();
			dataBuf = ByteBuffer.allocate(fileSize);
			inChan.read(dataBuf);
			inChan.close();
			inFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] datArray = null;
		if (fileSize > 0)
		{
			int cypher = dataBuf.get(fileSize/2);
			datArray = dataBuf.array();
			for (int i = 0; i < fileSize; i++)
			{
				if (i != fileSize/2)
					datArray[i] -= cypher;
			}
		}
		
		//now read the input as a text
		String finalText = "empty";
		if (datArray != null)
			finalText = new String(datArray);
		return finalText;
	}
	
	void addSourceTab(File sourceFile)
	{
		Scanner sc = null;
		InputStreamReader rd;
		try {
			rd = new InputStreamReader(new FileInputStream(sourceFile));
			//sc = new Scanner(new FileInputStream(sourceFile));
			sc = new Scanner(rd);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String finalText = "";
		JTextPane newTP = new JTextPane();
		
		while(sc.hasNextLine())
		{
			finalText += sc.nextLine() + "\r\n";
		}
		newTP.setText(finalText);
		try {
			highlightDoc(newTP.getStyledDocument(), 0, -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//add listener
		newTP.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent evt) {
				textBoxKeyReleased(evt);
			}
		});
		//allow it to scroll
		//JPanel thePanel = new JPanel();
		//thePanel.setLayout(new BorderLayout());
		//thePanel.add(newTP);
		JScrollPane scrollable = new JScrollPane(newTP);
		scrollable.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		newTP.setPreferredSize(new Dimension(1000, 400));
		tabPanel.addTab(sourceFile.getName(), scrollable);
		
		//add file to list
		fList.add(sourceFile);
		sc.close();
	}
        
        private String removeComments(String text) {
            int commentStart = text.indexOf("//");
            text = StrTools.slice(text, 0,
                   (commentStart == -1) ? text.length() : commentStart);
            return text;
        }
	
 	void saveScript(File scriptFile) throws IOException
	{
		JTextPane pane = getActiveTextPane();
		String paneText = pane.getText();
		if (!scriptFile.exists())
		{
			if (!scriptFile.getParentFile().exists())
				scriptFile.getParentFile().mkdirs();
			scriptFile.createNewFile();
		}
		//make some thing to write a file with
		FileOutputStream oStream = new FileOutputStream(scriptFile);
		FileChannel output = oStream.getChannel();
		Scanner sc = new Scanner(paneText);
		//read each line one at a time, correcting for proper newline format
		String finalText = "";
		int textLen = 0;
		while (sc.hasNextLine())
		{
			String next = sc.nextLine();
			next = replaceDefines(next);
                        next = removeComments(next);
			textLen += next.length() + 2;
			finalText += next + "\r\n";			
		}
		sc.close();
		ByteBuffer cBuf = ByteBuffer.allocate(finalText.length());
		int key = (int) finalText.charAt((textLen) / 2);
		for (int i = 0; i < textLen; i++)
		{
			if (i != finalText.length() / 2)
				{
					byte nextByte = (byte) finalText.charAt(i);
					nextByte += key;
					cBuf.put(nextByte);
				}
			else
				cBuf.put((byte) (finalText.charAt(i)));
		}
		//cBuf.put((byte) 0);
		cBuf.flip();
		output.write(cBuf);
		output.close();
		oStream.close();		
	}
	
	void saveSource(File sourceFile) throws IOException
	{
		JTextPane pane = getActiveTextPane();
		String paneText = pane.getText();
		if (!sourceFile.exists())
		{
			if (!sourceFile.getParentFile().exists())
				sourceFile.getParentFile().mkdirs();
			sourceFile.createNewFile();
		}
		//make some thing to write a file with
		FileOutputStream oStream = new FileOutputStream(sourceFile);
		FileChannel output = oStream.getChannel();
		Scanner sc = new Scanner(paneText);
		//read each line one at a time, correcting for proper newline format
		String finalText = "";
		int textLen = 0;
		while (sc.hasNextLine())
		{
			String next = sc.nextLine();
			textLen += next.length() + 2;
			finalText += next + "\r\n";			
		}
		sc.close();
		ByteBuffer cBuf = ByteBuffer.wrap(finalText.getBytes());
		output.write(cBuf);
		output.close();
		oStream.close();		
	}
	
	public static void saveDefines() throws IOException
	{
		JTextPane pane = definePane;
		File sourceFile = new File(System.getProperty("user.dir") + "\\tsc_def.txt");
		String paneText = pane.getText();
		if (!sourceFile.exists())
		{
			if (!sourceFile.getParentFile().exists())
				sourceFile.getParentFile().mkdirs();
			sourceFile.createNewFile();
		}
		//make some thing to write a file with
		FileOutputStream oStream = new FileOutputStream(sourceFile);
		FileChannel output = oStream.getChannel();
		Scanner sc = new Scanner(paneText);
		//read each line one at a time, correcting for proper newline format
		String finalText = "";
		int textLen = 0;
		while (sc.hasNextLine())
		{
			String next = sc.nextLine();
			textLen += next.length() + 2;
			finalText += next + "\r\n";			
		}
		sc.close();
		ByteBuffer cBuf = ByteBuffer.wrap(finalText.getBytes());
		output.write(cBuf);
		output.close();
		oStream.close();	
	}
	
	static void highlightDoc(StyledDocument doc, int first, int last) throws IOException
	{
		if (last < first)
			last = Integer.MAX_VALUE;
		TscLexer lexer = new TscLexer();
		try {
			lexer.reset(new StringReader(doc.getText(0, doc.getLength())), first, -1, 0);
			
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		TscToken t;
		while ((t = lexer.getNextToken()) != null)
		{
			doc.setCharacterAttributes(t.getCharBegin(), t.getCharEnd() - t.getCharBegin(), styles.get(t.getDescription()), true);
			if (t.getLineNumber() > last) break;
		}
	}

	public void actionPerformed(ActionEvent event) {
		int menuObj;
		if (event.getSource() == findButton)
			menuObj = -1;
		else
			for (menuObj = 0; menuObj < menuArray.length; menuObj++)
				if (event.getSource() == menuArray[menuObj]) break;
		
		JFileChooser fc;
		FileNameExtensionFilter filter;
		fc = new JFileChooser();
		fc.setCurrentDirectory(lastDir);
		int retVal;
		switch (menuObj)
		{
		case 0: //open
			filter = new FileNameExtensionFilter("Script files", "tsc", "txt");
			fc.setFileFilter(filter);
			retVal = fc.showOpenDialog(frame);
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				lastDir = fc.getCurrentDirectory();
				File selected = fc.getSelectedFile();
				if (selected.getName().endsWith(".tsc"))
				{
				//look for the "script source" file
				String sourcePath = fc.getCurrentDirectory().toString() + "\\ScriptSource\\" + selected.getName().replace(".tsc", ".txt");
				File source = new File(sourcePath);
				if (source.exists())
					addSourceTab(source);
				else
					addScriptTab(selected);
				} else { //is a text file
				addSourceTab(selected);
				}
			}
			break;
		case 1: //save
			try {
				populateDefines();
				File panelFile = fList.get(tabPanel.getSelectedIndex());
				String path = panelFile.getAbsolutePath();
				if (panelFile == defFile) {
					PrintWriter oWrite = new PrintWriter(new BufferedWriter(new FileWriter(panelFile)));
					JTextPane tp = getActiveTextPane();
					oWrite.write(tp.getText());
					oWrite.close();
				} else if (path.endsWith(".tsc")) { //script was selected
					saveScript(panelFile);
					//try to find a source in the subdirectory
					String subPath = panelFile.getParent() + "\\ScriptSource\\" + panelFile.getName().replace(".tsc", ".txt");
					File sourceFile = new File(subPath);
					if (sourceFile.exists())
					{//save it here
						saveSource(sourceFile);
					} else { //look for the file in the same directory
						subPath = panelFile.getParent() + "\\" + panelFile.getName().replace(".tsc", ".txt");
						File altFile = new File(subPath);
						if (altFile.exists())
							saveSource(altFile);
						else
							saveSource(sourceFile);
					}
				} else if (path.endsWith(".txt")){ //wasa source file
					saveSource(panelFile);
					//try to find the script file
					String poorlynamedvariable = panelFile.getParentFile().getName();
					if (poorlynamedvariable.equals("ScriptSource"))
					{
						//in the proper subfolder
						File superFile = new File(panelFile.getParentFile().getParent() + "\\" + panelFile.getName().replace(".txt", ".tsc"));
						saveScript(superFile);
					} else {
						//wrong spot for sources you asshold
						//stick it in the same spot
						File superFile = new File(panelFile.getParent() + "\\" + panelFile.getName().replace(".txt", ".tsc"));
						saveScript(superFile);
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			break;
		case 2: //save as
			filter = new FileNameExtensionFilter("Script files", "tsc");
			fc.setFileFilter(filter);
			retVal = fc.showSaveDialog(frame);
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				populateDefines();
				try {
					File panelFile = fc.getSelectedFile();
					String path = panelFile.getAbsolutePath();
					if (path.endsWith(".tsc")) //script was selected
					{
						saveScript(panelFile);
						//try to find a source in the subdirectory
						String subPath = panelFile.getParent() + "\\ScriptSource\\" + panelFile.getName().replace(".tsc", ".txt");
						File sourceFile = new File(subPath);
						if (sourceFile.exists())
						{//save it here
							saveSource(sourceFile);
						} else { //look for the file in the same directory
							subPath = panelFile.getParent() + "\\" + panelFile.getName().replace(".tsc", ".txt");
							File altFile = new File(subPath);
							if (altFile.exists())
								saveSource(altFile);
							else
								saveSource(sourceFile);
						}
					} else { //wasa source file
						saveSource(panelFile);
						//try to find the script file
						String poorlynamedvariable = panelFile.getParentFile().getName();
						if (poorlynamedvariable.equals("ScriptSource"))
						{
							//in the proper subfolder
							File superFile = new File(panelFile.getParentFile().getParent() + "\\" + panelFile.getName().replace(".txt", ".tsc"));
							saveScript(superFile);
						} else {
							//wrong spot for sources you asshold
							//stick it in the same spot
							File superFile = new File(panelFile.getParent() + "\\" + panelFile.getName().replace(".txt", ".tsc"));
							saveScript(superFile);
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			break;
		case 3: //close (?)
			int index = tabPanel.getSelectedIndex();
			if (index > 0)
			{
				tabPanel.remove(index);
				fList.remove(index);
			}
			break;
		case 4: //pickin' a executable
			filter = new FileNameExtensionFilter("Cave Stories", "bin", "exe", "tbl");
			fc.setFileFilter(filter);
			retVal = fc.showOpenDialog(frame);
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				if (selectWindow != null)
					selectWindow.dispose();
				selectWindow = new ScriptSelector(fc.getSelectedFile(), this);
			}
			break;
		case 5: //We have to make the list of flags
			if (selectWindow != null)
			{
				try {
					selectWindow.generateFlagList();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				StrTools.msgBox("No stage data loaded!");
			}
			break;
		case 6: //We have to make the list of TRAs
			if (selectWindow != null)
			{
				try {
					selectWindow.generateTRAList();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				StrTools.msgBox("No stage data loaded!");
			}
			break;
		default: //it was the search button all along!
			String findText = findField.getText();
			JTextPane pane = getActiveTextPane();
			if (!findText.isEmpty())
			{
				pane.requestFocusInWindow();
				StrTools.findAndSelectTextInTextComponent(pane, findText, true, true, true);
			}
			break;
		}
	}
	
	private static void textBoxKeyReleased(java.awt.event.KeyEvent evt) {
		JTextPane area = getActiveTextPane();
		if (area == definePane)
			return;
		int cPos = area.getCaretPosition();
		//calculate line #
		Scanner sc = null;
		try {
			sc = new Scanner(area.getText(0, area.getDocument().getLength()));
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
		int character = 0, startLine = 0;
		String oldLine;
		String newLine = "";
		while (character < cPos)
		{
			oldLine = newLine;
			newLine = sc.nextLine();
			character += newLine.length() + 1;
			startLine++;
			if (!sc.hasNextLine())
				break;
		}
		
		//colour it
		try {
			TscApp.highlightDoc((StyledDocument)area.getDocument(), startLine - 1, startLine + 1);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//find nearest command
	    int returns = 0;
	    //weed out carriage return characters
	    String txt = area.getText();
	    for (int i = 0; i < cPos; i++)
	    {
	    	if (txt.charAt(i) == '\r')
	    	{
	    		cPos++;
	    		returns++;
	    	}
	    }		
	    txt = txt.substring(0, cPos + 4);
	    int tagPos = txt.lastIndexOf('<');
	    if ((txt.length() - tagPos >= 4))
	    {
		    txt = txt.substring(tagPos, tagPos + 4);
		    int index = commandList.getNextMatch(txt, 0, Position.Bias.Forward);
		    commandList.setSelectedIndex(index);
	    }
	}
	
	private static void pasteActionPerformed(ActionEvent evt) {
		JTextPane pane = getActiveTextPane();
		if (pane == definePane)
			return;
		
		//colour the whole damn thing
		try {
			TscApp.highlightDoc((StyledDocument)pane.getDocument(), 0, -1);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	/* I don't know what I'm doing
	 * this wasn't explained well enough
	protected class UndoListener implements UndoableEditListener {

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			undo.addEdit(e.getEdit());
		}
	}
	*/
}


class ListMan implements ListSelectionListener
{
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false)
		{
			int selection = TscApp.commandList.getSelectedIndex();
			if (selection != -1)
			{
				TscCommand selCom = null;
				if (TscApp.commandInf != null && TscApp.commandInf.size() != 0)
					selCom = TscApp.commandInf.elementAt(selection);
				else
					return;
				String comStr = "Command:" + selCom.name + "\n" + selCom.commandCode;
				for (int i = 0; i < selCom.numParam; i++)
				{
					if (i < 3)
					{
						comStr += (char)('X' + i);
						comStr += (char)('X' + i);
						comStr += (char)('X' + i);
						comStr += (char)('X' + i);
					} else {
						//so lazy
						comStr += (char)('A' + i - 3);
						comStr += (char)('A' + i - 3);
						comStr += (char)('A' + i - 3);
						comStr += (char)('A' + i - 3);
					}
					
					if (i != selCom.numParam - 1)
						comStr += ':';
				}
				TscApp.comLabel.setText(comStr);
				TscApp.descLabel.setText("Description:\n" + selCom.description);
			}
		}			
	}
}
