package tsc;


import java.io.*;

import java.util.*;

//import com.Ostermiller.Syntax.*;
//import com.Ostermiller.Syntax.Lexer.Lexer;
//import com.Ostermiller.Syntax.Lexer.Token;
@SuppressWarnings("unused")
public class TscLexer
{
	private Scanner sc = new Scanner(System.in);
		
	private int charCount;
	private boolean isFace = false;
	private boolean overLimit = false;
	private boolean wasEnded = false;
	private int lineNum;
	private int col;
	private int character;
	
	private String line;
	private int strPos;
	private int argsRemaining;
	
	private TscToken lastToken;
	
	private static Map<String, Integer> argMap = new Hashtable<String, Integer>();
	
	TscLexer()
	{
		if (argMap.size() <= 0)
			initMap();
	}
	
	TscLexer(InputStream in)
	{
		sc.close();
		sc = new Scanner(in);
		line = sc.nextLine();
		lineNum = 0;
		col = 0;
		character = 0;
		strPos = 0;
		
		if (argMap.size() <= 0)
			initMap();
	}
	
	TscLexer(Reader in)
	{
		sc.close();
		sc = new Scanner(in);
		line = sc.nextLine();
		character = 0;
		lineNum = 0;
		col = 0;
		strPos = 0;
		
		if (argMap.size() <= 0)
			initMap();
	}
	
	void initMap()
	{
		for (int i = 0; i < TscApp.commandInf.size(); i++)
		{
			argMap.put(TscApp.commandInf.elementAt(i).commandCode, TscApp.commandInf.elementAt(i).numParam);
		}
	}
	
	public TscToken getNextToken() throws IOException 
	{		
		if (strPos >= line.length())
		{
			strPos = 0;
			try {
				line = sc.nextLine();
			} catch (NoSuchElementException e) {
				return null;
			}
			lineNum++;
			character++;
			charCount = 0;
			overLimit = false;
		}
		if (line.equals(""))
			return getNextToken();
		TscToken nextToken = null;
		String tokenStr = "";
		char nextChar = line.charAt(strPos);
		if (nextChar == '#')
		{
			//try to grab the value one character at a time
			for (int i = 0; i < 5; i++)
			{
				try { 
					tokenStr += line.charAt(strPos + i);
				} catch (IndexOutOfBoundsException e) {
					nextToken = new TscToken("eveNum", tokenStr, lineNum, character, character + tokenStr.length());
					character += tokenStr.length();
					strPos += tokenStr.length();
					lastToken = nextToken;
					return nextToken;
				}
			}
			nextToken = new TscToken("eveNum", tokenStr, lineNum, character, character+5);
			character  += 5;
			wasEnded = false;
			isFace = false;
			strPos += 5;
		} else if (argsRemaining > 0) {
			if ((lastToken.getContents().charAt(0) == '<') || (lastToken.getContents().length() == 1))
			{
				//number token
				//try to grab the value one character at a time
				for (int i = 0; i < 4; i++)
				{
					try { 
						tokenStr += line.charAt(strPos + i);
					} catch (IndexOutOfBoundsException e) {
						nextToken = new TscToken("number", tokenStr, lineNum, character, character + tokenStr.length());
						character += tokenStr.length();
						strPos += tokenStr.length();
						lastToken = nextToken;
						return nextToken;
					}
				}
				nextToken = new TscToken("number", tokenStr, lineNum, character, character + 4);
				if (lastToken.getContents().equals("<FAC") &&
					tokenStr.equals("0000"))
					isFace = false;
				character += 4;
				strPos += 4;
				argsRemaining--;
			} else {
				//spacer token
				tokenStr += line.charAt(strPos);
				nextToken = new TscToken("spacer", tokenStr, lineNum, character, character + 1);
				character++;
				strPos++;
			}
		} else if (wasEnded) {
			int tokenLen = line.substring(strPos).length();
			nextToken = new TscToken("comment", line.substring(strPos), lineNum, character, character + tokenLen);
			character += tokenLen;
			strPos += tokenLen;
		} else {
			if (nextChar == '<')
			{
				//try to grab the value one character at a time
				for (int i = 0; i < 4; i++)
				{
					try { 
						tokenStr += line.charAt(strPos + i);
					} catch (IndexOutOfBoundsException e) {
						nextToken = new TscToken("tag", tokenStr, lineNum, character, character + tokenStr.length());
						character += tokenStr.length();
						strPos += tokenStr.length();
						lastToken = nextToken;
						return nextToken;
					}
				}
				if (tokenStr != null)
				{
					if (argMap.containsKey(tokenStr))
						argsRemaining = argMap.get(tokenStr);
					else
						argsRemaining = 0;
				} else {
					argsRemaining = 0;
					tokenStr = "err";
				}
				if (tokenStr.equals("<FAC"))
					isFace = true;
				if (tokenStr.equals("<END") || 
					tokenStr.equals("<TRA") || 
					tokenStr.equals("<EVE") || 
					tokenStr.equals("<LDP") || 
					tokenStr.equals("<INI") ||
					tokenStr.equals("<ESC") )
					wasEnded = true;
				if (tokenStr.equals("<CLR"))
				{
					charCount = 0;
					overLimit = false;
				}
				nextToken = new TscToken("tag", tokenStr, lineNum, character, character + 4);
				character += 4;
				strPos += 4;
				
			} else {
				tokenStr = "";
				boolean setOver = false;
				while (true)
				{
					if (isFace)
					{
						if ((charCount > 27) && (!overLimit))
						{
							setOver = true;
							break;
						}
					} else {
						if ((charCount > 34) && (!overLimit))
						{
							setOver = true;
							break;
						}
					}
					tokenStr += nextChar;
					strPos++;
					if (strPos >= line.length()) break;
					nextChar = line.charAt(strPos);
					if (nextChar == '<') break;
					charCount++;
				}
				if (!overLimit)
					nextToken = new TscToken("text", tokenStr, lineNum, character, character + tokenStr.length());
				else
					nextToken = new TscToken("overflow", tokenStr, lineNum, character, character + tokenStr.length());
				if (setOver)
					overLimit = true;
				character += tokenStr.length();
			}
		}
		lastToken = nextToken;
		return nextToken;
	}

	
	public void reset(Reader reader, int yyline, int yychar, int yycolumn)
			throws IOException 
	{		
		sc.close();
		sc = new Scanner(reader);
		if (yychar < 0)
		{
			for (int i = 0; i < yyline; i++)
			{
				line = sc.nextLine();
				character += line.length() + 1;
			}
			lineNum = yyline;
			strPos = yycolumn;
			if (line != null)
			{
				character -= (line.length() - strPos + 1);
			} else {
				line = sc.nextLine();
				character = 0;
			}
		} else {
			character = 0;
			while (character < yychar)
			{
				line = sc.nextLine();
				character += line.length() + 1;
				lineNum++;
			}
			if (character > yychar)
			{
				strPos = line.length() - (character - yychar);
				character = yychar;
			}
		}
	}
}

class TscToken
{
	private int ID;
	private String desc;
	private String token;
	private boolean comment;
	private boolean whitespace;
	private boolean error;
	private int lineNum;
	private int charBegin;
	private int charEnd;
	private String strErr;
	private int state;
	
	TscToken(String description, String tokenStr, int lineNum_in, int charBegin_in, int charEnd_in)
	{
		ID = 0;
		desc = description;
		token = tokenStr;
		lineNum = lineNum_in;
		charBegin = charBegin_in;
		charEnd = charEnd_in;
		strErr = "No error";
		comment = false;
		whitespace = false;
		error = false;
		state = 0;
	}
	
	public int getID() {return ID;}
	public String getDescription() {return desc;}
	public String getContents() {return token;}
	public boolean isComment() {return comment;}
	public boolean isWhiteSpace() {return whitespace;}
	public boolean isError() {return error;}
	public int getLineNumber() {return lineNum;}
	public int getCharBegin() {return charBegin;}
	public int getCharEnd() {return charEnd;}
	public String errorString() {return strErr;}
	public int getState() {return state;}	
}