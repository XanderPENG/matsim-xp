package receiver;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.VehicleType;

/**
 * This class is used to generate carriers for my example.
 * Specifically, only 2 carriers are generated.
 */
public class CarrierGeneration {
    Carriers carriers = new Carriers();

    public CarrierGeneration() {}

    public void generateCarriers(){
        // Create 2 vehicle types
        VehicleType lightVanType = CarrierVehicleType.Builder.newInstance(Id.create("light", VehicleType.class))
                .setCapacity(3000)  // in kg
                .setFixCost(100)
                .setCostPerDistanceUnit(4.22E-3)
                .setCostPerTimeUnit(0.089)
                .build();
        VehicleType heavyVanType = CarrierVehicleType.Builder.newInstance(Id.create("heavy", VehicleType.class))
                .setCapacity(5000)  // in kg
                .setFixCost(150)
                .setCostPerDistanceUnit(5.22E-3)
                .setCostPerTimeUnit(0.109)
                .build();
        // Create Carrier 1
        Carrier carrier1 = CarriersUtils.createCarrier(Id.create("carrier1", Carrier.class));
        CarrierVehicle lightVan = CarrierVehicle.Builder.newInstance(
                        Id.createVehicleId("lightVan1"),
                        Id.createLinkId("i(3,4)"),
                        lightVanType)
                .build();
        CarrierVehicle heavyVan = CarrierVehicle.Builder.newInstance(
                        Id.createVehicleId("heavyVan1"),
                        Id.createLinkId("i(3,4)"),
                        heavyVanType)
                .build();
        CarrierCapabilities carrierCapabilities1 = CarrierCapabilities.Builder.newInstance()
                .addVehicle(lightVan)
                .addVehicle(heavyVan)
                .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
                .build();
        carrier1.setCarrierCapabilities(carrierCapabilities1);
        carriers.addCarrier(carrier1);

        // Create Carrier 2
        Carrier carrier2 = CarriersUtils.createCarrier(Id.create("carrier2", Carrier.class));
        CarrierVehicle lightVan2 = CarrierVehicle.Builder.newInstance(
                        Id.createVehicleId("lightVan2"),
                        Id.createLinkId("i(7,7)R"),
                        lightVanType)
                .build();
        CarrierVehicle heavyVan2 = CarrierVehicle.Builder.newInstance(
                        Id.createVehicleId("heavyVan2"),
                        Id.createLinkId("i(7,7)R"),
                        heavyVanType)
                .build();
        CarrierCapabilities carrierCapabilities2 = CarrierCapabilities.Builder.newInstance()
                .addVehicle(lightVan2)
                .addVehicle(heavyVan2)
                .setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
                .build();
        carrier2.setCarrierCapabilities(carrierCapabilities2);
        carriers.addCarrier(carrier2);
    }

    public Carriers getCarriers(){
        return carriers;
    }


}
