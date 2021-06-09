public class AddMachineRequest {
    int MachineUniqueId;
    String MachineName;
    String MachineType;
    String MachineProductionSpeed;

    AddMachineRequest(int MachineUniqueId, String MachineName, String MachineType, String MachineProductionSpeed){
        this.MachineUniqueId = MachineUniqueId;
        this.MachineName = MachineName;
        this.MachineType = MachineType;
        this.MachineProductionSpeed = MachineProductionSpeed;
    }
}