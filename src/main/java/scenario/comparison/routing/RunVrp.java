package scenario.comparison.routing;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;
import network.core.TransMode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Deprecated
public class RunVrp {
    private static final Logger LOG = LogManager.getLogger(RunVrp.class);

    public static void main(String[] args) {

        // Load MATSim network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("../../data/intermediate/test/GemeenteLeuvenCleanedAllModesV1.xml.gz");
        Network network = scenario.getNetwork();

        // Extract the bike network
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
        Network bikeNetwork = NetworkUtils.createNetwork();
        filter.filter(bikeNetwork, Set.of(TransMode.Mode.BIKE.name));
//        Network bikeNetwork = deriveSubNetwork(network, TransMode.Mode.BIKE);
        // Get the nodes and links from the bike subnetwork
        List<Node> nodes = new ArrayList<>(bikeNetwork.getNodes().values());
        List<Link> links = new ArrayList<>(bikeNetwork.getLinks().values());

//        List<Node> nodes = new ArrayList<>(network.getNodes().values());
//        List<Link> links = new ArrayList<>(network.getLinks().values());


        // Generate the VRP problem using jsprit
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

        // Add depot (origin point for vehicles)
        Node depotNode = nodes.get(20); // assuming the first node is the depot
        Location depotLocation = Location.Builder.newInstance()
                .setId(depotNode.getId().toString())
                .setCoordinate(Coordinate.newInstance(depotNode.getCoord().getX(), depotNode.getCoord().getY()))
                .build();

        // Define a vehicle type
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("0")
                .addCapacityDimension(0, 100)
                .setProfile("cargo_bike_type1");
        VehicleTypeImpl vehicleType = vehicleTypeBuilder.build();

        // Define a vehicle
        VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance("cargo_bike1")
                .setStartLocation(depotLocation)
                .setReturnToDepot(true)
                .setType(vehicleType);
        VehicleImpl vehicle = vehicleBuilder.build();
        vrpBuilder.addVehicle(vehicle);
        List<Integer> serviceNodesIdx = List.of(10, 30);
        List<Node> serviceNodes = new ArrayList<>();
        for (int i : serviceNodesIdx) {
            serviceNodes.add(nodes.get(i));
        }
        // Add services (delivery points)
        for (int i : serviceNodesIdx) {
            Node deliveryNode = nodes.get(i);
            Service service = Service.Builder.newInstance("service_" + i)
                    .addSizeDimension(0, 5)  // demand of the service (adjust as needed)
                    .setLocation(Location.Builder.newInstance()
                            .setId(deliveryNode.getId().toString())
                            .setCoordinate(Coordinate.newInstance(deliveryNode.getCoord().getX(), deliveryNode.getCoord().getY()))
                            .build())
                    .build();
            vrpBuilder.addJob(service);
        }
        // Add the depot and get all locations
        List<Node> allLocations = new ArrayList<>(serviceNodes);
        allLocations.add(depotNode);
        // Get cost matrix
        VehicleRoutingTransportCostsMatrix costMatrix = new RunVrp().createCostMatrixFromMATSim(bikeNetwork, allLocations);

        // Step 4: Build the problem
        VehicleRoutingProblem problem = vrpBuilder.setRoutingCost(costMatrix).build();

        // Step 5: Solve the VRP problem
        VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(problem);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

        // Get the best solution
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);


        // Step 6: Output the solution
        System.out.println("Solution found:");
        VehicleRoutingTransportCosts transportCosts = problem.getTransportCosts();
        for (VehicleRoute route : bestSolution.getRoutes()) {
            double totalTime = 0;
            double totalDistance = 0;
            Location previousLocation = route.getStart().getLocation();
            System.out.println("Route for vehicle: " + route.getVehicle().getId());
            System.out.println("Start location: " + route.getStart().getLocation());
            System.out.println("Total cost: " + route.getVehicle().getType().getVehicleCostParams().fix);
            for (TourActivity activity : route.getActivities()) {
                Location currentLocation = activity.getLocation();
                double travelTime = transportCosts.getTransportTime(previousLocation, currentLocation, 0, null, route.getVehicle());
                double travelDistance = transportCosts.getTransportCost(previousLocation, currentLocation, 0, null, route.getVehicle());
                totalTime += travelTime;
                totalDistance += travelDistance;
                System.out.println("Activity: " + activity.getName() + " at location " + activity.getLocation().getId());
                System.out.println("Arrival time: " + activity.getArrTime());
                System.out.println("End time: " + activity.getEndTime());
            }
            System.out.println("End location: " + route.getEnd().getLocation());
            System.out.println("Total time: " + totalTime);
            System.out.println("Total distance: " + totalDistance);
        }

        // Print costs at the global level
        SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);

        // Write the solution to a CSV file
        new RunVrp().writeSolutionToCsv(bestSolution, transportCosts, "../../data/clean/comparison/routing/vrpResult.csv");
    }

    VehicleRoutingTransportCostsMatrix createCostMatrixFromMATSim(Network network, List<Node> nodes){
        LOG.info("Creating cost matrix from MATSim network");
        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder.newInstance(true);
        // Create a Dijkstra or A* algorithm to find the shortest paths

        TravelTime travelTime = new CustomizedTravelTime();
        TravelDisutility travelDisutility = new customizedTravelDisutility();
        DijkstraFactory dijkstraFactory = new DijkstraFactory(false);
        LeastCostPathCalculator dijkstra = dijkstraFactory.createPathCalculator(network, travelDisutility, travelTime);

        for (Node fromNode : nodes) {
            for (Node toNode : nodes) {
                LOG.info("Calculating cost from node {} to node {}", fromNode.getId(), toNode.getId());
                if (fromNode.equals(toNode)) {
                    costMatrixBuilder.addTransportDistance(fromNode.getId().toString(), toNode.getId().toString(), 0);
                    costMatrixBuilder.addTransportTime(fromNode.getId().toString(), toNode.getId().toString(), 0);
                    continue;
                }
                LeastCostPathCalculator.Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 0, null, null);
                LOG.info("Adding dist and time to from node {} and to node {}", fromNode.getId().toString(), toNode.getId().toString());
                costMatrixBuilder.addTransportDistance(fromNode.getId().toString(), toNode.getId().toString(), path.travelCost);
                costMatrixBuilder.addTransportTime(fromNode.getId().toString(), toNode.getId().toString(), path.travelTime);
                LOG.info("Cost from node {} {} to node {} {} is {}, {}", fromNode.getId(), fromNode.getCoord(), toNode.getId(), toNode.getCoord(), path.travelCost, path.travelTime);
            }
        }
        return costMatrixBuilder.build();
    }

    void writeSolutionToCsv(VehicleRoutingProblemSolution solution, VehicleRoutingTransportCosts transportCosts, String filename){
        try {
            CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(filename), CSVFormat.DEFAULT);
            LOG.info("Writing solution to file {}", filename);
            csvPrinter.printRecord("VehicleId", "Activity", "LocationId", "LocationCoords", "ArrivalTime", "EndTime", "Travel Time", "Travel Distance", "AccumulatedTime", "AccumulatedDistance", "AccumulatedCost");

            for (VehicleRoute route : solution.getRoutes()) {
                double totalTime = 0;
                double totalDistance = 0;
                double totalCost = 0;
                Location previousLocation = route.getStart().getLocation();
                // Record the Start location
                csvPrinter.printRecord(route.getVehicle().getId(), "Start", route.getStart().getLocation().getId(), route.getStart().getLocation().getCoordinate(), 0, 0, 0, 0, 0, 0, 0);
                for (TourActivity activity : route.getActivities()) {
                    Location currentLocation = activity.getLocation();
                    // Calculate the travel time and distance
                    double travelTime = transportCosts.getTransportTime(previousLocation, currentLocation, 0, null, route.getVehicle());
                    double travelDistance = transportCosts.getDistance(previousLocation, currentLocation, 0, route.getVehicle());
                    double travelCost = transportCosts.getTransportCost(previousLocation, currentLocation, 0, null, route.getVehicle());
                    // Update the total time and distance
                    totalTime += travelTime;
                    totalDistance += travelDistance;
                    totalCost += travelCost;
                    csvPrinter.printRecord(route.getVehicle().getId(), activity.getName(), activity.getLocation().getId(), activity.getLocation().getCoordinate(),
                            activity.getArrTime(), activity.getEndTime(), travelTime, travelDistance, totalTime, totalDistance, travelCost);
                    previousLocation = currentLocation;
                }
                // Record the End location
                Location endLocation = route.getEnd().getLocation();
                double travelTime = transportCosts.getTransportTime(previousLocation, endLocation, 0, null, route.getVehicle());
                double travelDistance = transportCosts.getDistance(previousLocation, endLocation, 0, route.getVehicle());
                double travelCost = transportCosts.getTransportCost(previousLocation, endLocation, 0, null, route.getVehicle());
                totalTime += travelTime;
                totalDistance += travelDistance;
                totalCost += travelCost;
                csvPrinter.printRecord(route.getVehicle().getId(), "End", endLocation.getId(), endLocation.getCoordinate(), 0, 0, travelTime, travelDistance, totalTime, totalDistance, totalCost);
            }
            LOG.info("Solution written to file {}", filename);
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static class CustomizedTravelTime implements TravelTime {
        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            return 0;
        }
    }

    static class customizedTravelDisutility implements TravelDisutility {
        @Override
        public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
            return link.getLength();
        }

        @Override
        public double getLinkMinimumTravelDisutility(Link link) {
            return link.getLength();
        }
    }
}
