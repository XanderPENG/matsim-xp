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
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class VrpSolving {
    private static final Logger LOG = LogManager.getLogger(VrpSolving.class);
    private final Network network;
    private final List<Node> depotNodes;
    private final List<Service> services;
    private final List<Node> serviceNodes;
    private final List<Vehicle> vehicles;

    private VehicleRoutingProblemSolution solution;
    private VehicleRoutingProblem problem;

    VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

    public VrpSolving(Network network, List<Node> depotNodes,
                      List<Service> services, List<Node> serviceNodes, List<Vehicle> vehicles) {
        this.network = network;
        this.depotNodes = depotNodes;
        this.services = services;
        this.vehicles = vehicles;
        this.serviceNodes = serviceNodes;
    }

    private void createVrp(){
        List<Node> allNodes = new ArrayList<>(this.serviceNodes);
        allNodes.addAll(this.depotNodes);

        VehicleRoutingTransportCostsMatrix costMatrix = createCostMatrixFromMATSim(this.network, allNodes);
        vrpBuilder.setRoutingCost(costMatrix);
        vrpBuilder.addAllJobs(this.services);
        vrpBuilder.addAllVehicles(this.vehicles);
//        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        problem = vrpBuilder.build();
    }

    public void solve(){
        createVrp();
        VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(this.problem);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
        solution = bestSolution;
        // print solution
        SolutionPrinter.print(this.problem, bestSolution, SolutionPrinter.Print.VERBOSE);
    }

    public void writeSolutionToCsv(String filename){
        try {
            CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(filename), CSVFormat.DEFAULT);
            LOG.info("Writing solution to file {}", filename);
            csvPrinter.printRecord("VehicleId", "Activity", "LocationId", "LocationCoords", "ArrivalTime", "EndTime", "Travel Time", "Travel Distance", "AccumulatedTime", "AccumulatedDistance", "AccumulatedCost");
            VehicleRoutingTransportCosts transportCosts = problem.getTransportCosts();
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

    private VehicleRoutingTransportCostsMatrix createCostMatrixFromMATSim(Network network, List<Node> nodes){
        LOG.info("Creating cost matrix from MATSim network");
        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder.newInstance(true);
        // Create a Dijkstra or A* algorithm to find the shortest paths

        TravelTime travelTime = new CustomizedTravelTime();
        TravelDisutility travelDisutility = new RunVrp.customizedTravelDisutility();
        DijkstraFactory dijkstraFactory = new DijkstraFactory(false);
        LeastCostPathCalculator dijkstra = dijkstraFactory.createPathCalculator(network, travelDisutility, travelTime);

        for (Node fromNode : nodes) {
            for (Node toNode : nodes) {
//                LOG.info("Calculating cost from node {} to node {}", fromNode.getId(), toNode.getId());
                if (fromNode.equals(toNode)) {
                    costMatrixBuilder.addTransportDistance(fromNode.getId().toString(), toNode.getId().toString(), 0);
                    costMatrixBuilder.addTransportTime(fromNode.getId().toString(), toNode.getId().toString(), 0);
                    continue;
                }
                LeastCostPathCalculator.Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 0, null, null);
                costMatrixBuilder.addTransportDistance(fromNode.getId().toString(), toNode.getId().toString(), path.travelCost);
                costMatrixBuilder.addTransportTime(fromNode.getId().toString(), toNode.getId().toString(), path.travelTime);
//                LOG.info("Cost from node {} {} to node {} {} is {}, {}", fromNode.getId(), fromNode.getCoord(), toNode.getId(), toNode.getCoord(), path.travelCost, path.travelTime);
            }
        }
        LOG.info("Cost matrix created");
        return costMatrixBuilder.build();
    }

    static class CustomizedTravelTime implements TravelTime {
        @Override
        public double getLinkTravelTime(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
            return link.getLength() / link.getFreespeed();
        }
    }

    static class customizedTravelDisutility implements TravelDisutility {
        @Override
        public double getLinkTravelDisutility(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
            return link.getLength();
        }

        @Override
        public double getLinkMinimumTravelDisutility(Link link) {
            return link.getLength();
        }
    }


}
