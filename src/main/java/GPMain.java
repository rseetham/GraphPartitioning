import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class GPMain {
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException
    {
        LabelPropagation lp = new LabelPropagation();

        int numNodes = 196591; //Number of nodes in the network
        int numThreads= 8; //Number of threads to use

        long startTime = System.nanoTime();
        //input is "edgelist" format "id id" sorted by first id (ids are sequentially numbered 1 to numNodes inclusive)
        lp.readEdgesRMat(numNodes, "gedges.txt");
        lp.findCommunities("base_output_path",numThreads); //directory to save current list of communities to after each pass as well as final output files
        lp.writeMemberships("membership.txt");
        lp.writeMembershipsSmart("memberships_renumbered.txt");
        lp.edgeCuts();

        long estimatedTime = System.nanoTime() - startTime;
        System.out.println("Elapsed Time is "+estimatedTime);
    }
}
