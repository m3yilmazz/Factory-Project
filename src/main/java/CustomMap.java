import java.util.ArrayList;

public interface CustomMap {
   void set(int machineId, int jobId) throws InterruptedException;
   ArrayList<Integer> get(int machineId) throws InterruptedException;
   void remove(int machineId) throws InterruptedException;
}