package org.esupportail.esupsgcclient.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.log4j.Logger;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class EsupSGCJFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final static Logger log = Logger.getLogger(EsupSGCJFrame.class);
	
	private static ClassLoader classLoader = EsupSGCJFrame.class.getClassLoader();;
	
	private static Color BACK = new Color(224, 224, 224);
		
	public JLabel stepClientReady = new JLabel("Client en cours de chargement");
	public JLabel stepReadQR = new JLabel("Lecture du QRCode");
	public JLabel stepReadCSN = new JLabel("Lecture du CSN");
	public JLabel stepSelectSGC = new JLabel("Selection dans le SGC");
	public JLabel stepEncodageApp = new JLabel("Encodage de la carte");
	public JLabel stepEncodageCnous = new JLabel("Encodage CNOUS");
	public JLabel stepSendCSV = new JLabel("Envoi du CSV");
	
	private JTextArea logTextarea = new JTextArea();
	private String lastText = null;
	private Font fontTitle = Font.getFont("Arial");
	private Font fontStep = Font.getFont("Arial");
	private JLabel textPrincipal = new JLabel("", SwingConstants.LEFT);
	
	public Webcam webcam = null;
	public WebcamPanel webCamPanel = null;
	public JPanel webCamJPanel = new JPanel();
	
	public JPanel logJPanel = new JPanel();
	
	public JButton buttonLogs = new JButton("Masquer les logs");
	public JButton buttonExit = new JButton("Quitter");
	public JButton buttonRestart = new JButton("Restart");
	
	public EsupSGCJFrame() throws HeadlessException {
		super();
		
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		    	if ("GTK+".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    log.warn("swing theme error");
		}
		
		this.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
		this.setTitle("ESUP-SCG-CLIENT");
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		BoxLayout boxLayout = new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS);
		this.setLayout(boxLayout);
		Dimension size = new Dimension(1000, 1500);
		this.setPreferredSize(size);
		this.setExtendedState(JFrame.NORMAL);
		
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
		
		fontTitle = fontTitle.deriveFont(30f);
		fontStep = fontStep.deriveFont(24f);
		
		JPanel titleJPanel = new JPanel();
		titleJPanel.setLayout(new BoxLayout(titleJPanel, BoxLayout.X_AXIS));
		titleJPanel.setBackground(BACK);
		JLabel title = new JLabel("", SwingConstants.LEFT);
		title.setFont(fontTitle);
		title.setText("ESUP-SGC-CLIENT : ");
		titleJPanel.add(title);
		titleJPanel.add(textPrincipal);
		titleJPanel.setMaximumSize(new Dimension(1000, 50));
		titleJPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		
		webCamJPanel.setLayout(new BorderLayout());
		webCamJPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		webCamJPanel.setMaximumSize(new Dimension(1000, 240));
		webCamJPanel.setBackground(BACK);
		
		JPanel processJPanel = new JPanel();
		processJPanel.setLayout(new BoxLayout(processJPanel, BoxLayout.Y_AXIS));
		processJPanel.setBackground(BACK);
		processJPanel.add(stepClientReady);
		processJPanel.add(Box.createRigidArea(new Dimension(0,5)));
		processJPanel.add(stepReadQR);
		processJPanel.add(Box.createRigidArea(new Dimension(0,5)));
		processJPanel.add(stepReadCSN);
		processJPanel.add(Box.createRigidArea(new Dimension(0,5)));
		processJPanel.add(stepSelectSGC);
		processJPanel.add(Box.createRigidArea(new Dimension(0,5)));
		processJPanel.add(stepEncodageApp);
		processJPanel.add(Box.createRigidArea(new Dimension(0,5)));
		processJPanel.add(stepEncodageCnous);
		processJPanel.add(Box.createRigidArea(new Dimension(0,5)));
		processJPanel.add(stepSendCSV);
		processJPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		processJPanel.setMaximumSize(new Dimension(1000, 250));
		
		JPanel buttonsJPanel = new JPanel();
		buttonsJPanel.setLayout(new BoxLayout(buttonsJPanel, BoxLayout.X_AXIS));
		buttonsJPanel.setMaximumSize(new Dimension(1000, 50));
		buttonsJPanel.setBackground(BACK);
		buttonsJPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonsJPanel.add(buttonRestart);
		buttonsJPanel.add(Box.createRigidArea(new Dimension(5,0)));
		buttonsJPanel.add(buttonLogs);
		buttonsJPanel.add(Box.createRigidArea(new Dimension(5,0)));
		buttonsJPanel.add(buttonExit);

		
		
		logJPanel.setLayout(new BorderLayout());
		logJPanel.setMaximumSize(new Dimension(1000, 250));
		logJPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		logTextarea.setEditable(false);
		JScrollPane scroll = new JScrollPane(logTextarea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setAutoscrolls(true);
		logJPanel.add(scroll);
		
		buttonLogs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(logJPanel.isVisible()){
					logJPanel.setVisible(false);
					buttonLogs.setText("Afficher les logs");
				}else{
					logJPanel.setVisible(true);
					buttonLogs.setText("Masquer les logs");
				}
			}
		});
		
		JPanel mainJPanel = new JPanel();
		int border = 20;
		mainJPanel.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
		mainJPanel.setMaximumSize(new Dimension(1000, 1500));
		mainJPanel.setBackground(BACK);
		mainJPanel.setLayout(new BoxLayout(mainJPanel, BoxLayout.Y_AXIS));
		
		mainJPanel.add(titleJPanel);
		mainJPanel.add(webCamJPanel);
		mainJPanel.add(processJPanel);
		mainJPanel.add(buttonsJPanel);
		mainJPanel.add(logJPanel);
		
		this.getContentPane().add(mainJPanel);

		this.pack();
		this.setVisible(true);
	}
	
	public void initWebCam() throws WebcamException {

		Dimension[] nonStandardResolutions = new Dimension[] {
	            WebcamResolution.PAL.getSize(),
	            WebcamResolution.HD720.getSize(),
	            new Dimension(720, 480),
	            new Dimension(1920, 1080),
	    };

		Dimension size = WebcamResolution.HD720.getSize();
		try{
			webcam = Webcam.getDefault();
			webcam.setCustomViewSizes(nonStandardResolutions);
	
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
				webCamPanel = new WebcamPanel(webcam);
				webCamPanel.setPreferredSize(new Dimension(460, 240));
				webCamPanel.setFPSLimited(true);
				webCamPanel.setFPSLimit(15);
				webCamPanel.setFPSDisplayed(false);
				webCamPanel.setDisplayDebugInfo(false);
				webCamPanel.setImageSizeDisplayed(true);
				webCamJPanel.add(webCamPanel);
				
			}
		}catch (Exception e){
			log.error("webcam error" + e.getMessage());
			throw new WebcamException(e);
		}
	}
	
	public void changeTextPrincipal(String text, Color color){
		textPrincipal.setText(text);
		textPrincipal.setForeground(color);
	}
	
	public void initSteps(){
		stepClientReady.setFont(fontStep);
		stepClientReady.setForeground(Color.GRAY);		
		stepReadQR.setFont(fontStep);
		stepReadQR.setForeground(Color.GRAY);
		stepReadCSN.setFont(fontStep);
		stepReadCSN.setForeground(Color.GRAY);
		stepSelectSGC.setFont(fontStep);
		stepSelectSGC.setForeground(Color.GRAY);
		stepEncodageApp.setFont(fontStep);
		stepEncodageApp.setForeground(Color.GRAY);
		stepEncodageCnous.setFont(fontStep);
		stepEncodageCnous.setForeground(Color.GRAY);
		stepSendCSV.setFont(fontStep);
		stepSendCSV.setForeground(Color.GRAY);
		textPrincipal.setFont(fontTitle);
		textPrincipal.setForeground(Color.DARK_GRAY);
	}

	public void addLogText(String text) {
		logTextarea.append(text);
		lastText = text;
		logTextarea.setCaretPosition(logTextarea.getDocument().getLength());
	}
	
	public void addLogTextLn(String type, String text) {
		if(!text.equals(lastText)) {
			String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
			logTextarea.append("["+type + "] " + date + " - " + text + "\n");
			lastText = text;
			logTextarea.setCaretPosition(logTextarea.getDocument().getLength());
		}
	}
	
	public void exit(){
		if(webcam != null){
			if(webCamPanel != null) {
				webCamPanel.stop();
			}
			webcam.close();
		}
	}
	
}
