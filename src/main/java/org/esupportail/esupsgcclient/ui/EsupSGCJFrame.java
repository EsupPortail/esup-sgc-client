package org.esupportail.esupsgcclient.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import org.apache.log4j.Logger;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class EsupSGCJFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final static Logger log = Logger.getLogger(EsupSGCJFrame.class);
	
	private static ClassLoader classLoader = EsupSGCJFrame.class.getClassLoader();;
	
	private JLabel stepReadQR = new JLabel("Lecture du QRCode");
	private JLabel stepReadCSN = new JLabel("Lecture du CSN");
	private JLabel stepSelectSGC = new JLabel("Selection dans le SGC");
	private JLabel stepEncodageApp = new JLabel("Encodage de la carte");
	private JLabel stepEncodageCnous = new JLabel("Encodage CNOUS");
	private JLabel stepSendCSV = new JLabel("Envoi du CSV");
	
	private JTextArea logTextarea = null;
	private String lastText = null;
	private Font fontTitle = Font.getFont("Arial");
	private Font fontStep = Font.getFont("Arial");
	private JLabel textPrincipal = new JLabel();
	private JPanel logPanel;

	
	public Webcam webcam = null;
	public WebcamPanel panel = null;
	public JButton buttonTest = new JButton("Tester une carte");
	public JButton buttonExit = new JButton("Quitter");
	
	Dimension size = WebcamResolution.QVGA.getSize();

	public EsupSGCJFrame() throws HeadlessException {
		super();
		setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
		InputStream is = classLoader.getResourceAsStream("font/Roboto-Regular.ttf");
		InputStream is2 = classLoader.getResourceAsStream("font/Roboto-Regular.ttf");
		try {
			fontTitle = Font.createFont(Font.TRUETYPE_FONT, is);
			fontStep = Font.createFont(Font.TRUETYPE_FONT, is2);
		} catch (FontFormatException e) {
			log.error(e);
			e.printStackTrace();
		} catch (IOException e) {
			log.error(e);
		}   
		fontTitle = fontTitle.deriveFont(26f);
		fontStep = fontStep.deriveFont(16f);
		setLayout(new FlowLayout());
		setTitle("EsupNfcTag - WebCam & PCSC");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		webcam = Webcam.getDefault();
		if(webcam != null) {
			webcam.setViewSize(size);
			webcam.setImageTransformer(new WebcamImageTransformer() {
		            public BufferedImage transform(BufferedImage image) {
		                BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_BGR);
		                Graphics2D g2 = bi.createGraphics();
		                g2.rotate(Math.PI, image.getWidth()/2.0, image.getHeight()/2.0);
		                g2.drawImage(image, 0, 0, null);
		                g2.dispose();
		                bi.flush();
		                return bi;
		            }
		        });
			panel = new WebcamPanel(webcam);
			panel.setPreferredSize(size);
			panel.setFPSLimited(true);
			panel.setFPSLimit(15);
			panel.setFPSDisplayed(true);
			panel.setDisplayDebugInfo(true);
			panel.setImageSizeDisplayed(true);
		}


		logTextarea = new JTextArea();
		logTextarea.setEditable(false);

		DefaultCaret caret = (DefaultCaret)logTextarea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	    
		JScrollPane scroll = new JScrollPane(logTextarea, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setAutoscrolls(true);
		
		textPrincipal.setHorizontalAlignment(JLabel.CENTER);
		textPrincipal.setVerticalAlignment(JLabel.CENTER);
		textPrincipal.setFont(fontTitle);
		
		initSteps();
		
		JPanel processStatus = new JPanel();
		processStatus.setLayout(new BoxLayout(processStatus, BoxLayout.Y_AXIS));
		processStatus.add(stepReadQR);
		processStatus.add(Box.createRigidArea(new Dimension(0,5)));
		processStatus.add(stepReadCSN);
		processStatus.add(Box.createRigidArea(new Dimension(0,5)));
		processStatus.add(stepSelectSGC);
		processStatus.add(Box.createRigidArea(new Dimension(0,5)));
		processStatus.add(stepEncodageApp);
		processStatus.add(Box.createRigidArea(new Dimension(0,5)));
		processStatus.add(stepEncodageCnous);
		processStatus.add(Box.createRigidArea(new Dimension(0,5)));
		processStatus.add(stepSendCSV);
		processStatus.add(Box.createRigidArea(new Dimension(0,20)));
		//processStatus.add(buttonTest);
		processStatus.add(Box.createRigidArea(new Dimension(0,100)));
		//processStatus.add(buttonUnlock);
		processStatus.add(Box.createRigidArea(new Dimension(0,100)));
		processStatus.add(buttonExit);
		
		logPanel = new JPanel();
		logPanel.setLayout(new BorderLayout());
		logPanel.add(scroll);
		logPanel.setPreferredSize(new Dimension(1000, 300));
		
		JPanel instructPanel = new JPanel();
		instructPanel.setLayout(new BorderLayout());
		instructPanel.add(processStatus, BorderLayout.EAST);
		
		JPanel webCamPanel = new JPanel();
		webCamPanel.setLayout(new BorderLayout());
		if(panel!=null){
			webCamPanel.add(panel);
		}else{
			webCamPanel.add(new JLabel("No Webcam"));
		}
		webCamPanel.add(instructPanel , BorderLayout.EAST);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(webCamPanel, BorderLayout.NORTH);
		mainPanel.add(logPanel, BorderLayout.SOUTH);
		
		getContentPane().add(mainPanel);
		pack();
		setVisible(true);
		logPanel.setVisible(true);

	}
	
	public void changeTextPrincipal(String text){
		textPrincipal.setText(text);
	}

	public void initSteps(){
		stepReadQR.setFont(fontStep);
		setStepReadQR(Color.GRAY);
		stepReadCSN.setFont(fontStep);
		setStepReadCSN(Color.GRAY);
		stepSelectSGC.setFont(fontStep);
		setStepSelectSGC(Color.GRAY);
		stepEncodageApp.setFont(fontStep);
		setStepEncodageApp(Color.GRAY);
		stepEncodageCnous.setFont(fontStep);
		setStepEncodageCnous(Color.GRAY);
		stepSendCSV.setFont(fontStep);
		setStepSendCSV(Color.GRAY);
	}
	
	public void setStepReadQR(Color color) {
		if(color != null) stepReadQR.setForeground(color);
	}

	public void setStepReadCSN(Color color) {
		if(color != null) stepReadCSN.setForeground(color);
	}
	
	public void setStepSelectSGC(Color color) {
		if(color != null) stepSelectSGC.setForeground(color);
	}

	public void setStepEncodageApp(Color color) {
		if(color != null) stepEncodageApp.setForeground(color);
	}

	public void setStepEncodageCnous(Color color) {
		if(color != null) stepEncodageCnous.setForeground(color);
	}

	public void setStepSendCSV(Color color) {
		if(color != null) stepSendCSV.setForeground(color);
	}
	
	public void addLogText(String text) {
		logTextarea.append(text);
		lastText = text;
		logTextarea.setCaretPosition(logTextarea.getDocument().getLength());
	}
	
	public void addLogTextLn(String text) {
		if(!text.equals(lastText)) {
			logTextarea.append(text + "\n");
			lastText = text;
			logTextarea.setCaretPosition(logTextarea.getDocument().getLength());
		}
	}
	
	public void exit(){
		if(webcam != null){
			panel.stop();
			webcam.close();
		}
	}
	
}
