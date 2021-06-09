public class Machine {
    static final String MACHINE_STATE_BUSY = "BUSY";
    static final String MACHINE_STATE_EMPTY = "EMPTY";

    int MachineUniqueId;
    String MachineName;
    String MachineType;
    String MachineProductionSpeed;
    String MachineState;

    Machine(int MachineUniqueId, String MachineName, String MachineType, String MachineProductionSpeed){
        this.MachineUniqueId = MachineUniqueId;
        this.MachineName = MachineName;
        this.MachineType = MachineType;
        this.MachineProductionSpeed = MachineProductionSpeed;
        this.MachineState = Machine.MACHINE_STATE_EMPTY;
    }
}
