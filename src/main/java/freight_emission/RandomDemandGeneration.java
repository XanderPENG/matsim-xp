package freight_emission;

import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.freight.carriers.CarrierShipment;

import java.util.*;

class RandomDemandGeneration {
    private final Network multimodalNetwork;
    private double demandAmount;  // Demand amount in kg
    private int numDemandPoints;
    private Tuple<Integer, Integer> demandRange;  // Demand range in kg
    private Boundary boundary;
    private Set<Id<Node>> depots = Set.of(Id.createNodeId("3408495316"), Id.createNodeId("1531042729"));

    public RandomDemandGeneration(Network multimodalNetwork) {
        this.multimodalNetwork = multimodalNetwork;
        this.demandAmount = 1000;  // kg
        this.numDemandPoints = 50;
        this.demandRange = new Tuple<>(5, 25);  // kg
        this.boundary = new Boundary(172161.734, 174265.856, 173412.828, 175444.093);
    }

    public RandomDemandGeneration(Network multimodalNetwork, Set<Id<Node>> depots, double demandAmount, int numDemandPoints, Tuple<Integer, Integer> demandRange, Boundary boundary) {
        this.multimodalNetwork = multimodalNetwork;
        this.depots = depots;
        this.demandAmount = demandAmount;
        this.numDemandPoints = numDemandPoints;
        this.demandRange = demandRange;
        this.boundary = boundary;
    }


    public Set<CarrierShipment> generateDemandWithoutTimeWindow(Set<String> transportModes) {
        // Get the depot links
        Set<Link> depotLinks = findDepotLinks(transportModes);

        Set<Link> filteredLinks = filterLinksWithAllowedTransportModes(transportModes);
        // Randomly select demand points
        List<Link> selectedLinks = new ArrayList<>();
        for (int i = 0; i < numDemandPoints; i++) {
            int randomIndex = (int) (Math.random() * filteredLinks.size());
            Link randomLink = (Link) filteredLinks.toArray()[randomIndex];
            while (selectedLinks.contains(randomLink)) {
                randomIndex = (int) (Math.random() * filteredLinks.size());
                randomLink = (Link) filteredLinks.toArray()[randomIndex];
            }
            selectedLinks.add(randomLink);
        }

        List<Integer> goodsWeights = generateRandomNumbers((int) demandAmount, numDemandPoints, demandRange.getFirst(), demandRange.getSecond());
        Set<CarrierShipment> jobs = new HashSet<>();

        // Create shipment jobs
        for (int i = 0; i < numDemandPoints; i++) {
            Id<CarrierShipment> shipmentId = Id.create("shipment_" + i, CarrierShipment.class);
            // Randomly select a depot
            int randomDepotIndex = (int) (Math.random() * depotLinks.size());
            Link randomDepotLink = (Link) depotLinks.toArray()[randomDepotIndex];
            CarrierShipment shipment = new CarrierShipment.Builder(shipmentId, randomDepotLink.getId(), selectedLinks.get(i).getId(), goodsWeights.get(i))
                    .build();
            jobs.add(shipment);
        }

        return jobs;
    }


    public static List<Integer> generateRandomNumbers(int totalAmount, int numberOfParts, int minValue, int maxValue) {
        List<Integer> numbers = new ArrayList<>();
        Random random = new Random();
        int sum = 0;

        // Generate initial random numbers between minValue and maxValue
        for (int i = 0; i < numberOfParts; i++) {
            int randomValue = random.nextInt(maxValue - minValue + 1) + minValue;
            numbers.add(randomValue);
            sum += randomValue;
        }

        // Adjust the sum to ensure the total sum equals totalAmount
        int difference = totalAmount - sum;

        // Distribute the difference
        while (difference != 0) {
            for (int i = 0; i < numberOfParts && difference != 0; i++) {
                int currentValue = numbers.get(i);

                if (difference > 0 && currentValue < maxValue) {
                    int adjustment = Math.min(difference, maxValue - currentValue);
                    numbers.set(i, currentValue + adjustment);
                    difference -= adjustment;
                } else if (difference < 0 && currentValue > minValue) {
                    int adjustment = Math.min(-difference, currentValue - minValue);
                    numbers.set(i, currentValue - adjustment);
                    difference += adjustment;
                }
            }
        }

        return numbers;
    }

    public Set<Link> findDepotLinks(Set<String> transportModes) {
        Set<Link> depotLinks = new HashSet<>();
        for (Id<Node> depotId : depots) {
            Node depotNode = multimodalNetwork.getNodes().get(depotId);
            // Add in links
            for (Link link : depotNode.getInLinks().values()) {
                if (link.getAllowedModes().containsAll(transportModes)) {
                    depotLinks.add(link);
                }
            }
            // add out links
            for (Link link : depotNode.getOutLinks().values()) {
                if (link.getAllowedModes().containsAll(transportModes)) {
                    depotLinks.add(link);
                }
            }
        }
        return depotLinks;
    }

    private Set<Node> filterNodesWithAllowedTransportModes(Set<String> transportModes) {
        Set<Node> filteredNodes = new HashSet<>();
        for (Link link : multimodalNetwork.getLinks().values()) {
            if (link.getAllowedModes().containsAll(transportModes)) {
                // Add the nodes of the link to the set
                filteredNodes.add(link.getFromNode());
                filteredNodes.add(link.getToNode());
            }
        }
        return filteredNodes;
    }

    private Set<Link> filterLinksWithAllowedTransportModes(Set<String> transportModes) {
        Set<Link> filteredLinks = new HashSet<>();
        for (Link link : multimodalNetwork.getLinks().values()) {
            if (link.getAllowedModes().containsAll(transportModes)) {
                filteredLinks.add(link);
            }
        }
        return filteredLinks;
    }

    private boolean isWithinBoundary(double x, double y) {
        assert boundary != null;
        return boundary.getMinX() <= x && x <= boundary.getMaxX() && boundary.getMinY() <= y && y <= boundary.getMaxY();
    }

    private boolean isWithinBoundary(Node node) {
        return isWithinBoundary(node.getCoord().getX(), node.getCoord().getY());
    }

    public static class Boundary {
        private final double xMin;
        private final double xMax;
        private final double yMin;
        private final double yMax;

        public Boundary(double xMin, double xMax, double yMin, double yMax) {
            this.xMin = xMin;
            this.xMax = xMax;
            this.yMin = yMin;
            this.yMax = yMax;
        }

        public double getMinX() {
            return xMin;
        }

        public double getMaxX() {
            return xMax;
        }

        public double getMinY() {
            return yMin;
        }

        public double getMaxY() {
            return yMax;
        }
    }
}
