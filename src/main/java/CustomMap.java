import java.util.ArrayList;

public interface CustomMap {
   public void set(int machineId, int jobId) throws InterruptedException;
   public ArrayList<Integer> get(int machineId) throws InterruptedException;
   public void remove(int machineId) throws InterruptedException;
}