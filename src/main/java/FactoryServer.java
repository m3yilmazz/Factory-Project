import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FactoryServer
{
	private static ServerSocket serverSocket;
	private static final int PORT = 9999;
	private static CustomArrayList<Machine> machinesSharedLocation = new SyncedCustomArrayList<Machine>();
	private static CustomArrayList<Job> jobsSharedLocation = new SyncedCustomArrayList<Job>();
	private static ArrayList<User> users = new ArrayList<User>();
	private static final ExecutorService executorService = Executors.newCachedThreadPool();
	private static CustomMap jobSchedulingSharedLocation = new SyncedCustomMap();

	static Map<String, String> USERNAME_PASSWORD_MAP = new HashMap<String, String>();

	private static final GsonBuilder gsonBuilder = new GsonBuilder();
	private static final Gson gson = gsonBuilder.serializeNulls().create();

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

		try {
			serverSocket = new ServerSocket(PORT);
		}
		catch (IOException ioEx) {
			System.out.println("\nUnable to set up port!");
			System.exit(1);
		}

		do {
			Socket client = serverSocket.accept();
			System.out.println("\nNew client accepted.\n");

			ClientHandler handler = new ClientHandler(client, machinesSharedLocation, jobsSharedLocation, users, executorService, jobSchedulingSharedLocation, gson);

			handler.start();
		}while (true);
	}
}

class ClientHandler extends Thread
{
	private final Socket client;
	private Scanner input;
	private PrintWriter output;
	private CustomArrayList<Machine> machinesSharedLocation;
	private CustomArrayList<Job> jobsSharedLocation;
	private ArrayList<User> users;
	private final ExecutorService executorService;
	private CustomMap jobSchedulingSharedLocation;
	private final Gson gson;

	private Future<ArrayList<Integer>> jobsArrayListFeature;
	private ArrayList<Integer> jobsArrayList = new ArrayList<Integer>();

	private Future<ArrayList<Job>> jobsFuture;
	private ArrayList<Job> jobs = new ArrayList<Job>();

	private Future<ArrayList<Machine>> machinesFuture;
	private ArrayList<Machine> machines = new ArrayList<Machine>();

	public ClientHandler(Socket socket, CustomArrayList<Machine> machinesSharedLocation, CustomArrayList<Job> jobsSharedLocation, ArrayList<User> users, ExecutorService executorService, CustomMap jobSchedulingSharedLocation, Gson gson)
	{
		this.client = socket;
		this.machinesSharedLocation = machinesSharedLocation;
		this.jobsSharedLocation = jobsSharedLocation;
		this.users = users;
		this.executorService = executorService;
		this.jobSchedulingSharedLocation = jobSchedulingSharedLocation;
		this.gson = gson;

		try {
			input = new Scanner(client.getInputStream());
			output = new PrintWriter(client.getOutputStream(),true);
		}
		catch(IOException ioEx) {
			ioEx.printStackTrace();
		}
	}

	@Override
	public void run() {
		String received, command, argumentsJsonString;

		do {
			try{
				received = input.nextLine();
			}
			catch (NoSuchElementException e){
				break;
			}

			String[] splitReceived = received.split("\\u002A");

			//command = splitReceived[0];
			Request<String> requestJson = gson.fromJson(received, Request.class);
			command = requestJson.RequestCommand;

			String metric;
			User receivedUser = new User(null, null);

			switch (command) {
				case Commands.LOGIN -> {
					try {
						Type type = new TypeToken<Request<LoginRequest>>() {}.getType();
						Request<LoginRequest> requestObject = gson.fromJson(received, type);

						receivedUser.UserName = requestObject.RequestBody.UserName;
						receivedUser.UserPassword = requestObject.RequestBody.UserPassword;
						receivedUser.UserStatus = User.OFFLINE;
					}
					catch (JsonSyntaxException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					if (receivedUser.UserName.isEmpty() || receivedUser.UserPassword.isEmpty()) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					boolean isUserNameOrPasswordIsWrong = true;

					for (User user : users) {
						if (receivedUser.UserName.equals(user.UserName)) {
							if (user.UserStatus.equals(User.ONLINE)) {
								output.println(gson.toJson(ErrorResponses.RESPONSE_201));
								break;
							}
							else {
								if (receivedUser.UserPassword.equals(user.UserPassword)) {
									user.UserStatus = User.ONLINE;
									output.println(gson.toJson(SuccessfulResponses.RESPONSE_100));
									isUserNameOrPasswordIsWrong = false;
									System.out.println(user.UserName + " has logged in.");
									break;
								}
								else {
									output.println(gson.toJson(ErrorResponses.RESPONSE_200));
									break;
								}
							}
						}
					}

					if (isUserNameOrPasswordIsWrong) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_200));
					}
				}

				case Commands.LOGOFF -> {
					try {
						Type type = new TypeToken<Request<LoginRequest>>() {}.getType();
						Request<LoginRequest> requestObject = gson.fromJson(received, type);

						receivedUser.UserName = requestObject.RequestBody.UserName;
						receivedUser.UserPassword = requestObject.RequestBody.UserPassword;
					}
					catch (JsonSyntaxException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					try {
						if (receivedUser.UserName.isEmpty() || receivedUser.UserPassword.isEmpty()) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (NullPointerException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					for (User user : users) {
						if (receivedUser.UserName.equals(user.UserName)) {
							if (receivedUser.UserPassword.equals(user.UserPassword)) {
								if (user.UserStatus.equals(User.ONLINE)) {
									user.UserStatus = User.OFFLINE;
									output.println(gson.toJson(SuccessfulResponses.RESPONSE_101));
									System.out.println(user.UserName + " has logged off.");
									break;
								}
							}
						}
					}
					output.println(gson.toJson(ErrorResponses.RESPONSE_202));
				}

				case Commands.ADD_MACHINE -> {
					Machine receivedMachine = new Machine(0, null, null, null);
					boolean machineIdIsAlreadyExist = false;

					try {
						Type type = new TypeToken<Request<AddMachineRequest>>() {}.getType();
						Request<AddMachineRequest> requestObject = gson.fromJson(received, type);

						receivedMachine.MachineUniqueId = requestObject.RequestBody.MachineUniqueId;
						receivedMachine.MachineName = requestObject.RequestBody.MachineName;
						receivedMachine.MachineType = requestObject.RequestBody.MachineType;
						receivedMachine.MachineProductionSpeed = requestObject.RequestBody.MachineProductionSpeed;
						receivedMachine.MachineState = Machine.MACHINE_STATE_EMPTY;
					}
					catch (JsonSyntaxException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					try {
						if (receivedMachine.MachineUniqueId == 0 ||
							receivedMachine.MachineName.isEmpty() ||
							receivedMachine.MachineType.isEmpty() ||
							receivedMachine.MachineProductionSpeed.isEmpty()) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (NullPointerException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					for (Machine machine : machines) {
						if (machine.MachineUniqueId == receivedMachine.MachineUniqueId) {
							machineIdIsAlreadyExist = true;
							break;
						}
					}

					if (machineIdIsAlreadyExist) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_210));
					}
					else {
						if (!(receivedMachine.MachineType.equals(JobTypes.CNC) ||
								receivedMachine.MachineType.equals(JobTypes.DOKUM) ||
								receivedMachine.MachineType.equals(JobTypes.KILIF) ||
								receivedMachine.MachineType.equals(JobTypes.KAPLAMA))) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_211));
							break;
						}

						try{
							Integer.parseInt(receivedMachine.MachineProductionSpeed.split(" ")[0]);
						}
						catch (NumberFormatException e){
							output.println(gson.toJson(ErrorResponses.RESPONSE_213));
							break;
						}

						try {
							metric = receivedMachine.MachineProductionSpeed.split(" ")[1];
							if (metric.equals(JobTypes.METRICS.get(receivedMachine.MachineType))) {
								executorService.execute(new ArrayListProducer<Machine>(machinesSharedLocation, receivedMachine));

								output.println(gson.toJson(SuccessfulResponses.RESPONSE_110));

								System.out.println(
									"Added Machine: " +
									"Machine Unique Id: " + receivedMachine.MachineUniqueId + ", " +
									"Machine Name: " + receivedMachine.MachineName + ", " +
									"Machine Type: " + receivedMachine.MachineType + ", " +
									"Machine Production Speed: " + receivedMachine.MachineProductionSpeed + ", " +
									"Machine State: " + receivedMachine.MachineState
								);

								assignJobToMachine();
							} else {
								output.println(gson.toJson(ErrorResponses.RESPONSE_212));
							}
						}
						catch (ArrayIndexOutOfBoundsException e){
							output.println(gson.toJson(ErrorResponses.RESPONSE_212));
						}
					}
				}

				case Commands.REMOVE_MACHINE -> {
					MachineIdRequest receivedMachineId = new MachineIdRequest(0);
					boolean isRemovingSuccessful = false;
					Machine removedMachine = new Machine(0, null, null, null);
					try {
						Type type = new TypeToken<Request<MachineIdRequest>>() {}.getType();
						Request<MachineIdRequest> requestObject = gson.fromJson(received, type);

						receivedMachineId.MachineId = requestObject.RequestBody.MachineId;

						if(receivedMachineId.MachineId == 0){
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (JsonSyntaxException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if(machines.size() == 0){
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
						break;
					}

					for (Machine machine : machines) {
						if (machine.MachineUniqueId == receivedMachineId.MachineId) {
							if(machine.MachineState.equals(Machine.MACHINE_STATE_BUSY)){
								output.println(gson.toJson(ErrorResponses.RESPONSE_281));
								break;
							}
							removedMachine = machine;

							executorService.execute(new JobAssignmentRemover(jobSchedulingSharedLocation, receivedMachineId.MachineId));
							machines.remove(machine);

							isRemovingSuccessful = true;
							break;
						}
					}

					if(isRemovingSuccessful){
						output.println(gson.toJson(SuccessfulResponses.RESPONSE_180));
						System.out.println(
								"Removed Machine: " +
								"Machine Unique Id: " + removedMachine.MachineUniqueId + ", " +
								"Machine Name: " + removedMachine.MachineName + ", " +
								"Machine Type: " + removedMachine.MachineType + ", " +
								"Machine Production Speed: " + removedMachine.MachineProductionSpeed + ", " +
								"Machine State: " + removedMachine.MachineState
						);
					}
					else {
						output.println(gson.toJson(ErrorResponses.RESPONSE_280));
					}
				}

				case Commands.GET_MACHINES -> {
					GetMachinesResponse getMachinesResponse = new GetMachinesResponse();
					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if (machines.size() != 0) {
						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.CNC))
								getMachinesResponse.CNCMachines.add(machine);

						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.DOKUM))
								getMachinesResponse.DOKUMMachines.add(machine);

						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.KILIF))
								getMachinesResponse.KILIFMachines.add(machine);

						for (Machine machine : machines)
							if (machine.MachineType.equals(JobTypes.KAPLAMA))
								getMachinesResponse.KAPLAMAMachines.add(machine);

						output.println(gson.toJson(new Response<GetMachinesResponse>(150, ResponseMessages.RESPONSE_MESSAGE_150, getMachinesResponse)));
					} else {
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
					}
				}

				case Commands.GET_MACHINE_INFORMATIONS -> {
					MachineIdRequest receivedMachineId = new MachineIdRequest(0);
					GetMachineInformationResponse getMachineInformationResponse = new GetMachineInformationResponse();

					try {
						Type type = new TypeToken<Request<MachineIdRequest>>() {}.getType();
						Request<MachineIdRequest> requestObject = gson.fromJson(received, type);

						receivedMachineId.MachineId = requestObject.RequestBody.MachineId;

						if(receivedMachineId.MachineId == 0){
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (JsonSyntaxException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if(machines.size() == 0){
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
						break;
					}

					for (Machine machine : machines) {
						if (machine.MachineUniqueId == receivedMachineId.MachineId) {
							getMachineInformationResponse.Machine = machine;
							break;
						}
					}

					if (getMachineInformationResponse.Machine == null) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_220));
					}
					else {
						jobsArrayListFeature = executorService.submit(new JobAssignmentConsumer(jobSchedulingSharedLocation, receivedMachineId.MachineId));

						try {
							jobsArrayList = jobsArrayListFeature.get();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}

						if (jobsArrayList == null) {
							output.println(gson.toJson(new Response<GetMachineInformationResponse>(
									120,
									ResponseMessages.RESPONSE_MESSAGE_120,
									getMachineInformationResponse))
							);
							break;
						}

						jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

						try {
							jobs = jobsFuture.get();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}

						for (Job job : jobs)
							for (Integer jobId : jobsArrayList)
								if (job.JobUniqueId == jobId)
									if (job.JobState.equals(Job.JOB_STATE_COMPLETED))
										getMachineInformationResponse.CompletedJobs.add(job);

						output.println(gson.toJson(new Response<GetMachineInformationResponse>(
								120,
								ResponseMessages.RESPONSE_MESSAGE_120,
								getMachineInformationResponse))
						);
					}
				}

				case Commands.SEND_JOB_ORDER -> {
					Job receivedJob = new Job(0, null, null);
					boolean isJobIdAlreadyExist = false;

					try {
						Type type = new TypeToken<Request<SendJobOrderRequest>>() {}.getType();
						Request<SendJobOrderRequest> requestObject = gson.fromJson(received, type);

						receivedJob.JobUniqueId = requestObject.RequestBody.JobUniqueId;
						receivedJob.JobType = requestObject.RequestBody.JobType;
						receivedJob.JobLength = requestObject.RequestBody.JobLength;
						receivedJob.JobState = Job.JOB_STATE_PENDING;
					}
					catch (JsonSyntaxException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					try {
						if (receivedJob.JobUniqueId == 0 ||
							receivedJob.JobType.isEmpty() ||
							receivedJob.JobLength.isEmpty()) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_410));
							break;
						}
					}
					catch (NullPointerException e) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_410));
						break;
					}

					jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

					try {
						jobs = jobsFuture.get();
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					for (Job job : jobs) {
						if (job.JobUniqueId == receivedJob.JobUniqueId) {
							isJobIdAlreadyExist = true;
							break;
						}
					}

					if (isJobIdAlreadyExist) {
						output.println(gson.toJson(ErrorResponses.RESPONSE_230));
					}
					else {
						if (!(receivedJob.JobType.equals(JobTypes.CNC) ||
								receivedJob.JobType.equals(JobTypes.DOKUM) ||
								receivedJob.JobType.equals(JobTypes.KILIF) ||
								receivedJob.JobType.equals(JobTypes.KAPLAMA))) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_231));
							break;
						}

						try {
							metric = receivedJob.JobLength.split(" ")[1];

							if (metric.equals(JobTypes.METRICS.get(receivedJob.JobType))) {
								executorService.execute(new ArrayListProducer<Job>(jobsSharedLocation, receivedJob));

								output.println(gson.toJson(SuccessfulResponses.RESPONSE_130));

								System.out.println(
									"Added Job: " +
									"Job Unique Id: " + receivedJob.JobUniqueId + ", " +
									"Job Type: " + receivedJob.JobType + ", " +
									"Job Length: " + receivedJob.JobLength + ", " +
									"Job State: " + receivedJob.JobState
								);

								assignJobToMachine();
							} else {
								output.println(gson.toJson(ErrorResponses.RESPONSE_232));
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_232));
						}
					}
				}

				case Commands.GET_PENDING_JOB_ORDERS -> {
					GetPendingJobOrdersResponse getPendingJobOrdersResponse = new GetPendingJobOrdersResponse();
					jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

					boolean isCNCPendingJobExist = false, isDOKUMPendingJobExist = false, isKILIFPendingJobExist = false, isKAPLAMAPendingJobExist = false;

					try {
						jobs = jobsFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if(jobs.size() == 0){
						output.println(gson.toJson(ErrorResponses.RESPONSE_430));
						break;
					}

					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.CNC)) {
								getPendingJobOrdersResponse.CNCJobOrders.add(job);
								isCNCPendingJobExist = true;
							}
					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.DOKUM)) {
								getPendingJobOrdersResponse.DOKUMJobOrders.add(job);
								isDOKUMPendingJobExist = true;
							}
					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.KILIF)) {
								getPendingJobOrdersResponse.KILIFJobOrders.add(job);
								isKILIFPendingJobExist = true;
							}
					for (Job job : jobs)
						if (job.JobState.equals(Job.JOB_STATE_PENDING))
							if (job.JobType.equals(JobTypes.KAPLAMA)) {
								getPendingJobOrdersResponse.KAPLAMAJobOrders.add(job);
								isKAPLAMAPendingJobExist = true;
							}

					if(!(isCNCPendingJobExist || isDOKUMPendingJobExist || isKILIFPendingJobExist || isKAPLAMAPendingJobExist)){
						output.println(gson.toJson(ErrorResponses.RESPONSE_430));
						break;
					}
					output.println(gson.toJson(new Response<GetPendingJobOrdersResponse>(
							160,
							ResponseMessages.RESPONSE_MESSAGE_160,
							getPendingJobOrdersResponse))
					);
				}

				case Commands.GET_MACHINE_STATES -> {
					GetMachineStatesResponse getMachineStatesResponse = new GetMachineStatesResponse();
					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));
					try {
						machines = machinesFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
					if (machines.size() != 0) {
						for (Machine machine : machines)
							getMachineStatesResponse.Machines.add(machine);

						output.println(gson.toJson(new Response<GetMachineStatesResponse>(170, ResponseMessages.RESPONSE_MESSAGE_170, getMachineStatesResponse)));
					} else {
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
					}
				}

				case Commands.GET_PROCESSING_JOB_ORDERS -> {
					GetProcessingJobResponse getProcessingJobResponse = new GetProcessingJobResponse();

					machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

					try {
						machines = machinesFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					jobsFuture = executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

					try {
						jobs = jobsFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					if (machines.size() != 0) {
						for (Machine machine : machines) {
							if (machine.MachineState.equals(Machine.MACHINE_STATE_BUSY)) {
								jobsArrayListFeature = executorService.submit(new JobAssignmentConsumer(jobSchedulingSharedLocation, machine.MachineUniqueId));

								try {
									jobsArrayList = jobsArrayListFeature.get();
								} catch (InterruptedException | ExecutionException e) {
									e.printStackTrace();
								}

								if (jobsArrayList == null) {
									getProcessingJobResponse.Machines.add(machine);
									getProcessingJobResponse.Jobs.add(null);
									continue;
								}

								for (Job job : jobs) {
									for (Integer jobId1 : jobsArrayList) {
										if (job.JobUniqueId == jobId1) {
											if (job.JobState.equals(Job.JOB_STATE_PROCESSING)) {
												getProcessingJobResponse.Machines.add(machine);
												getProcessingJobResponse.Jobs.add(job);
											}
										}
									}
								}
							}
						}

						if (getProcessingJobResponse.Machines.size() == 0) {
							output.println(gson.toJson(ErrorResponses.RESPONSE_240));
						} else {
							output.println(gson.toJson(new Response<GetProcessingJobResponse>(
									140,
									ResponseMessages.RESPONSE_MESSAGE_140,
									getProcessingJobResponse))
							);
						}
					} else {
						output.println(gson.toJson(ErrorResponses.RESPONSE_420));
					}
				}
				default -> output.println(gson.toJson(ErrorResponses.RESPONSE_400));
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
		jobsFuture =  executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

		try {
			jobs = jobsFuture.get();
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

		try {
			machines = machinesFuture.get();
		}
		catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		long bigFixedValue = 999999999999L;
		long comparisonDelay = bigFixedValue;

		int machineId = 0;
		int jobId = 0;

		for (Job job: jobs){
			if(job.JobState.equals("PENDING")){
				for(Machine machine: machines){
					if(machine.MachineState.equals("EMPTY")){
						if(machine.MachineType.equals(job.JobType)){
							int productionSpeed = Integer.parseInt(machine.MachineProductionSpeed.split(" ")[0]);
							int productLength = Integer.parseInt(job.JobLength.split(" ")[0]);
							//long delay = productLength / productionSpeed * 1000L * 60L;
							long delay = productLength / productionSpeed * 1000L;
							if(comparisonDelay > delay){
								comparisonDelay = delay;
								machineId = machine.MachineUniqueId;
								jobId = job.JobUniqueId;
							}

						}
					}
				}
			}
		}

		if(machineId != 0 && jobId != 0 && comparisonDelay != bigFixedValue){
			for (Job job: jobs){
				if(job.JobUniqueId == jobId){
					for(Machine machine: machines){
						if(machine.MachineUniqueId == machineId){
							machine.MachineState = "BUSY";
							job.JobState = "PROCESSING";
							executorService.execute(new JobAssignmentProducer(jobSchedulingSharedLocation, machine.MachineUniqueId, job.JobUniqueId));
							System.out.println("Job assigned.");
							completeJob(machine.MachineUniqueId, job.JobUniqueId, comparisonDelay);
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
				jobsFuture =  executorService.submit(new ArrayListConsumer<Job>(jobsSharedLocation));

				try {
					jobs = jobsFuture.get();
				}
				catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}

				machinesFuture = executorService.submit(new ArrayListConsumer<Machine>(machinesSharedLocation));

				try {
					machines = machinesFuture.get();
				}
				catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}

				for(Machine machine: machines){
					if(machineId == machine.MachineUniqueId){
						machine.MachineState = Machine.MACHINE_STATE_EMPTY;
						System.out.println("Machine State: " + machine.MachineState + " " + "Machine Id: " + machine.MachineUniqueId);
					}
				}

				for(Job job: jobs){
					if(jobId == job.JobUniqueId){
						job.JobState = Job.JOB_STATE_COMPLETED;
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

