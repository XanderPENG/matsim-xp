package freight_emission;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.CarrierVehicleType;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;

/**
 * Factory class to create vehicle types for freight-emission vehicles (i.e., vans and cargo bikes).
 */

public class FreightVehicleTypeFactory {

    public static VehicleType createDefaultVan(String idKey){
        VehicleType van = CarrierVehicleType.Builder.newInstance(Id.create(idKey, VehicleType.class))
                .setCapacity(3000)  // in kg
                .setFixCost(0)
                .setCostPerDistanceUnit(4.22E-3)
                .setCostPerTimeUnit(0.089)
                .build();
        van.getCapacity().setSeats(2);

        van.setLength(5.0);
        van.setWidth(2.0);

//        van.setNetworkMode("car");
        van.setMaximumVelocity(30/3.6); // 50 km/h converted to m/s, this is the maximum velocity of the vehicle
        van.setFlowEfficiencyFactor(1.0); // This is the flow efficiency factor of the vehicle
        van.setPcuEquivalents(1); // This is the passenger car unit equivalent of the vehicle

        EngineInformation engineInformation = van.getEngineInformation();
        engineInformation.getAttributes().putAttribute("HbefaVehicleCategory", "LIGHT_COMMERCIAL_VEHICLE");
        engineInformation.getAttributes().putAttribute("HbefaTechnology", "petrol (4S)");
        engineInformation.getAttributes().putAttribute("HbefaSizeClass", "N1-III");
        engineInformation.getAttributes().putAttribute("HbefaEmissionsConcept", "LCV P Euro-6");

        return van;
    }

    public static VehicleType createDefaultCargoBike(String idKey){
        VehicleType cargoBike = CarrierVehicleType.Builder.newInstance(Id.create(idKey, VehicleType.class))
                .setCapacity(125)  // in kg
                .setFixCost(0)
                .setCostPerDistanceUnit(3E-3)
                .setCostPerTimeUnit(0.058)
                .build();
        cargoBike.getCapacity().setSeats(1);
        cargoBike.setLength(3.0);
        cargoBike.setWidth(1.0);

//        cargoBike.setNetworkMode("bike");
        cargoBike.setMaximumVelocity(25/3.6); // 25 km/h converted to m/s, this is the maximum velocity of the vehicle
        cargoBike.setFlowEfficiencyFactor(1.0); // This is the flow efficiency factor of the vehicle
        cargoBike.setPcuEquivalents(1); // This is the passenger car unit equivalent of the vehicle

        EngineInformation engineInformation = cargoBike.getEngineInformation();
        engineInformation.getAttributes().putAttribute("HbefaVehicleCategory", "MOTORCYCLE");
        engineInformation.getAttributes().putAttribute("HbefaTechnology", "electricity");
        engineInformation.getAttributes().putAttribute("HbefaSizeClass", "NA");
        engineInformation.getAttributes().putAttribute("HbefaEmissionsConcept", "MC Electric");

        return cargoBike;
    }


}
