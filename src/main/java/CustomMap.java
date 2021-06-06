import java.util.ArrayList;

public interface CustomMap {
   public void set(int machineId, int jobId) throws InterruptedException;
   public ArrayList<Integer> get(int machineId) throws InterruptedException;
}