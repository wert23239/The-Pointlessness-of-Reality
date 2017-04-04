package tsc;


import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

public class CloseFrameAction extends JFrame {

	private static final long serialVersionUID = 1L;

	public CloseFrameAction(String s)
	{
		setTitle(s);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e) 
			{
				//save defines
				try {
					TscApp.saveDefines();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.exit(0);
			}
		});
	}
}