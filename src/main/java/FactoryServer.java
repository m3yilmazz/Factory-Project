import com.google.gson.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FactoryServer
{
	private static ServerSocket serverSocket;
	private static final int PORT = 9999;
	private static ArrayList<Machine> machines = new ArrayList<Machine>();
	private static ArrayList<Job> jobs = new ArrayList<Job>();
	private static ArrayList<User> users = new ArrayList<User>();
	private static ExecutorService executorService = Executors.newCachedThreadPool();
	private static CustomMap jobSchedulingSharedLocation = new SynchedCustomMap();

	static Map<String, String> USERNAME_PASSWORD_MAP = new HashMap<String, String>();

	private static GsonBuilder gsonBuilder = new GsonBuilder();
	private static Gson gson = gsonBuilder.create();
	private static Gson prettyGson = gsonBuilder.setPrettyPrinting().create();

	public static void main(String[] args) throws IOException
	{
		users.add(new User("admin", "admin"));
		users.add(new User("admin1", "admin1"));
		users.add(new User("admin2", "admin2"));

		USERNAME_PASSWORD_MAP.put("admin", "admin");
		USERNAME_PASSWORD_MAP.put("admin1", "admin1");
		USERNAME_PASSWORD_MAP.put("admin2", "admin2");

		JobTypes.METRICS.put(JobTypes.CNC, JobTypes.CNC_METRIC);
		JobTypes.METRICS.put(JobTypes.DOKUM, JobTypes.DOKUM_METRIC);
		JobTypes.METRICS.put(JobTypes.KILIF, JobTypes.KILIF_METRIC);
		JobTypes.METRICS.put(JobTypes.KAPLAMA, JobTypes.KAPLAMA_METRIC);

		try
		{
			serverSocket = new ServerSocket(PORT);
		}
		catch (IOException ioEx)
		{
			System.out.println("\nUnable to set up port!");
			System.exit(1);
		}

		do
		{
			Socket client = serverSocket.accept();
			System.out.println("\nNew client accepted.\n");

			ClientHandler handler = new ClientHandler(client, machines, jobs, users, executorService, jobSchedulingSharedLocation, gson, prettyGson);

			handler.start();
		}while (true);
	}
}

class ClientHandler extends Thread
{
	private Socket client;
	private Scanner input;
	private PrintWriter output;
	private ArrayList<Machine> machines;
	private ArrayList<Job> jobs;
	private ArrayList<User> users;
	private ExecutorService executorService;
	private CustomMap jobSchedulingSharedLocation;
	private Gson gson;
	private Gson prettyGson;

	public ClientHandler(Socket socket, ArrayList<Machine> machines, ArrayList<Job> jobs, ArrayList<User> users, ExecutorService executorService, CustomMap jobSchedulingSharedLocation, Gson gson, Gson prettyGson)
	{
		client = socket;
		this.machines = machines;
		this.jobs = jobs;
		this.users = users;
		this.executorService = executorService;
		this.jobSchedulingSharedLocation = jobSchedulingSharedLocation;
		this.gson = gson;
		this.prettyGson = prettyGson;

		try
		{
			input = new Scanner(client.getInputStream());
			output = new PrintWriter(client.getOutputStream(),true);
		}
		catch(IOException ioEx)
		{
			ioEx.printStackTrace();
		}
	}

	public void run() {
		String received, command, argumentsJsonString;

		do {
			try{
				received = input.nextLine();
			}
			catch (NoSuchElementException e){
				break;
			}

			String[] splittedReceived = received.split("\\u002A");

			command = splittedReceived[0];

			String metric;
			User receivedUser;

			switch (command) {
				case Commands.LOGIN:
					try{
						argumentsJsonString = splittedReceived[1];

						try{
							receivedUser = gson.fromJson(argumentsJsonString, User.class);
							receivedUser.UserStatus = User.OFFLINE;
						}
						catch (JsonParseException e){
							output.println(410);
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					if(receivedUser.UserName.isEmpty() || receivedUser.UserPassword.isEmpty()){
						output.println(410);
						break;
					}

					boolean isUserNameOrPasswordIsWrong = true;
					for(User user: users){
						if(receivedUser.UserName.equals(user.UserName)){
							if(user.UserStatus.equals(User.ONLINE)){
								output.println(201);
								break;
							}
							else {
								if(receivedUser.UserPassword.equals(user.UserPassword)){
									user.UserStatus = User.ONLINE;
									output.println(100);
									isUserNameOrPasswordIsWrong = false;
									System.out.println(user.UserName + " has logged in.");
									break;
								}
								else {
									output.println(200);
									break;
								}
							}
						}
					}
					if(isUserNameOrPasswordIsWrong){
						output.println(200);
						break;
					}
					break;

				case Commands.LOGOFF:
					try{
						argumentsJsonString = splittedReceived[1];

						try{
							receivedUser = gson.fromJson(argumentsJsonString, User.class);
						}
						catch (JsonParseException e){
							output.println(410);
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					try{
						if(receivedUser.UserName.isEmpty() || receivedUser.UserPassword.isEmpty()){
							output.println(410);
							break;
						}
					}
					catch (NullPointerException e){
						output.println(410);
						break;
					}

					for(User user: users){
						if(receivedUser.UserName.equals(user.UserName)){
							if(receivedUser.UserPassword.equals(user.UserPassword)){
								if(user.UserStatus.equals(User.ONLINE)){
									user.UserStatus = User.OFFLINE;
									output.println(101);
									System.out.println(user.UserName + " has logged off.");
									break;
								}
							}
						}
					}

					output.println(202);
					break;

				case Commands.ADD_MACHINE:
					Machine receivedMachine;
					boolean machineIdIsAlreadyExist = false;

					try{
						argumentsJsonString = splittedReceived[1];

						try{
							receivedMachine = gson.fromJson(argumentsJsonString, Machine.class);
							receivedMachine.MachineState = Machine.MACHINE_STATE_EMPTY;
						}
						catch (JsonParseException e){
							output.println(410);
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					try{
						if(receivedMachine.MachineUniqueId == 0 ||
							receivedMachine.MachineName.isEmpty() ||
							receivedMachine.MachineType.isEmpty() ||
							receivedMachine.MachineProductionSpeed.isEmpty())
						{
							output.println(410);
							break;
						}
					}
					catch (NullPointerException e){
						output.println(410);
						break;
					}

					for(Machine machine: machines){
						if(machine.MachineUniqueId == receivedMachine.MachineUniqueId){
							machineIdIsAlreadyExist = true;
							break;
						}
					}

					if(machineIdIsAlreadyExist) {
						output.println(210);
						break;
					}
					else {
						if( !(receivedMachine.MachineType.equals(JobTypes.CNC) ||
							receivedMachine.MachineType.equals(JobTypes.DOKUM) ||
							receivedMachine.MachineType.equals(JobTypes.KILIF) ||
							receivedMachine.MachineType.equals(JobTypes.KAPLAMA)))
						{
							output.println(211);
							break;
						}

						metric = receivedMachine.MachineProductionSpeed.split(" ")[1];
						if(metric.equals(JobTypes.METRICS.get(receivedMachine.MachineType))){
							machines.add(receivedMachine);

							output.println(110);

							System.out.println(
								"Added Machine: " +
								"Machine Unique Id: " + receivedMachine.MachineUniqueId + ", " +
								"Machine Name: " + receivedMachine.MachineName + ", " +
								"Machine Type: " + receivedMachine.MachineType + ", " +
								"Machine Production Speed: " + receivedMachine.MachineProductionSpeed + ", " +
								"Machine State: " + receivedMachine.MachineState
							);

							assignJobToMachine();
						}
						else {
							output.println(212);
							break;
						}
					}
					break;

				case Commands.GET_MACHINES:
					GetMachinesResponse getMachinesResponse = new GetMachinesResponse();
					if(!machines.isEmpty()){
						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.CNC)) {
								getMachinesResponse.CNC_MACHINES.add(machine);
							}
						}

						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.DOKUM)) {
								getMachinesResponse.DOKUM_MACHINES.add(machine);
							}
						}

						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.KILIF)) {
								getMachinesResponse.KILIF_MACHINES.add(machine);
							}
						}

						for(Machine machine: machines){
							if (machine.MachineType.equals(JobTypes.KAPLAMA)) {
								getMachinesResponse.KAPLAMA_MACHINES.add(machine);
							}
						}
						output.println(gson.toJson(getMachinesResponse));
					}
					else {
						output.println(220);
					}
					break;

				case Commands.GET_MACHINE_INFORMATIONS:
					GetMachineInformationsRequest receivedMachineId;
					GetMachineInformationResponse getMachineInformationResponse = new GetMachineInformationResponse();
					try{
						argumentsJsonString = splittedReceived[1];

						try{
							receivedMachineId = gson.fromJson(argumentsJsonString, GetMachineInformationsRequest.class);
						}
						catch (JsonParseException e){
							output.println(410);
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					for(Machine machine: machines){
						if(machine.MachineUniqueId == receivedMachineId.MachineId){
							getMachineInformationResponse.MACHINE = machine;
							break;
						}
					}
					if(getMachineInformationResponse.MACHINE == null){
						output.println(230);
						break;
					}
					else {
						try {
							List<Integer> jobList = jobSchedulingSharedLocation.get(receivedMachineId.MachineId);
							if(jobList == null){
								output.println(gson.toJson(getMachineInformationResponse));
								break;
							}
							for(Job job: jobs){
								for (Integer jobId: jobList){
									if(job.JobUniqueId == jobId){
										if(job.JobState.equals(Job.JOB_STATE_DONE)){
											getMachineInformationResponse.COMPLETED_JOBS.add(job);
										}
									}
								}
							}
						}
						catch (InterruptedException e) { }

						output.println(gson.toJson(getMachineInformationResponse));
					}
					break;

				case Commands.SEND_JOB_ORDER:
					Job receivedJob;
					boolean isJobIdAlreadyExist = false;

					try{
						argumentsJsonString = splittedReceived[1];

						try{
							receivedJob = gson.fromJson(argumentsJsonString, Job.class);
							receivedJob.JobState = Job.JOB_STATE_PENDING;
						}
						catch (JsonParseException e){
							output.println(410);
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e){
						output.println(410);
						break;
					}

					try{
						if( receivedJob.JobUniqueId == 0 ||
							receivedJob.JobType.isEmpty() ||
							receivedJob.JobLength.isEmpty())
						{
							output.println(410);
							break;
						}
					}
					catch (NullPointerException e){
						output.println(410);
						break;
					}

					for(Job job: jobs){
						if(job.JobUniqueId == receivedJob.JobUniqueId){
							isJobIdAlreadyExist = true;
							break;
						}
					}

					if(isJobIdAlreadyExist) {
						output.println(240);
						break;
					}
					else {
						if( !(  receivedJob.JobType.equals(JobTypes.CNC) ||
								receivedJob.JobType.equals(JobTypes.DOKUM) ||
								receivedJob.JobType.equals(JobTypes.KILIF) ||
								receivedJob.JobType.equals(JobTypes.KAPLAMA)))
						{
							output.println(241);
							break;
						}

						metric = receivedJob.JobLength.split(" ")[1];
						if(metric.equals(JobTypes.METRICS.get(receivedJob.JobType))){
							jobs.add(receivedJob);

							output.println(140);

							System.out.println(
								"Added Job: " +
								"Job Unique Id: " + receivedJob.JobUniqueId + ", " +
								"Job Type: " + receivedJob.JobType + ", " +
								"Job Length: " + receivedJob.JobLength + ", " +
								"Job State: " + receivedJob.JobState
							);

							assignJobToMachine();
						}
						else {
							output.println(242);
							break;
						}
					}
					break;

				case Commands.GET_PENDING_JOB_ORDERS:
					GetPendingJobOrdersResponse getPendingJobOrdersResponse = new GetPendingJobOrdersResponse();

					for(Job job: jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.CNC))
								getPendingJobOrdersResponse.CNC_JOB_ORDERS.add(job);

					for(Job job: jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if(job.JobType.equals(JobTypes.DOKUM))
								getPendingJobOrdersResponse.DOKUM_JOB_ORDERS.add(job);

					for(Job job: jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if(job.JobType.equals(JobTypes.KILIF))
								getPendingJobOrdersResponse.KILIF_JOB_ORDERS.add(job);

					for(Job job: jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if(job.JobType.equals(JobTypes.KAPLAMA))
								getPendingJobOrdersResponse.KAPLAMA_JOB_ORDERS.add(job);

					output.println(gson.toJson(getPendingJobOrdersResponse));
					break;

				case Commands.GET_MACHINE_STATES:
					GetMachineStatesResponse getMachineStatesResponse = new GetMachineStatesResponse();

					if(!machines.isEmpty()){
						for(Machine machine: machines)
							getMachineStatesResponse.MACHINES.add(machine);

						output.println(gson.toJson(getMachineStatesResponse));
					}
					else {
						output.println(220);
						break;
					}
					break;

				case Commands.GET_PROCESSING_JOB_ORDERS:
					GetProcessingJobResponse getProcessingJobResponse = new GetProcessingJobResponse();
					if(!machines.isEmpty()){
						for(Machine machine: machines){
							if(machine.MachineState.equals(Machine.MACHINE_STATE_BUSY)){
								try {
									List<Integer> jobList = jobSchedulingSharedLocation.get(machine.MachineUniqueId);
									if(jobList == null){
										getProcessingJobResponse.MACHINES.add(machine);
										getProcessingJobResponse.JOBS.add(null);
										continue;
									}
									for(Job job: jobs){
										for (Integer jobId1: jobList){
											if(job.JobUniqueId == jobId1){
												if(job.JobState.equals(Job.JOB_STATE_PROCESSING)){
													getProcessingJobResponse.MACHINES.add(machine);
													getProcessingJobResponse.JOBS.add(job);
												}
											}
										}
									}
								}
								catch (InterruptedException e) { }
							}
						}

						if(getProcessingJobResponse.MACHINES.size() == 0){
							output.println(250);
							break;
						}
						else {
							output.println(gson.toJson(getProcessingJobResponse));
						}
					}
					else {
						output.println(220);
						break;
					}

					break;

				default:
					output.println(400);
					break;
			}
		} while (!received.equals("EXIT"));

		try {
			if (client != null) {
				System.out.println("Closing down connection...");
				client.close();
			}
		} catch (IOException ioEx) {
			System.out.println("Unable to disconnect!");
		}
	}

	void assignJobToMachine(){
		for (Job job: jobs){
			if(job.JobState.equals("PENDING")){
				for(Machine machine: machines){
					if(machine.MachineState.equals("EMPTY")){
						if(machine.MachineType.equals(job.JobType)){
							int productionSpeed = Integer.parseInt(machine.MachineProductionSpeed.split(" ")[0]);
							int productLength = Integer.parseInt(job.JobLength.split(" ")[0]);
							//long delay = productLength / productionSpeed * 1000L * 60L;
							long delay = productLength / productionSpeed * 1000L;
							machine.MachineState = "BUSY";
							job.JobState = "PROCESSING";
							executorService.execute(new JobAssignmentProducer(jobSchedulingSharedLocation, machine.MachineUniqueId, job.JobUniqueId));
							System.out.println("Job assigned.");
							completeJob(machine.MachineUniqueId, job.JobUniqueId, delay);
							break;
						}
					}
				}
			}
		}
	}

	void completeJob(int machineId, int jobId, long delay){
		TimerTask task = new TimerTask() {
			public void run() {
				for(Machine machine: machines){
					if(machineId == machine.MachineUniqueId){
						machine.MachineState = "EMPTY";
						System.out.println("Machine State: " + machine.MachineState + " " + "Machine Id: " + machine.MachineUniqueId);
					}
				}

				for(Job job: jobs){
					if(jobId == job.JobUniqueId){
						job.JobState = "DONE";
						System.out.println("Job State: " + job.JobState + " " + "Job Id: " + job.JobUniqueId);
					}
				}
				assignJobToMachine();
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, delay);
	}
}

