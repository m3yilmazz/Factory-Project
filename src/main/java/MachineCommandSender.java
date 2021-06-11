import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class MachineCommandSender extends SwingWorker<String, Object> {
    private final String message;
    Scanner networkInput;
    PrintWriter networkOutput;
    JTextArea responseJTextArea;
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();
    JTextField machineUniqueIdJTextField;
    JTextField machineNameJTextField;
    JTextField machineTypeJTextField;
    JTextField machineProductionSpeedJTextField;
    JPanel buttonAndResponseCodePanel;
    JButton addMachineJButton;
    JButton removeMachineJButton;
    Window window;
    JFrame jFrame;

    MachineCommandSender(
            String message,
            Scanner networkInput,
            PrintWriter networkOutput,
            JTextArea responseJTextArea,
            JTextField machineUniqueIdJTextField,
            JTextField machineNameJTextField,
            JTextField machineTypeJTextField,
            JTextField machineProductionSpeedJTextField,
            JPanel buttonAndResponseCodePanel,
            JButton addMachineJButton,
            JButton removeMachineJButton,
            Window window,
            JFrame jFrame)
    {
        this.message = message;
        this.networkInput = networkInput;
        this.networkOutput = networkOutput;
        this.responseJTextArea = responseJTextArea;
        this.machineUniqueIdJTextField = machineUniqueIdJTextField;
        this.machineNameJTextField = machineNameJTextField;
        this.machineTypeJTextField = machineTypeJTextField;
        this.machineProductionSpeedJTextField = machineProductionSpeedJTextField;
        this.buttonAndResponseCodePanel = buttonAndResponseCodePanel;
        this.addMachineJButton = addMachineJButton;
        this.removeMachineJButton = removeMachineJButton;
        this.window = window;
        this.jFrame = jFrame;
    }

    @Override
    protected String doInBackground() {
        String response;
        networkOutput.println(message);
        response = networkInput.nextLine();
        return  response;
    }

    protected void done()
    {
        String response, command;
        StringBuffer buffer = new StringBuffer();

        try
        {
            response = get();

            Request<String> requestObject = gson.fromJson(message, Request.class);
            command = requestObject.RequestCommand;

            switch (command) {
                case Commands.ADD_MACHINE, Commands.REMOVE_MACHINE -> {
                    Response<String> responseObject = gson.fromJson(response, Response.class);

                    if (responseObject.ResponseCode == 110) {
                        machineUniqueIdJTextField.setEditable(false);
                        machineNameJTextField.setEditable(false);
                        machineTypeJTextField.setEditable(false);
                        machineProductionSpeedJTextField.setEditable(false);
                        responseJTextArea.setEditable(false);

                        buttonAndResponseCodePanel.remove( addMachineJButton );
                        buttonAndResponseCodePanel.add( removeMachineJButton );

                        window.setSize( 500, 500 );
                        jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    }

                    if(responseObject.ResponseCode == 180){
                        jFrame.addWindowListener(new WindowAdapter() {
                            public void windowClosing(WindowEvent ev) {
                                System.exit(1);
                            }
                        });
                    }
                    else if(responseObject.ResponseCode == 280 || responseObject.ResponseCode == 281){
                        jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    }

                    responseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
                }
            }
        }
        catch ( InterruptedException ex ) {
            responseJTextArea.setText("Interrupted while waiting for results.");
        }
        catch ( ExecutionException ex ) {
            responseJTextArea.setText("Error encountered while performing request.");
        }
    }
}
