import java.util.ArrayList;

public interface CustomArrayList<T> {
    void set(T object) throws InterruptedException;
    ArrayList<T> get() throws InterruptedException;
}
