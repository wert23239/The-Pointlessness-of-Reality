package tsc;


import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;

import javax.swing.*;

@SuppressWarnings("serial")
public class ScriptSelector extends JFrame {
	private JList stageList;
	private File originFile;
	
	private TscApp parent;
	
	private Vector<String> fileList = new Vector<String>();
	private Vector<String> mapNameList = new Vector<String>();
	
	//some defines for the shits
	long SECTION_OFFSET = 0x208; //this shouldn't change unless someone really screws things up
	
	ScriptSelector(File inFile, TscApp p)
	{
		parent = p;
		originFile = inFile;
		try {
			populateList(inFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		addComponentsToPane(this.getContentPane());
		this.setTitle("Script Selector");
		this.pack();
		this.setMinimumSize(this.getSize());
		this.setVisible(true);
	}
	
	public void generateFlagList() throws IOException
	{
		//make sure we have a list to read
		if (fileList.isEmpty())
			return;
		//create something to read our input
		BufferedWriter output = null;
		output = new BufferedWriter(new FileWriter("FlagListing.txt"));
		//this is the wicked setup I've got going for
		LinkedList<Integer> fList = new LinkedList<Integer>();
		HashMap<Integer, Vector<String>> locTable = new HashMap<Integer, Vector<String>>();
		for (int i = 0; i < fileList.size(); i++)
		{
			String subDir;
			if (originFile.getName().endsWith(".bin"))
			{
				//bin mapdata, one layer deep
				subDir = "\\Stage\\";
			} else { //exe mapdata, two layers deep
				subDir = "\\data\\Stage\\";
			}
			String sourcePath = originFile.getParent() + subDir + fileList.get(i) + ".tsc";
			File sourceFile = new File(sourcePath);
			if (!sourceFile.exists()) //make sure it's actually there before we go at it
				continue;
			//parse the file
			TscLexer tLex = new TscLexer();
			tLex.reset(new StringReader(TscApp.parseScript(sourceFile)), 0, -1, 0);
			TscToken t;
			int currentEvent = 0;
			while ((t = tLex.getNextToken()) != null)
			{
				if (t.getDescription().equals("eveNum"))
				{
					currentEvent = StrTools.ascii2Num_CS(t.getContents().substring(1));
				} else if (t.getContents().equals("<FL+") || t.getContents().equals("<FL-") || t.getContents().equals("<FLJ")) {
					String tag = t.getContents();
					if ((t = tLex.getNextToken()) != null)
					{
						int flagNum = StrTools.ascii2Num_CS(t.getContents());
						Vector<String> locList;
						if (fList.contains(flagNum))
						{
							locList = locTable.get(flagNum);
						} else {
							locList = new Vector<String>();
							locTable.put(flagNum, locList);
							fList.add(flagNum);
						}
						locList.add("\t " + tag + " " + sourceFile.getName() + " event #" + currentEvent + "\r\n");
					} else {//if there is a next token
						break;
					}
				} //elseif token was flag+ or flag-
			} //while we have more tokens
		}//for each file
		//sort the flag list
		int[] fArray = new int[fList.size()];
		for (int i = 0; i < fList.size(); i++)
		{
			fArray[i] = fList.get(i);
		}
		java.util.Arrays.sort(fArray);
		for (int i = 0; i < fArray.length; i++)
		{
			output.write("Flag " + fArray[i] + "\r\n");
			Vector<String> locList = locTable.get(fArray[i]);
			for (int j = 0; j < locList.size(); j++)
			{
				output.write(locList.get(j));
			}
		}
		output.close();
		StrTools.msgBox("Flag parsing complete.\nOutput written to FlagListing.txt");
	}
	
	public void generateTRAList() throws IOException
	{
		//make sure we have a list to read
		if (fileList.isEmpty())
			return;
		//create something to read our input
		BufferedWriter output = null;
		output = new BufferedWriter(new FileWriter("TRAListing.txt"));
		//this is the wicked data setup I've got going
		LinkedList<Integer> tList = new LinkedList<Integer>(); //list of integers to index the hashmap
		HashMap<Integer, Vector<String>> locTable = new HashMap<Integer, Vector<String>>(); //vectors of strings (info) indexed by map number
		for (int i = 0; i < fileList.size(); i++)
		{
			String subDir;
			if (originFile.getName().endsWith(".bin"))
			{
				//bin mapdata, one layer deep
				subDir = "\\Stage\\";
			} else { //exe mapdata, two layers deep
				subDir = "\\data\\Stage\\";
			}
			String sourcePath = originFile.getParent() + subDir + fileList.get(i) + ".tsc";
			File sourceFile = new File(sourcePath);
			if (!sourceFile.exists()) //make sure it's actually there before we go at it
				continue;
			//parse the file
			TscLexer tLex = new TscLexer();
			tLex.reset(new StringReader(TscApp.parseScript(sourceFile)), 0, -1, 0);
			TscToken t;
			int currentEvent = 0;
			while ((t = tLex.getNextToken()) != null)
			{
				if (t.getDescription().equals("eveNum"))
				{
					currentEvent = StrTools.ascii2Num_CS(t.getContents().substring(1));
				} else if (t.getContents().equals("<TRA")) {
					int[] params = new int[4];
					int pAssigned = 0;
					//get all parameters for command
					while (pAssigned < 4)
					{
						t = tLex.getNextToken();
						if (t == null)
							break;
						if (t.getDescription().equals("number"))
						{
							params[pAssigned] = StrTools.ascii2Num_CS(t.getContents());
							pAssigned++;
						}
						
					}
					//make list
					Vector<String> locList;
					if (tList.contains(params[0]))
					{
						locList = locTable.get(params[0]);
					} else {
						locList = new Vector<String>();
						locTable.put(params[0], locList);
						tList.add(params[0]);
					}
					locList.add("\t " + sourceFile.getName() + " event #" + currentEvent + " TRA to coords (" + params[2] + "," + params[3] + ") running event " + params[1] + "\r\n");
					if (t == null)
						break;
				} //elseif token was tra
			} //while we have more tokens
		}//for each file
		//sort the flag list
		int[] tArray = new int[tList.size()];
		for (int i = 0; i < tList.size(); i++)
		{
			tArray[i] = tList.get(i);
		}
		java.util.Arrays.sort(tArray);
		for (int i = 0; i < tArray.length; i++)
		{
			output.write("Transport scripts to map " + tArray[i] + " " + mapNameList.get(tArray[i]) + "\r\n");
			Vector<String> locList = locTable.get(tArray[i]);
			for (int j = 0; j < locList.size(); j++)
			{
				output.write(locList.get(j));
			}
		}
		output.close();
		StrTools.msgBox("TRA parsing complete.\nOutput written to TRAListing.txt");
	}
	
	public void addComponentsToPane(Container pane)
	{
		//stageList.setPreferredSize(new Dimension(200, 400));
		stageList.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
				{
					String filename = fileList.get(stageList.getSelectedIndex());
					parent.listAction(filename, originFile);
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				
			}

		});
		
		JScrollPane listScroll = new JScrollPane(stageList);
		
		listScroll.setMinimumSize(new Dimension(200, 400));
		listScroll.setPreferredSize(new Dimension(200, 420));
		pane.add(listScroll);
	}
	
	public void populateList(File f) throws IOException
	{
		Vector<String> list = new Vector<String>();
		FileChannel inChan = null;
		
		FileInputStream inStream;
		inStream = new FileInputStream(f);
		inChan = inStream.getChannel();
		
		if (f.getName().endsWith(".exe"))
		{
			//the hard part
			ByteBuffer uBuf = ByteBuffer.allocate(2);
			uBuf.order(ByteOrder.LITTLE_ENDIAN);
			//find how many sections
			inChan.position(0x116);
			inChan.read(uBuf);
			uBuf.flip();
			int numSection = uBuf.getShort();
			//read each segment
			//find the .csmap or .swdata segment
			int mapSec = -1;
			String[] secHeaders = new String[numSection];
			for (int i = 0; i < numSection; i++)
			{
				uBuf = ByteBuffer.allocate(8);
				inChan.position(0x208 + 0x28 * i);
				inChan.read(uBuf);
				uBuf.flip();
				String segStr = new String(uBuf.array());
				
				if (segStr.contains(".csmap"))
					mapSec = i;
				else if (segStr.contains(".swdata"))
					mapSec = i;
				secHeaders[i] = segStr;
			}
			
			if (mapSec == -1) //virgin executable
			{
				int numMaps = 95;
				inChan.position(0x937B0); //seek to start of mapdatas
				for (int i = 0; i < numMaps; i++)
				{
					//for each map
					uBuf = ByteBuffer.allocate(200);
					inChan.read(uBuf);
					uBuf.flip();
					/*
					typedef struct {
						   char tileset[32];
						   char filename[32];
						   char scrollType[4];
						   char bgName[32];
						   char npc1[32];
						   char npc2[32];
						   char bossNum;
						   char mapName[35];
						}nMapData;
						*/
					char nextChar;
					//read map name
					String mapName = "";
					for (int j = 0; j < 35; j++)
					{
						nextChar = (char)uBuf.get(165 + j);
						if (nextChar == 0)
							break;
						else
							mapName += nextChar;
					}
					
					//read file name
					String fileName = "";
					for (int j = 0; j < 32; j++)
					{
						nextChar = (char) uBuf.get(32 + j);
						if (nextChar == 0)
							break;
						else
							fileName += nextChar;
					}
					list.add(mapName + " - " + fileName);
					fileList.add(fileName);					
				} //for each map
			} else { //exe has been edited probably
				if (secHeaders[mapSec].contains(".csmap"))
				{
					uBuf = ByteBuffer.allocate(4);
					uBuf.order(ByteOrder.LITTLE_ENDIAN);
					inChan.position(0x208 + 0x28*mapSec + 0x10);
					inChan.read(uBuf);
					uBuf.flip();
					int numMaps = uBuf.getInt() / 200;
					uBuf.flip();
					inChan.read(uBuf);
					uBuf.flip();
					int pData = uBuf.getInt();
					
					inChan.position(pData);//seek to start of CS map data
					for (int i = 0; i < numMaps; i++)
					{
						//for each map
						uBuf = ByteBuffer.allocate(200);
						inChan.read(uBuf);
						uBuf.flip();
						/*
						typedef struct {
							   char tileset[32];
							   char filename[32];
							   char scrollType[4];
							   char bgName[32];
							   char npc1[32];
							   char npc2[32];
							   char bossNum;
							   char mapName[35];
							}nMapData;
							*/
						char nextChar;
						//read map name
						String mapName = "";
						for (int j = 0; j < 35; j++)
						{
							nextChar = (char)uBuf.get(165 + j);
							if (nextChar == 0)
								break;
							else
								mapName += nextChar;
						}
						
						//read file name
						String fileName = "";
						for (int j = 0; j < 32; j++)
						{
							nextChar = (char) uBuf.get(32 + j);
							if (nextChar == 0)
								break;
							else
								fileName += nextChar;
						}
						list.add(mapName + " - " + fileName);
						mapNameList.add(mapName);
						fileList.add(fileName);					
					} //for each map
				} else {
					//sue's shit
					//read up where yo' data is at
					StrTools.msgBox("Warning!\nUsing Sue's Workshop is not advised!");
					uBuf = ByteBuffer.allocate(4);
					uBuf.order(ByteOrder.LITTLE_ENDIAN);
					inChan.position(0x208 + 0x28*mapSec + 0x10);
					inChan.read(uBuf);
					uBuf.flip();
					@SuppressWarnings("unused")
					int numMaps = uBuf.getInt() / 200;
					uBuf.flip();
					inChan.read(uBuf);
					uBuf.flip();
					int pData = uBuf.getInt();
					inChan.position(pData + 0x10);//seek to start of Sue's map data
					while (true)
					{
						//for each map
						uBuf = ByteBuffer.allocate(200);
						inChan.read(uBuf);
						uBuf.flip();
						
						//check if it's the FFFFFFFFFFFFFFinal map
						if (uBuf.getInt(0) == -1)
							break;
						/*
						typedef struct {
							   char tileset[32];
							   char filename[32];
							   char scrollType[4];
							   char bgName[32];
							   char npc1[32];
							   char npc2[32];
							   char bossNum;
							   char mapName[35];
							}nMapData;
							*/
						char nextChar;
						//read map name
						String mapName = "";
						for (int j = 0; j < 35; j++)
						{
							nextChar = (char)uBuf.get(165 + j);
							if (nextChar == 0)
								break;
							else
								mapName += nextChar;
						}
						
						//read file name
						String fileName = "";
						for (int j = 0; j < 32; j++)
						{
							nextChar = (char) uBuf.get(32 + j);
							if (nextChar == 0)
								break;
							else
								fileName += nextChar;
						}
						list.add(mapName + " - " + fileName);
						mapNameList.add(mapName);
						fileList.add(fileName);
					} //for each map
				}
			}
			
		} else if (f.getName().endsWith("bin")){ //is a bin
			//int maps array data
			ByteBuffer nBuf = ByteBuffer.allocate(4);
			nBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(nBuf);
			nBuf.flip();
			int numMaps = nBuf.getInt();
			ByteBuffer dBuf = ByteBuffer.allocate(numMaps * 116);
			inChan.read(dBuf);
			inChan.close();
			inStream.close();
			dBuf.flip();
			
			for (int i = 0; i < numMaps; i++) //for each map
			{
				/*
				typedef struct {
					   char tileset[16];
					   char filename[16];
					   char scrollType;
					   char bgName[16];
					   char npc1[16];
					   char npc2[16];
					   char bossNum;
					   char mapName[34];
					}nMapData;
					*/
				char nextChar;
				String nextMap = "";
				for (int j = 0; j < 34; j++)
				{
					nextChar = (char)dBuf.get(116 * i + 82 + j);
					if (nextChar == 0)
						break;
					else
						nextMap += nextChar;
				}
				String nextFile = "";
				for (int j = 0; j < 16; j++)
				{
					nextChar = (char)dBuf.get(116 * i + 16 + j);
					if (nextChar == 0)
						break;
					else
						nextFile += nextChar;
				}
				
				list.add(nextMap + " - " + nextFile);
				mapNameList.add(nextMap);
				fileList.add(nextFile);
			}
		} else if (f.getName().endsWith("tbl")){ //CS+ type
			//int maps array data
			int numMaps = (int) (f.length() / 229);
			ByteBuffer dBuf = ByteBuffer.allocate(numMaps * 229);
			inChan.read(dBuf);
			inChan.close();
			inStream.close();
			dBuf.flip();
			
			for (int i = 0; i < numMaps; i++) //for each map
			{
				/*
				typedef struct {
					   char tileset[32];
					   char filename[32];
					   char scrollType[4];
					   char bgName[32];
					   char npc1[32];
					   char npc2[32];
					   char bossNum;
					   char ????[32];
					   char mapName[32];
					}nMapData;
					*/
				char nextChar;
				String nextMap = "";
				for (int j = 0; j < 34; j++)
				{
					nextChar = (char)dBuf.get(229 * i + 197 + j);
					if (nextChar == 0)
						break;
					else
						nextMap += nextChar;
				}
				String nextFile = "";
				for (int j = 0; j < 16; j++)
				{
					nextChar = (char)dBuf.get(229 * i + 32 + j);
					if (nextChar == 0)
						break;
					else
						nextFile += nextChar;
				}
				
				list.add(nextMap + " - " + nextFile);
				mapNameList.add(nextMap);
				fileList.add(nextFile);
			}
		}
		stageList = new JList(list);
	}
}
