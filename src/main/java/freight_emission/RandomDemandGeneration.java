package freight_emission;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.TimeWindow;

import java.util.*;
import java.util.stream.Collectors;

class RandomDemandGeneration {

    private final Network multimodalNetwork;
    private final double demandAmount;  // Demand amount in kg
    private final int numDemandPoints;
    private final Tuple<Integer, Integer> demandRange;  // Demand range in kg
    private final Boundary boundary;
//    private Set<Id<Node>> depots = Set.of(Id.createNodeId("3408495316"), Id.createNodeId("1531042729"));
    private final Set<Id<Link>> depotLinksId;

    public RandomDemandGeneration(Network multimodalNetwork) {
        this.multimodalNetwork = multimodalNetwork;
        this.demandAmount = 3000;  // kg
        this.numDemandPoints = 200;
        this.demandRange = new Tuple<>(5, 25);  // kg
        this.boundary = new Boundary(172161.734, 174265.856, 173412.828, 175444.093);
        this.depotLinksId = setDefaultDepotLinks();
    }

    public RandomDemandGeneration(Network multimodalNetwork, Set<Id<Link>> depotLinks, double demandAmount, int numDemandPoints, Tuple<Integer, Integer> demandRange, Boundary boundary) {
        this.multimodalNetwork = multimodalNetwork;
        this.demandAmount = demandAmount;
        this.numDemandPoints = numDemandPoints;
        this.demandRange = demandRange;
        this.boundary = boundary;
        this.depotLinksId = depotLinks;
    }

    private Set<Id<Link>> setDefaultDepotLinks() {
        Set<Id<Link>> depotLinks = new HashSet<>();
        depotLinks.add(Id.createLinkId("333784188_r_3"));
        depotLinks.add(Id.createLinkId("27566523_11"));
        return depotLinks;
    }

    public Set<CarrierShipment> generateDemandWithoutTimeWindow(Set<String> transportModes) {
        Set<Link> filteredLinks = filterLinksWithAllowedTransportModes(transportModes);
        // Filter links within the boundary, based on the nodes
        filteredLinks = filteredLinks.stream().filter(link -> isWithinBoundary(link.getFromNode()) && isWithinBoundary(link.getToNode())).collect(Collectors.toSet());
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
            Set<Link> depotLinks = depotLinksId.stream().map(linkId -> multimodalNetwork.getLinks().get(linkId)).collect(Collectors.toSet());
            int randomDepotIndex = (int) (Math.random() * depotLinks.size());
            Link randomDepotLink = (Link) depotLinks.toArray()[randomDepotIndex];
            CarrierShipment shipment = CarrierShipment.Builder.newInstance(shipmentId, randomDepotLink.getId(), selectedLinks.get(i).getId(), goodsWeights.get(i))
                    .build();
            jobs.add(shipment);
        }

        return jobs;
    }

    public Set<CarrierShipment> generateDemandWithTimeWindow(Set<String> transportModes) {
        Set<Link> filteredLinks = filterLinksWithAllowedTransportModes(transportModes);
        // Filter links within the boundary, based on the nodes
        filteredLinks = filteredLinks.stream().filter(link -> isWithinBoundary(link.getFromNode()) && isWithinBoundary(link.getToNode())).collect(Collectors.toSet());
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

        // Create several candidate time windows
        List<TimeWindow> candidateTimeWindows = new ArrayList<>();
        // morning delivery
        candidateTimeWindows.add(TimeWindow.newInstance(6 * 3600, 8 * 3600));
        candidateTimeWindows.add(TimeWindow.newInstance(6.1 * 3600, 8.1 * 3600));
        // midday delivery
//        candidateTimeWindows.add(TimeWindow.newInstance(8 * 3600, 10 * 3600));
//        candidateTimeWindows.add(TimeWindow.newInstance(12 * 3600, 14 * 3600));
        // night delivery - add 2 close time windows to increase the probability of selecting this time window
//        candidateTimeWindows.add(TimeWindow.newInstance(18 * 3600, 20 * 3600));
//        candidateTimeWindows.add(TimeWindow.newInstance(18.1 * 3600, 20.1 * 3600));

        // Create shipment jobs
        for (int i = 0; i < numDemandPoints; i++) {
            Id<CarrierShipment> shipmentId = Id.create("shipment_" + i, CarrierShipment.class);
            // Randomly select a depot
            Set<Link> depotLinks = depotLinksId.stream().map(linkId -> multimodalNetwork.getLinks().get(linkId)).collect(Collectors.toSet());
            int randomDepotIndex = (int) (Math.random() * depotLinks.size());
            Link randomDepotLink = (Link) depotLinks.toArray()[randomDepotIndex];
            int randomTimeWindowIndex = (int) (Math.random() * candidateTimeWindows.size());
            CarrierShipment shipment = CarrierShipment.Builder.newInstance(shipmentId, randomDepotLink.getId(), selectedLinks.get(i).getId(), goodsWeights.get(i))
//                    .setDeliveryTimeWindow(candidateTimeWindows.get(randomTimeWindowIndex))
                    .setDeliveryServiceTime(1.5 * 60)
                    .setPickupTimeWindow(TimeWindow.newInstance(candidateTimeWindows.get(randomTimeWindowIndex).getStart() - 60 * 60,
                            candidateTimeWindows.get(randomTimeWindowIndex).getStart() - 60 * 10))
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

    public Set<Id<Link>> getDepotLinksId() {
        return depotLinksId;
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
