import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SyncedCustomMap implements CustomMap {
   private final Lock accessLock = new ReentrantLock();

   private HashMap<Integer, ArrayList<Integer>> jobAssignment = new HashMap<Integer, ArrayList<Integer>>();

   public void set( int machineId, int jobId ) throws InterruptedException
   {
      accessLock.lock();
      ArrayList<Integer> jobList = new ArrayList<Integer>() ;

      try
      {
         jobList = jobAssignment.get(machineId) == null ? new ArrayList<Integer>() : jobAssignment.get(machineId);
         jobList.add(jobId);

         jobAssignment.put(machineId, jobList);

         System.out.println("Job Set: " + machineId + " with value: " + jobId);
      }
      finally {
         accessLock.unlock();
      }
   }

   @Override
   public ArrayList<Integer> get(int machineId) throws InterruptedException {
      accessLock.lock();

      ArrayList<Integer> jobList = new ArrayList<Integer>();

      try{
         jobList = jobAssignment.get(machineId);
      }
      finally{
         accessLock.unlock();
      }

      return jobList;
   }

   @Override
   public void remove(int machineId) throws InterruptedException {
      accessLock.lock();

      try {
         jobAssignment.remove(machineId);
      }
      finally {
         accessLock.unlock();
      }
   }
}