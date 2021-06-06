import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchedCustomMap implements CustomMap {
   private final Lock accessLock = new ReentrantLock();

   private final Condition canWrite = accessLock.newCondition();
   private final Condition canRead = accessLock.newCondition();

   private HashMap<Integer, ArrayList<Integer>> jobAssignment = new HashMap<Integer, ArrayList<Integer>>();

   public void set( int machineId, int jobId ) throws InterruptedException
   {
      accessLock.lock();

      try
      {
         ArrayList<Integer> jobList = new ArrayList<Integer>() ;
         jobList = jobAssignment.get(machineId) == null ? new ArrayList<Integer>() : jobAssignment.get(machineId);
         jobList.add(jobId);
         jobAssignment.put(machineId, jobList);
         System.out.println("Job Setted: " + machineId + " with value: " + jobAssignment.get(machineId));

         canRead.signal();
      }
      finally
      {
         accessLock.unlock();
      }
   }

   @Override
   public ArrayList<Integer> get(int machineId) throws InterruptedException {
      accessLock.lock();
      ArrayList<Integer> jobList;
      try{
         jobList = jobAssignment.get(machineId);

         canWrite.signal();
      }
      finally
      {
         accessLock.unlock();
      }

      return jobList;
   }
}