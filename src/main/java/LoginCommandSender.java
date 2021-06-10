import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.lang.reflect.Type;
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
    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private final Gson gson = gsonBuilder.create();

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
            Response<String> responseObject = gson.fromJson(response, Response.class);
            if(responseObject.ResponseCode == 100){
                jFrame.remove(userPropsPanel);
                jFrame.remove(buttonAndResponseCodePanel);

                jFrame.setLayout( new GridLayout( 1, 2, 10, 10 ) );
                window.setSize( 1000, 900 );

                container.add(commandButtonsPanel);
                container.add(responseJPanel);
            }
            else {
                loginResponseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
            }
        }
        catch ( InterruptedException ex ) {
            loginResponseJTextArea.setText("Interrupted while waiting for results.");
        }
        catch ( ExecutionException ex ) {
            loginResponseJTextArea.setText("Error encountered while performing request.");
        }
    }
}
