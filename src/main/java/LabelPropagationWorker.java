import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.Callable;

public class LabelPropagationWorker implements Callable<Boolean>
{

    private Vector<Integer> dominantLabels;
    private Vector<Integer> labelCounts;
    private int nodeId;
    private int partitionSize = 200; //Number of Partition Needed from the Graph
    private Vector<Node> nodeList; 	// Shared
    private Vector<Integer> threshold; 	// Shared

    public LabelPropagationWorker(Vector<Node> nodeList, Vector<Integer> threshold)
    {
        dominantLabels = new Vector<Integer>();
        labelCounts = new Vector<Integer>(nodeList.size());

        for (int i=0; i<nodeList.size(); i++)
        {
            labelCounts.add(Integer.valueOf(0));
        }

        this.nodeList = nodeList;//Shared reference
        this.threshold = threshold;

        System.out.println("Worker created.");
    }



    public void setNodeToProcess(int nodeId)
    {
        this.nodeId=nodeId;
    }

    public Boolean call()
    {

        if (nodeId==-1)
        {
            return Boolean.FALSE;
        }

        boolean continueRunning = false;

        Collections.fill(labelCounts, Integer.valueOf(0));
        dominantLabels.clear();

        Node currentNode = nodeList.get(nodeId);
        int maxCount = 0;
        // Neighbors
        for (Integer neighborId : currentNode.getNeighbors())
        {

            int nLabel = nodeList.get(neighborId).getLabel();
            if (nLabel == 0)
                continue; // No label yet (only if initial labels are given?)

            int nLabelCount = labelCounts.get(nLabel) + 1;
            labelCounts.set(nLabel, nLabelCount);

            if (maxCount < nLabelCount)
            {
                maxCount = nLabelCount;
                //dominantLabels.clear();
                dominantLabels.add(nLabel);
            }
            else if (maxCount == nLabelCount)
            {
                dominantLabels.add(nLabel);
            }

        }


        int dominantLabelIndex = -1, index = -1;

        if(dominantLabels.size() > 0)
        {
            dominantLabelIndex = Collections.max(labelCounts); //Value
            index = labelCounts.indexOf(dominantLabelIndex);
        }

        if(dominantLabels.size() > 0)
        {
            if(threshold.get(index) < partitionSize)
            {

                //System.out.println("Removing Label which exceeds Threshold");
                int currentVal = currentNode.getLabel();
                int nVal = threshold.get(index) + 1;
                threshold.set(index, nVal);
                int cVal = threshold.get(currentVal) - 1;
                threshold.set(currentVal, cVal);

                if (labelCounts.get(currentVal) < maxCount)
                {
                    continueRunning = true;
                }

                currentNode.setLabel(index);
                dominantLabels.clear();
            }
            else if(threshold.get(index) == partitionSize &&  currentNode.getLabel() == index)
            {
                int currentVal = currentNode.getLabel();
                if (labelCounts.get(currentVal) < maxCount)
                {
                    continueRunning = true;
                }
                dominantLabels.clear();
            }
            else
            {
                int index2 = 1;
                while(index2 > 0)
                {
                    index2 = dominantLabels.indexOf(dominantLabelIndex);
                    if(index2 > 0)
                    {
                        dominantLabels.remove(index2);
                    }

                }

                while(dominantLabels.size() > 0)
                {
                    int val = Collections.min(dominantLabels);
                    int valIndex = dominantLabels.indexOf(val);
                    if(threshold.get(val) == partitionSize)
                    {
                        //System.out.println("Removing Label which exceeds Threshold");
                        dominantLabels.remove(valIndex);
                        continue;
                    }
                    else
                    {
                        //System.out.println("In Else");
                        int currentVal = currentNode.getLabel();
                        int nVal = threshold.get(val) + 1;
                        threshold.set(val, nVal);
                        int cVal = threshold.get(currentVal) - 1;
                        threshold.set(currentVal, cVal);
                        if (labelCounts.get(currentVal) < maxCount)
                        {
                            //System.out.println("Changing Continue Running?");
                            continueRunning = true;
                        }
                        currentNode.setLabel(val);
                        dominantLabels.clear();
                    }
                }
            }
        }
        return Boolean.valueOf(continueRunning);
    }
}