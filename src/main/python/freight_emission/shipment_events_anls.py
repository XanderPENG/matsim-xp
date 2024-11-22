from utils import read_matsim_events_as_df
import pandas as pd
import geopandas as gpd

def read_shipment_events(input_file_dir):
    # Read the shipment events
    shipment_events = read_matsim_events_as_df(input_file_dir + 'output_events.xml.gz',
                                               event_types='Freight shipment pickup starts')
    return shipment_events

def aggregate_vehicle_shipment_demand(shipment_events_df):
    vehicle_capacity_demand = {}
    for vehicle in shipment_events_df['vehicle'].unique():
        sub_df = shipment_events_df.query('vehicle == @vehicle')
        capacity_demand = sub_df['capacityDemand'].sum()
        vehicle_capacity_demand[vehicle] = capacity_demand
    return vehicle_capacity_demand

def write_vehicle_capacity_demand_to_csv(vehicle_capacity_demand, output_dir):
    vehicle_capacity_demand_df = pd.DataFrame.from_dict(vehicle_capacity_demand, orient='index', columns=['capacityDemand'])
    vehicle_capacity_demand_df.reset_index(inplace=True)
    vehicle_capacity_demand_df.rename(columns={'index': 'vehicle'}, inplace=True)
    vehicle_capacity_demand_df.to_csv(output_dir+'vehicle_capacity_demand.csv.gz',
                                      index=False,
                                      compression='gzip',
                                      encoding='utf-8-sig')
    return vehicle_capacity_demand_df