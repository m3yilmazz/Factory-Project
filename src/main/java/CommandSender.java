import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.io.PrintWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class CommandSender extends SwingWorker<String, Object> {
    private final String message;
    Scanner networkInput;
    PrintWriter networkOutput;
    JTextArea responseJTextArea;
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();

    CommandSender(String message, Scanner networkInput, PrintWriter networkOutput, JTextArea responseJTextArea){
        this.message = message;
        this.networkInput = networkInput;
        this.networkOutput = networkOutput;
        this.responseJTextArea = responseJTextArea;
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
            command = message.split("\\u002A")[0];
            switch (command){
                case Commands.ADD_MACHINE, Commands.SEND_JOB_ORDER -> {
                    Response<String> responseObject = gson.fromJson(response, Response.class);
                    responseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
                }

                case Commands.GET_MACHINES -> {
                    Type type = new TypeToken<Response<GetMachinesResponse>>(){}.getType();
                    Response<GetMachinesResponse> responseObject = gson.fromJson(response, type);
                    if(responseObject.ResponseCode == 150){
                        buffer = new StringBuffer();
                        buffer.append("CNC MACHINES\n\n");
                        for (Machine machine : responseObject.ResponseBody.CNCMachines)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        buffer.append("DÖKÜM MACHINES\n\n");
                        for (Machine machine : responseObject.ResponseBody.DOKUMMachines)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        buffer.append("KILIF MACHINES\n\n");
                        for (Machine machine : responseObject.ResponseBody.KILIFMachines)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        buffer.append("KAPLAMA MACHINES\n\n");
                        for (Machine machine : responseObject.ResponseBody.KAPLAMAMachines)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        responseJTextArea.setText(String.valueOf(buffer));
                    }
                    else {
                        responseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
                    }
                }

                case Commands.GET_MACHINE_INFORMATIONS -> {
                    Type type = new TypeToken<Response<GetMachineInformationResponse>>() {}.getType();
                    Response<GetMachineInformationResponse> responseObject = gson.fromJson(response, type);
                    if (responseObject.ResponseCode == 120) {
                        buffer = new StringBuffer();
                        buffer.append("MACHINE INFORMATIONS\n\n");
                        buffer.append(machineInformationFormatter(
                                responseObject.ResponseBody.Machine.MachineUniqueId,
                                responseObject.ResponseBody.Machine.MachineName,
                                responseObject.ResponseBody.Machine.MachineType,
                                responseObject.ResponseBody.Machine.MachineProductionSpeed,
                                responseObject.ResponseBody.Machine.MachineState
                        ));
                        buffer.append("COMPLETED JOBS\n\n");
                        for (Job job : responseObject.ResponseBody.CompletedJobs)
                            buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                        responseJTextArea.setText(String.valueOf(buffer));
                    } else {
                        responseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
                    }
                }

                case Commands.GET_PENDING_JOB_ORDERS -> {
                    Type type = new TypeToken<Response<GetPendingJobOrdersResponse>>() {}.getType();
                    Response<GetPendingJobOrdersResponse> responseObject = gson.fromJson(response, type);
                    if (responseObject.ResponseCode == 160) {
                        buffer = new StringBuffer();
                        buffer.append("PENDING JOB ORDERS\n\n");
                        buffer.append("CNC JOB ORDERS\n\n");
                        for (Job job : responseObject.ResponseBody.CNCJobOrders)
                            buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                        buffer.append("DÖKÜM JOB ORDERS\n\n");
                        for (Job job : responseObject.ResponseBody.DOKUMJobOrders)
                            buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                        buffer.append("KILIF JOB ORDERS\n\n");
                        for (Job job : responseObject.ResponseBody.KILIFJobOrders)
                            buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                        buffer.append("KAPLAMA JOB ORDERS\n\n");
                        for (Job job : responseObject.ResponseBody.KAPLAMAJobOrders)
                            buffer.append(jobInformationFormatter(job.JobUniqueId, job.JobType, job.JobLength, job.JobState));
                        responseJTextArea.setText(String.valueOf(buffer));
                    } else {
                        responseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
                    }
                }

                case Commands.GET_MACHINE_STATES -> {
                    Type type = new TypeToken<Response<GetMachineStatesResponse>>() {}.getType();
                    Response<GetMachineStatesResponse> responseObject = gson.fromJson(response, type);
                    if(responseObject.ResponseCode == 170){
                        buffer = new StringBuffer();
                        for (Machine machine : responseObject.ResponseBody.Machines)
                            buffer.append(machineInformationFormatter(machine.MachineUniqueId, machine.MachineName, machine.MachineType, machine.MachineProductionSpeed, machine.MachineState));
                        responseJTextArea.setText(String.valueOf(buffer));
                    } else {
                        responseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
                    }
                }

                case Commands.GET_PROCESSING_JOB_ORDERS -> {
                    Type type = new TypeToken<Response<GetProcessingJobResponse>>() {}.getType();
                    Response<GetProcessingJobResponse> responseObject = gson.fromJson(response, type);

                    if(responseObject.ResponseCode == 140){
                        buffer = new StringBuffer();
                        for (int index = 0; index < responseObject.ResponseBody.Machines.size(); ++index) {
                            buffer.append("MACHINE\n\n");
                            buffer.append(machineInformationFormatter(
                                    responseObject.ResponseBody.Machines.get(index).MachineUniqueId,
                                    responseObject.ResponseBody.Machines.get(index).MachineName,
                                    responseObject.ResponseBody.Machines.get(index).MachineType,
                                    responseObject.ResponseBody.Machines.get(index).MachineProductionSpeed,
                                    responseObject.ResponseBody.Machines.get(index).MachineState
                            ));

                            buffer.append("PROCESSING JOB IN THE MACHINE\n\n");
                            buffer.append(jobInformationFormatter(
                                    responseObject.ResponseBody.Jobs.get(index).JobUniqueId,
                                    responseObject.ResponseBody.Jobs.get(index).JobType,
                                    responseObject.ResponseBody.Jobs.get(index).JobLength,
                                    responseObject.ResponseBody.Jobs.get(index).JobState
                            ));
                        }
                        responseJTextArea.setText(String.valueOf(buffer));
                    } else {
                        responseJTextArea.setText("Response Code:\t" + responseObject.ResponseCode + "\n" + "Response Message:\t" + responseObject.ResponseMessage);
                    }
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

    String machineInformationFormatter(int machineId, String machineName, String machineType, String machineSpeed, String machineState){
        StringBuffer buffer = new StringBuffer();

        buffer.append(
            "Machine Unique Id:\t" + machineId + "\n" +
            "Machine Name:\t\t" + machineName + "\n" +
            "Machine Type:\t\t" + machineType + "\n" +
            "Machine Production Speed:\t" + machineSpeed + "\n" +
            "Machine State:\t\t" + machineState + "\n\n"
        );

        return String.valueOf(buffer);
    }

    String jobInformationFormatter(int jobId, String jobType, String jobLength, String jobState){
        StringBuffer buffer = new StringBuffer();

        buffer.append(
            "Job Unique Id:\t" + jobId + "\n" +
            "Job Type:\t" + jobType + "\n" +
            "Job Length:\t" + jobLength + "\n" +
            "Job State:\t" + jobState + "\n\n"
        );

        return String.valueOf(buffer);
    }
}
