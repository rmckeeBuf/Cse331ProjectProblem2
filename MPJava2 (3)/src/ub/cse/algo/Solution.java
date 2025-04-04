package ub.cse.algo;

import ub.cse.algo.util.Pair;

import java.util.*;

import static ub.cse.algo.Traversals.bfsPaths;

public class Solution {

    private Info info;
    private Graph graph;
    private ArrayList<Client> clients;
    private ArrayList<Integer> bandwidths;

    /**
     * Basic Constructor
     *
     * @param info: data parsed from input file
     */
    public Solution(Info info) {
        this.info = info;
        this.graph = info.graph;
        this.clients = info.clients;
        this.bandwidths = info.bandwidths;
    }

    /**
     * Method that returns the calculated
     * SolutionObject as found by your algorithm
     *
     * @return SolutionObject containing the paths, priorities and bandwidths
     */
    public SolutionObject outputPaths() {
        SolutionObject sol = new SolutionObject();

        //For each node make an arraylist to track packets sent per time tick.
        HashMap<Integer, int[]> packets = new HashMap<>();
        for(Integer node : graph.keySet()) {
            packets.put(node, new int[graph.keySet().size()]);
            for (int[] packetsPerTime : packets.values()){
                Arrays.fill(packetsPerTime, 0);
            }
        }

        //bfsPaths to initially generate shortest paths for every client
        HashMap<Integer, ArrayList<Integer>> intPaths = bfsPaths(graph, clients);


        //for each client track their payment
        HashMap<Integer, Integer> payment = new HashMap<>();
        for(Client c: clients){
            payment.put(c.id, c.payment);
        }

        //clash queue of clashes sorted by tolerance
        Queue<Integer> clashes = new PriorityQueue<>(Comparator.comparingInt(x-> payment.get(x)));

        //for each Client's path add 1 to every node at respective time and check if path doesn't violate bandwidth at that node
        for(Integer clientPathToCheck: intPaths.keySet()) {
            int time = 0;
            //go through each node in path
            for (Integer node : intPaths.get(clientPathToCheck)) {
                //increment time tick in this node's array
                packets.get(node)[time] += 1;
                //check if bandwidth is exceeded at this node
                if (packets.get(node)[time] > bandwidths.get(node)) {
                    //if bandwidth is exceeded add this Client's path to clashes
                    clashes.add(clientPathToCheck);
                    //add the node where class occured to clash location
                    //clashLocation.add(node);
                    packets.get(node)[time] -= 1;
                }
                //increment time along this path iteration
                time++;
            }
        }

        //while there are clashes to resolve
        while(!clashes.isEmpty()){
            Integer client = clashes.poll();
            //reroute (if possible) without breaching bandwidth of other nodes
            HashMap<Integer, ArrayList<Integer>> newPaths =bfsPathsBandwidth(graph, clients, packets);

            //if newPaths doesn't contain client then there can't be a path without the clashed node
            if(newPaths.containsKey(client)){
                //replace oldPath with newPath for client
                ArrayList< Integer> oldPath = intPaths.get(client);
                ArrayList< Integer> newPath = newPaths.get(client);
                intPaths.put(client, newPaths.get(client));

                //update packets by subtracting 1 packet sent at each node in oldPath
                int time = 0;
                for(Integer x: oldPath){
                    packets.get(x)[time]--;
                    time++;
                }
                //update packets by adding 1 packet sent at each node in newPath
                time = 0;
                for(Integer x: newPath){
                    packets.get(x)[time]++;
                    time++;
                }
            }
        }
        sol.paths = intPaths;
        return sol;
    }

    //based on bfsPaths from Traversals.java
    public HashMap<Integer, ArrayList<Integer>> bfsPathsBandwidth(Graph graph, ArrayList<Client> clients,HashMap<Integer, int[]> packets){
        int[] priors = new int[graph.size()];
        Arrays.fill(priors, -1);

        Queue<Pair<Integer, Integer>> searchQueue = new LinkedList<>();
        //CHANGE use Pairing of node and time at that node
        searchQueue.add(new Pair<>(graph.contentProvider,0));
        while (!searchQueue.isEmpty()) {
            //CHANGE node is first element of pair and time is second
            Pair<Integer,Integer> current = searchQueue.poll();
            int node = current.getFirst();
            int time = current.getSecond();

            for (int neighbor : graph.get(node)) {
                //CHANGE added a check for bandwidth constraint
                if (priors[neighbor] == -1 && neighbor != graph.contentProvider && packets.get(neighbor)[time + 1] < bandwidths.get(neighbor)){
                    priors[neighbor] = node;
                    //CHANGE pair is being added to queue
                    searchQueue.add(new Pair<>(neighbor, time + 1));
                }
            }
        }
        return pathsFromPriors(clients, priors);
    }

    //pathsFromPriors from Traversals.java no changes
    public HashMap<Integer, ArrayList<Integer>> pathsFromPriors(ArrayList<Client> clients, int[] priors) {
        HashMap<Integer, ArrayList<Integer>> paths = new HashMap<>(clients.size());
        // For every client, traverse the prior array, creating the path
        for (Client client : clients) {
            ArrayList<Integer> path = new ArrayList<>();
            int currentNode = client.id;
            while (currentNode != -1) {
                /*
                    Add this ID to the beginning of the
                    path so the path ends with the client
                 */
                path.add(0, currentNode);
                currentNode = priors[currentNode];
            }
            paths.put(client.id, path);
        }
        return paths;
    }
}

