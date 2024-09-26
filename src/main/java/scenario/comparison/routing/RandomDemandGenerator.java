package scenario.comparison.routing;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.util.Coordinate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is used to generate random service demand for the scenario.
 */
class RandomDemandGenerator {
    private final int numServices;
    private final Network network;
    private final Boundary boundary;
    private final List<Node> selectedNodes = new ArrayList<>();

    public RandomDemandGenerator(int numServices, Network network, Boundary boundary) {
        this.numServices = numServices;
        this.network = network;
        this.boundary = boundary;
    }

    public RandomDemandGenerator(int numServices, Network network) {
        this.numServices = numServices;
        this.network = network;
        this.boundary = null;
    }

    private boolean isWithinBoundary(double x, double y) {
        assert boundary != null;
        return boundary.getMinX() <= x && x <= boundary.getMaxX() && boundary.getMinY() <= y && y <= boundary.getMaxY();
    }

    private boolean isWithinBoundary(Node node) {
        return isWithinBoundary(node.getCoord().getX(), node.getCoord().getY());
    }

    public List<Service> generate() {
        // Filter nodes that are within the boundary
        List<Node> filteredNodes = network.getNodes().values().stream()
                .filter(this::isWithinBoundary)
                .collect(Collectors.toList());
        for (int i = 0; i < numServices; i++) {
            boolean isNodeSelected = false;
            while (!isNodeSelected) {
                int randomIndex = (int) (Math.random() * filteredNodes.size());
                Node randomNode = filteredNodes.get(randomIndex);
                if (!selectedNodes.contains(randomNode)) {
                    selectedNodes.add(randomNode);
                    isNodeSelected = true;
                }
            }
        }
        // Create services from selected nodes
        List<Service> services = new ArrayList<>();
        for (Node node : selectedNodes) {
            Service service = Service.Builder.newInstance("Service_"+node.getId())
                    .setLocation(Location.Builder.newInstance()
                            .setId(node.getId().toString())
                            .setCoordinate(Coordinate.newInstance(node.getCoord().getX(), node.getCoord().getY()))
                            .build())
                    .addSizeDimension(0, 5)
                    .build();
            services.add(service);
        }
        return services;
    }

    public List<Node> getSelectedNodes() {
        return this.selectedNodes;
    }

    public void writeSelectedNodesToCsv(String filename) {
        try {
            CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(filename), CSVFormat.DEFAULT);
            csvPrinter.printRecord("Node ID", "X", "Y");
            for (Node node : selectedNodes) {
                csvPrinter.printRecord(node.getId(), node.getCoord().getX(), node.getCoord().getY());
            }
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
