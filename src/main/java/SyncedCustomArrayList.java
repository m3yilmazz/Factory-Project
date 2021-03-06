import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SyncedCustomArrayList<T> implements CustomArrayList<T> {
    private final Lock accessLock = new ReentrantLock();

    private ArrayList<T> arrayList = new ArrayList<T>();

    @Override
    public void set(T object) throws InterruptedException {
        accessLock.lock();

        try {
            arrayList.add(object);
        }
        finally {
            accessLock.unlock();
        }
    }

    @Override
    public ArrayList<T> get() throws InterruptedException {
        accessLock.lock();

        ArrayList<T> _arrayList;

        try {
            _arrayList = arrayList;
        }
        finally {
            accessLock.unlock();
        }

        return _arrayList;
    }
}
