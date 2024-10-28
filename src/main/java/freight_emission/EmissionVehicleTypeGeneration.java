package freight_emission;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.*;

public class EmissionVehicleTypeGeneration {
    public static void main(String[] args) {
        Vehicles vehicles = VehicleUtils.createVehiclesContainer();

        VehicleType van = VehicleUtils.createVehicleType(Id.create("van", VehicleType.class));
        van.setLength(5.0);
        van.setWidth(2.0);
        EngineInformation engineInformation = van.getEngineInformation();
        engineInformation.getAttributes().putAttribute("HbefaVehicleCategory", "LIGHT COMMERCIAL VEHICLE");
        engineInformation.getAttributes().putAttribute("HbefaTechnology", "petrol");
        engineInformation.getAttributes().putAttribute("HbefaSizeClass", "&lt;1,6L");
        engineInformation.getAttributes().putAttribute("HbefaEmissionsConcept", "LCV-P-Euro-3");
        vehicles.addVehicleType(van);

        Vehicle van5 = VehicleUtils.createVehicle(Id.createVehicleId("van_5"), van);
        vehicles.addVehicle(van5);

        // Create a cargo bike
        VehicleType cargoBike = VehicleUtils.createVehicleType(Id.create("cargoBike", VehicleType.class));
        cargoBike.setLength(3.0);
        cargoBike.setWidth(1.0);
        EngineInformation engineInformationCargoBike = cargoBike.getEngineInformation();
        engineInformationCargoBike.getAttributes().putAttribute("HbefaVehicleCategory", "MOTORCYCLE");
        engineInformationCargoBike.getAttributes().putAttribute("HbefaTechnology", "electric");
        engineInformationCargoBike.getAttributes().putAttribute("HbefaSizeClass", "0");
        engineInformationCargoBike.getAttributes().putAttribute("HbefaEmissionsConcept", "eBike");


        // Write the vehicle types to a file
        MatsimVehicleWriter writer = new MatsimVehicleWriter(vehicles);
        writer.writeFile("../../data/intermediate/test/emissionVanTest.xml");

    }
}
