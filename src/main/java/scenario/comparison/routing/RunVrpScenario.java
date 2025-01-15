package scenario.comparison.routing;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import network.core.TransMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RunVrpScenario {
    private static final Logger LOG = LogManager.getLogger(RunVrpScenario.class);

    public static void main(String[] args) {
        // read the (sub)network
        Network network = readNetwork("../../data/intermediate/test/GemeenteLeuvenCleanedAllModesV1.xml.gz", TransMode.Mode.CAR);
        // generate random demand
        RandomDemandGenerator randomDemandGenerator = new RandomDemandGenerator(50, network, new RandomDemandGenerator.Boundary(172154.734, 174215.856, 173214.828, 175447.093));
        List<Service> services = randomDemandGenerator.generate();
        List<Node> serviceNodes = randomDemandGenerator.getSelectedNodes();
        // Define the depot nodes (2 for example)
        List<Id<Node>> depotNodesId = List.of(Id.createNodeId("3408495316"), Id.createNodeId("1531042729"));
        List<Node> depotNodes = depotNodesId.stream()
                .map(id -> network.getNodes().get(id))
                .collect(Collectors.toList());
        List<Location> depotLocations = depotNodes.stream()
                .map(node -> Location.Builder.newInstance()
                        .setId(node.getId().toString())
                        .setCoordinate(Coordinate.newInstance(node.getCoord().getX(), node.getCoord().getY()))
                        .build())
                .toList();
        LOG.info("Depot locations: " + depotLocations.get(1));
        // Define a vehicle type
        VehicleType vehicleType = createVehicleType("CVT0", new Tuple<>(0, 5000));
        // Define the vehicles
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vehicles.add(createVehicle("CV" + i, vehicleType, depotLocations.get(i < 5 ? 0 : 1), true));
        }

        // Solve the VRP problem
        VrpSolving vrpSolving = new VrpSolving(network, depotNodes, services, serviceNodes, vehicles);
        vrpSolving.solve();

        // write the solution to a file
        vrpSolving.writeSolutionToCsv("../../data/clean/comparison/routing/vrpResultCar5000kg.csv");
    }

    static Network readNetwork(String fileName, TransMode.Mode mode) {
        // Read the whole network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(fileName);
        Network network = scenario.getNetwork();
        if(mode != null) {
            LOG.info("Reading the subnetwork for mode: " + mode.name);
            TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
            Network subNetwork = NetworkUtils.createNetwork();
            filter.filter(subNetwork, Set.of(mode.name));
            return subNetwork;
        } else {
            return network;
        }
    }

    static VehicleType createVehicleType(String id, Tuple<Integer, Integer> capacity) {
        return VehicleTypeImpl.Builder.newInstance(id)
                .addCapacityDimension(capacity.getFirst(), capacity.getSecond())
//                .setProfile("cargo_bike_type1")
                .build();
    }

    static Vehicle createVehicle(String id, VehicleType vehicleType, Location startNode, boolean returnToDepot) {
        return VehicleImpl.Builder.newInstance(id)
                .setStartLocation(startNode)
                .setType(vehicleType)
                .setReturnToDepot(returnToDepot)
                .build();
    }
}
