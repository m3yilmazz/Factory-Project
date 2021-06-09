import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class LoginCommandSender extends SwingWorker<String, Object> {
    private final String message;
    private final Scanner networkInput;
    private final PrintWriter networkOutput;
    private final JTextArea loginResponseJTextArea;
    private final JFrame jFrame;
    private final JPanel userPropsPanel;
    private final JPanel buttonAndResponseCodePanel;
    private final Window window;
    private final Container container;
    private final JPanel commandButtonsPanel;
    private final JPanel responseJPanel;

    LoginCommandSender(
            String message,
            Scanner networkInput,
            PrintWriter networkOutput,
            JTextArea loginResponseJTextArea,
            JFrame jFrame,
            JPanel userPropsPanel,
            JPanel buttonAndResponseCodePanel,
            Window window,
            Container container,
            JPanel commandButtonsPanel,
            JPanel responseJPanel)
    {
        this.message = message;
        this.networkInput = networkInput;
        this.networkOutput = networkOutput;
        this.loginResponseJTextArea = loginResponseJTextArea;
        this.jFrame = jFrame;
        this.userPropsPanel = userPropsPanel;
        this.buttonAndResponseCodePanel = buttonAndResponseCodePanel;
        this.window = window;
        this.container = container;
        this.commandButtonsPanel = commandButtonsPanel;
        this.responseJPanel = responseJPanel;
    }

    @Override
    protected String doInBackground() throws Exception {
        String response;
        networkOutput.println(message);
        response = networkInput.nextLine();
        return  response;
    }

    protected void done()
    {
        String response;
        try
        {
            response = get();
            if(response.equals("410")){
                loginResponseJTextArea.setText("Response Code: 410 \nResponse Message: There is one or more missing argument in the command.");
            }
            else if(response.equals("200")){
                loginResponseJTextArea.setText("Response Code: 200 \nResponse Message: The username or password is wrong.");
            }
            else if(response.equals("100")){
                jFrame.remove(userPropsPanel);
                jFrame.remove(buttonAndResponseCodePanel);

                jFrame.setLayout( new GridLayout( 1, 2, 10, 10 ) );
                window.setSize( 1000, 900 );

                container.add(commandButtonsPanel);
                container.add(responseJPanel);
            }
        }
        catch ( InterruptedException ex )
        {
            loginResponseJTextArea.setText("Interrupted while waiting for results.");
        }
        catch ( ExecutionException ex )
        {
            loginResponseJTextArea.setText("Error encountered while performing request.");
        }
    }
}
