import java.util.HashSet;
import java.util.Set;

public class Node
{
    private int id;
    private int label;
    private Set<Node> neighbors;

    public Node(int id, int label)
    {
        this.id=id;
        this.label=label;
        this.neighbors = new HashSet<Node>();
    }

    public int getId()
    {
        return id;
    }

    public int getLabel()
    {
        return label;
    }

    public void setLabel(int label)
    {
        this.label = label;
    }

    public Set<Node> getNeighbors()
    {
        return neighbors;
    }

    public void setNeighbors(Set<Node> neighbors)
    {
        this.neighbors = neighbors;
    }

    public void addNeighbor(Node n)
    {
        this.neighbors.add(n);
    }
}