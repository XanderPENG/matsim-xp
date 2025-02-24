package receiver;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.receiver.*;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.freight.receiver.run.chessboard.ReceiverChessboardScenario;
//import org.matsim.contrib.freightreceiver.*;
//import org.matsim.contrib.freightreceiver.collaboration.CollaborationUtils;
//import org.matsim.contrib.freightreceiver.run.chessboard.ReceiverChessboardScenario;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.TimeWindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ReceiverOrderGeneration {
    static Id<Link> carrier1OriginId = Id.createLinkId("i(3,4)");
    static Id<Link> carrier2OriginId = Id.createLinkId("i(7,7)R");

    static Id<Carrier> carrier1Id = Id.create("carrier1", Carrier.class);
    static Id<Carrier> carrier2Id = Id.create("carrier2", Carrier.class);

    private final Carriers carriers;
    private final Receivers receivers;

    Map<String, ProductType> productTypes = new HashMap<>();
    Map<Id<Receiver>, ReceiverPlan> receiverPlans = new HashMap<>();

    public ReceiverOrderGeneration(Receivers receivers, Carriers carriers) {
        this.receivers = receivers;
        this.carriers = carriers;
    }

    /**
     * Create 2 Product Types:
     *  ProductType 1:
     *  ProductType 2:
     */
    void generateProductTypes(){
        ProductType productType1 = ReceiverUtils.createAndGetProductType(this.receivers, Id.create("productType1", ProductType.class), carrier1OriginId);
        productType1.setDescription("Product Type 1");
        productType1.setRequiredCapacity(5);

        ProductType productType2 = ReceiverUtils.createAndGetProductType(this.receivers, Id.create("productType2", ProductType.class), carrier2OriginId);
        productType2.setDescription("Product Type 2");
        productType2.setRequiredCapacity(10);

        this.productTypes.put("productType1", productType1);
        this.productTypes.put("productType2", productType2);
    }

    public void generateAllReceiverOrders(){
        generateProductTypes();
        generateCollaborativeReceiverOrders();
        generateNonCollaborativeReceiverOrders();
    }

    void generateReceiverOrders(Receiver receiver, boolean status){
        // Generate receiver product for collaborative receivers
        ReceiverProduct receiverProduct1 = ReceiverProduct.Builder.newInstance()
                .setProductType(this.productTypes.get("productType1"))
                .setReorderingPolicy(ReceiverUtils.createSSReorderPolicy(100, 500))
                .build();
        receiver.addProduct(receiverProduct1);

        ReceiverProduct receiverProduct2 = ReceiverProduct.Builder.newInstance()
                .setProductType(this.productTypes.get("productType2"))
                .setReorderingPolicy(ReceiverUtils.createSSReorderPolicy(200, 1000))
                .build();
        receiver.addProduct(receiverProduct2);

        // generate orders for collaborative receivers
        Collection<Order> orders = new ArrayList<>();

        Order Order1 = Order.Builder.newInstance(Id.create("Order1", Order.class), receiver, receiverProduct1)
                .setServiceTime(5*60)
                .buildWithCalculatedOrderQuantity();

        Order Order2 = Order.Builder.newInstance(Id.create("Order2", Order.class), receiver, receiverProduct2)
                .setServiceTime(10*60)
                .buildWithCalculatedOrderQuantity();

        orders.add(Order1);
        orders.add(Order2);

        // assign orders to receiver
        ReceiverOrder receiverOrder1 = new ReceiverOrder(receiver.getId(), orders, carrier1Id);
        ReceiverOrder receiverOrder2 = new ReceiverOrder(receiver.getId(), orders, carrier2Id);

        ReceiverPlan receiverPlan = ReceiverPlan.Builder.newInstance(receiver, status)
                .addReceiverOrder(receiverOrder1)
                .addReceiverOrder(receiverOrder2)
                .addTimeWindow(TimeWindow.newInstance(6*60*60, 10*60*60))
                .build();

        this.receiverPlans.put(receiver.getId(), receiverPlan);

        receiverPlan.setSelected(true);
        // add the time window cost to the receiver
        receiver.getAttributes().putAttribute(
                ReceiverUtils.ATTR_RECEIVER_TW_COST,
                1);

        ReceiverChessboardScenario.convertReceiverOrdersToInitialCarrierShipments(this.carriers, receiverOrder1, receiverPlan);
        ReceiverChessboardScenario.convertReceiverOrdersToInitialCarrierShipments(this.carriers, receiverOrder2, receiverPlan);
    }

    void generateCollaborativeReceiverOrders(){
        for (Receiver receiver : this.receivers.getReceivers().values()) {
            if ((boolean) receiver.getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS)) {
                generateReceiverOrders(receiver, true);
            }
        }
    }

    void generateNonCollaborativeReceiverOrders(){
        for (Receiver receiver : this.receivers.getReceivers().values()) {
            if (!(boolean) receiver.getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS)) {
                generateReceiverOrders(receiver, false);
            }
        }
    }

    public Map<String, ProductType> getProductTypes() {
        return productTypes;
    }

    public Map<Id<Receiver>, ReceiverPlan> getReceiverPlans() {
        return receiverPlans;
    }

}
