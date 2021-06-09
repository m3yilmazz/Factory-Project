import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class UserClient extends JFrame implements WindowListener, KeyListener {
	private static InetAddress host;
	private static final int PORT = 9999;
	static Socket socket = null;
	static Scanner networkInput;
	static PrintWriter networkOutput;

	private static String UserName = null, UserPassword = null;

	private static GsonBuilder gsonBuilder = new GsonBuilder();
	private static Gson gson = gsonBuilder.serializeNulls().create();

	private final JPanel userPropsPanel = new JPanel( new GridLayout( 2, 2, 5, 5 ) );
	private final JPanel buttonAndResponseCodePanel = new JPanel( new GridLayout( 2, 1, 5, 5 ) );

	private final JTextField userNameJTextField = new JTextField();
	private final JTextField userPasswordJTextField = new JTextField();
	private final JButton loginJButton = new JButton( "LOGIN" );
	private final JTextArea loginResponseJTextArea = new JTextArea();

	private final JPanel commandButtonsPanel = new JPanel( new GridLayout( 8, 1, 5, 5 ) );
	private final JPanel getMachineInformationInputPanel = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
	private final JPanel sendJobOrderInputPanel = new JPanel( new GridLayout( 4, 2, 5, 5 ) );
	private final JPanel responseJPanel = new JPanel( new GridLayout( 1, 1, 5, 5 ) );

	private final JButton getMachinesJButton = new JButton( "Get Machines" );
	private final JButton getMachineInformationsJButton = new JButton( "Get Machine Informations" );
	private final JButton sendJobOrderJButton = new JButton( "Send Job Order" );
	private final JButton getPendingJobOrdersJButton = new JButton( "Get Pending Job Orders" );
	private final JButton getMachineStatesJButton = new JButton( "Get Machine States" );
	private final JButton getProcessingJobOrdersInMachinesStatesJButton = new JButton( "Get Processing Job Orders In Machines" );

	private final JTextField machineUniqueIdJTextField = new JTextField();
	private final JTextField jobOrderUniqueIdJTextField = new JTextField();
	private final JTextField jobOrderTypeJTextField = new JTextField();
	private final JTextField jobOrderProductionLengthJTextField = new JTextField();

	private final JTextArea responseJTextArea = new JTextArea();

	private final JFrame mainJFrame = this;
	private final Window mainWindow = this;
	private final Container mainContainer = this;

	UserClient(Scanner networkInput, PrintWriter networkOutput, Gson gson){
		super( "User Client" );
		setLayout( new GridLayout( 2, 1, 10, 10 ) );

		userPropsPanel.add( new JLabel( "Username" ) );
		userPropsPanel.add( userNameJTextField );
		userPropsPanel.add( new JLabel( "Password" ) );
		userPropsPanel.add( userPasswordJTextField );
		loginJButton.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event)
				{
					String message, userName, userPassword, arguments;

					userName = userNameJTextField.getText();
					userPassword = userPasswordJTextField.getText();

					UserName = userName;
					UserPassword = userPassword;

					arguments = gson.toJson(new LoginRequest(userName, userPassword));

					message = Commands.LOGIN + "*" + arguments;

					LoginCommandSender loginCommandSender = new LoginCommandSender(
							message,
							networkInput,
							networkOutput,
							loginResponseJTextArea,
							mainJFrame,
							userPropsPanel,
							buttonAndResponseCodePanel,
							mainWindow,
							mainContainer,
							commandButtonsPanel,
							responseJPanel
					);
					loginCommandSender.execute();
				}
			}
		);
		loginJButton.addKeyListener(this);
		buttonAndResponseCodePanel.add( loginJButton );
		JScrollPane loginResponseJScrollPane = new JScrollPane(loginResponseJTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		buttonAndResponseCodePanel.add( loginResponseJScrollPane );

		commandButtonsPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "ACTIONS" ) );
		getMachinesJButton.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event )
				{
					responseJTextArea.setText("Fetching data...");

					CommandSender commandSender = new CommandSender(Commands.GET_MACHINES, networkInput, networkOutput, responseJTextArea);
					commandSender.execute();
				}
			}
		);
		commandButtonsPanel.add( getMachinesJButton );


		getMachineInformationInputPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "Get Machine Information" ) );
		getMachineInformationInputPanel.add( new JLabel( "Machine Unique Id" ) );
		getMachineInformationInputPanel.add( machineUniqueIdJTextField );
		commandButtonsPanel.add( getMachineInformationInputPanel );

		getMachineInformationsJButton.addActionListener( new ActionListener() {
			  public void actionPerformed( ActionEvent event )
			  {
				  String message, machineId, arguments;
				  int machineIdInteger;

				  machineId = machineUniqueIdJTextField.getText();

				  try{
					  machineIdInteger = Integer.parseInt(machineId);
				  }
				  catch (NumberFormatException e){
					  machineIdInteger = 0;
				  }

				  arguments = gson.toJson(new GetMachineInformationsRequest(machineIdInteger), GetMachineInformationsRequest.class);

				  message = Commands.GET_MACHINE_INFORMATIONS + "*" + arguments;

				  responseJTextArea.setText("Fetching data...");

				  CommandSender commandSender = new CommandSender(message, networkInput, networkOutput, responseJTextArea);
				  commandSender.execute();
			  }
		  }
		);
		commandButtonsPanel.add( getMachineInformationsJButton );
		sendJobOrderInputPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "Send Job Order" ) );
		sendJobOrderInputPanel.add( new JLabel( "Job Order Unique Id" ) );
		sendJobOrderInputPanel.add( jobOrderUniqueIdJTextField );
		sendJobOrderInputPanel.add( new JLabel( "Job Order Type" ) );
		sendJobOrderInputPanel.add( jobOrderTypeJTextField );
		sendJobOrderInputPanel.add( new JLabel( "Job Order Production Length" ) );
		sendJobOrderInputPanel.add( jobOrderProductionLengthJTextField );
		commandButtonsPanel.add( sendJobOrderInputPanel );

		sendJobOrderJButton.addActionListener( new ActionListener() {
				 public void actionPerformed( ActionEvent event )
				 {
					 String message, jobId, jobType, jobLength, arguments;
					 int jobIdInteger;

					 jobId = jobOrderUniqueIdJTextField.getText();
					 jobType = jobOrderTypeJTextField.getText();
					 jobLength = jobOrderProductionLengthJTextField.getText();

					 try{
						 jobIdInteger = Integer.parseInt(jobId);
					 }
					 catch (NumberFormatException e){
						 jobIdInteger = 0;
					 }

					 arguments = gson.toJson(new SendJobOrderRequest(jobIdInteger, jobType, jobLength));

					 message = Commands.SEND_JOB_ORDER  + "*" + arguments;

					 responseJTextArea.setText("Fetching data...");

					 CommandSender commandSender = new CommandSender(message, networkInput, networkOutput, responseJTextArea);
					 commandSender.execute();
				 }
			 }
		);
		commandButtonsPanel.add( sendJobOrderJButton );

		getPendingJobOrdersJButton.addActionListener( new ActionListener() {
			  public void actionPerformed( ActionEvent event )
			  {
				  responseJTextArea.setText("Fetching data...");

				  CommandSender commandSender = new CommandSender(Commands.GET_PENDING_JOB_ORDERS, networkInput, networkOutput, responseJTextArea);
				  commandSender.execute();
			  }
		  }
		);
		commandButtonsPanel.add( getPendingJobOrdersJButton );

		getMachineStatesJButton.addActionListener( new ActionListener() {
			  public void actionPerformed( ActionEvent event )
			  {
				  responseJTextArea.setText("Fetching data...");

				  CommandSender commandSender = new CommandSender(Commands.GET_MACHINE_STATES, networkInput, networkOutput, responseJTextArea);
				  commandSender.execute();
			  }
		  }
		);
		commandButtonsPanel.add( getMachineStatesJButton );

		getProcessingJobOrdersInMachinesStatesJButton.addActionListener( new ActionListener() {
			   public void actionPerformed( ActionEvent event )
			   {
				   responseJTextArea.setText("Fetching data...");

				   CommandSender commandSender = new CommandSender(Commands.GET_PROCESSING_JOB_ORDERS, networkInput, networkOutput, responseJTextArea);
				   commandSender.execute();
			   }
		   }
		);
		commandButtonsPanel.add( getProcessingJobOrdersInMachinesStatesJButton );

		responseJPanel.setBorder( new TitledBorder(new LineBorder( Color.BLACK ), "RESPONSE PANEL" ) );
		JScrollPane responseJScrollPane = new JScrollPane(responseJTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		responseJPanel.add(responseJScrollPane);

		add( userPropsPanel );
		add( buttonAndResponseCodePanel );
		setSize( 400, 300 );
		setVisible( true );
		addWindowListener(this);
	}
	
	public static void main(String[] args)
	{
		try {
			host = InetAddress.getLocalHost();

			try{
				socket = new Socket(host,PORT);

				networkInput = new Scanner(socket.getInputStream());
				networkOutput = new PrintWriter(socket.getOutputStream(),true);

			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		catch(UnknownHostException uhEx) {
			System.out.println("\nHost ID not found!\n");
			System.exit(1);
		}
		UserClient userClient = new UserClient(networkInput, networkOutput, gson);
		//userClient.setDefaultCloseOperation( EXIT_ON_CLOSE );
	}

	private static String sendCommand(String message, Scanner networkInput, PrintWriter networkOutput)
	{
		String responseCode;
		networkOutput.println(message);
		responseCode = networkInput.nextLine();
		return  responseCode;
	}

	@Override
	public void windowOpened(WindowEvent e) { }

	@Override
	public void windowClosing(WindowEvent e) {
		Thread newThread = new Thread(() -> {
			String message, arguments, response;

			arguments = gson.toJson(new LoginRequest(UserName, UserPassword));

			message = Commands.LOGOFF + "*" + arguments;

			response = sendCommand(message, networkInput, networkOutput);

			if(response.equals("101")){
				System.out.println("Response: " + response);
				sendCommand(Commands.EXIT, networkInput, networkOutput);
				System.exit(1);
			}
			else {
				System.out.println("Response: " + response);
				sendCommand(Commands.EXIT, networkInput, networkOutput);
				System.exit(0);
			}
		});
		newThread.start();
	}

	@Override
	public void windowClosed(WindowEvent e) { }
	@Override
	public void windowIconified(WindowEvent e) { }
	@Override
	public void windowDeiconified(WindowEvent e) { }
	@Override
	public void windowActivated(WindowEvent e) { }
	@Override
	public void windowDeactivated(WindowEvent e) { }

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == 10){
			loginJButton.doClick();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) { }
}

