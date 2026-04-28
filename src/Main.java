import mpi.*;
public class Main {
    public static void main(String[] args) throws Exception {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        System.out.println("Hello, I am " + me + " and the size is " + size);
        MPI.Finalize();
    }
}
