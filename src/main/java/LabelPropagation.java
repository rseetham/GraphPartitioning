import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

public class LabelPropagation
{

    private ConcurrentHashMap<Integer,Node> nodeList;
    private Vector<Integer> nodeOrder;
    private Vector<Integer> threshold;
    Logger logger;

    public LabelPropagation(int numNodes) {
        nodeList = new ConcurrentHashMap<Integer,Node>();
        nodeOrder = new Vector<Integer>(numNodes);
        threshold = new Vector<Integer>(numNodes);
        logger = Logger.getLogger("myLogger");
    }

    public void edgeCuts() throws IOException
    {
        int counts = 0;
        Node node;
        int nodeLabel = -1;

        for(int i=0;i<nodeList.size();i++)
        {
            node = nodeList.get(i);
            nodeLabel = node.getLabel();
            for (Integer neighborId : node.getNeighbors())
            {

                int nLabel = nodeList.get(neighborId).getLabel();
                if (nLabel == 0)
                    continue; // No label yet

                if (nodeLabel != nLabel)
                {
                    counts++;
                }
            }
        }
        counts = counts/2;
        System.out.println("The total edge counts is "+ counts);
    }

    public void readEdgesRMat (int numNodes, String fileName) {
        File file = new AccessResource().readFileFromResources(fileName);
        try {

            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split("\t");

                int source = Integer.valueOf(parts[0]);
                Node snode = nodeList.get(source);
                if (snode == null) {
                    nodeList.put(source, new Node(source, source));
                    nodeOrder.add(source);
                    threshold.add(source);
                }
                int target = Integer.valueOf(parts[1]);
                Node tnode = nodeList.get(target);
                if (tnode == null){
                    nodeList.put(target,new Node(target,target));
                    nodeOrder.add(target);
                    threshold.add(target);
                }

                logger.log(WARNING,"Source is" + source);
                logger.log(WARNING,"Target is" + target);

                //Directed Graph
                nodeList.get(source).addNeighbor(nodeList.get(target));
                //.get(target).addNeighbor(nodeList.get(source));
            }
            scanner.close();
        }

        catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("All edges read.");


    }


    public void readEdgesSh(int numNodes, String fileName) throws IOException
    {
        File file = new AccessResource().readFileFromResources(fileName);

        for (int i=0; i<=numNodes; i++) {
            nodeList.put(new Integer(i),new Node(i,i));
            nodeOrder.add(new Integer(i));
            threshold.add(new Integer(i));
        }
        System.out.println("Added " + numNodes + " nodes.");

        try {

            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split("\t");

                int source = Integer.valueOf(parts[0]);
                int target = Integer.valueOf(parts[1]);

                logger.log(WARNING,"Source is" + source);
                logger.log(WARNING,"Target is" + target);

                //Undirected Graph
                nodeList.get(source).addNeighbor(nodeList.get(target));
                nodeList.get(target).addNeighbor(nodeList.get(source));
            }
            scanner.close();
        }

        catch (IOException e) {
            e.printStackTrace();
        }

        logger.log(INFO,"All edges read.");
    }

    public void writeMemberships(String file) throws IOException {

        System.out.println("Writing membership.");

        FileOutputStream fso = new FileOutputStream(file);
        OutputStreamWriter fileWriter = new OutputStreamWriter(fso,Charset.forName("UTF-8"));

        Node n;
        for (int i=0; i<nodeList.size(); i++) {
            n=nodeList.get(i);
            fileWriter.write(n.getId()+" "+n.getLabel()+"\n");
        }

        System.out.println("Membership list written.");

        fileWriter.close();
        fso.close();
    }


    public void readMemberships(String file) throws IOException
    {
        System.out.println("Reading memberships.");
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line = br.readLine();
        while (line!=null)
        {
            String[] parts = line.split("\t");
            int nodeId = Integer.valueOf(parts[0]);
            int label = Integer.valueOf(parts[1]);
            nodeList.get(nodeId).setLabel(label);
            line=br.readLine();
        }
        System.out.println("Memberships loaded from file.");
        br.close();
    }


    public void writeMembershipsSmart(String file) throws IOException
    {

        System.out.println("Writing membership smart.");
        Map<Integer,Integer> labelMap = new HashMap<Integer,Integer>();
        int labelCount=0;
        for (int i=0; i<nodeList.size(); i++)
        {
            int label = nodeList.get(i).getLabel();
            Integer val =  labelMap.get(Integer.valueOf(label));
            if (val==null)
            {
                labelCount++;
                labelMap.put(Integer.valueOf(label), Integer.valueOf(labelCount));
            }
        }
        System.out.println("Found " + labelCount + " communities.");

        FileOutputStream fso = new FileOutputStream(file);
        OutputStreamWriter fileWriter = new OutputStreamWriter(fso,Charset.forName("UTF-8"));

        Node n;
        for (int i=0; i<nodeList.size(); i++)
        {
            n=nodeList.get(i);
            fileWriter.write(n.getId()+" "+labelMap.get(Integer.valueOf(n.getLabel())).intValue() +"\n");
        }

        System.out.println("Smart membership list written.");

        fileWriter.close();
        fso.close();
    }


    public void findCommunities(String basepath, int numThreads) throws InterruptedException, ExecutionException, IOException
    {

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Vector<LabelPropagationWorker> workers = new Vector<LabelPropagationWorker>(numThreads);

        for (int j=1; j<=numThreads; j++)
        {
            workers.add(new LabelPropagationWorker(nodeList,threshold));
        }

        int iter=0;
        int nodesChanged=201;

        while (nodesChanged>200)
        {

            nodesChanged=0;
            System.out.println("Running " + (++iter) + " iteration at " + System.currentTimeMillis() + ".");

            Collections.shuffle(nodeOrder);//DO NOT SHUFFLE nodeList

            for (int i=0; i<nodeList.size(); i+=numThreads)
            {
                for (int j=0; j<numThreads; j++)
                {
                    if ((j+i)<nodeList.size())
                    {
                        workers.get(j).setNodeToProcess(nodeOrder.get(i+j).intValue());
                    }
                    else
                    {
                        workers.get(j).setNodeToProcess(-1);
                    }
                }

                List<Future<Boolean>> results = threadPool.invokeAll(workers);

                for (int j=0; j<results.size(); j++)
                {
                    Boolean r = results.get(j).get();
                    if (r!=null && r.booleanValue()==true)
                    {
                        nodesChanged++;
                        if (nodesChanged==1) System.out.println("Another pass will be needed.");
                        break;
                    }
                }
            }


            //Pass complete
            if (basepath!=null)
            {
                //writeMemberships(basepath+"iter" + iter +"memberships.txt");
                System.out.println(nodesChanged + " nodes were changed in the last iteration.");
            }
        }

        System.out.println("Detection complete!");
        threadPool.shutdown();
    }
}
