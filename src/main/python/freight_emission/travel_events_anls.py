from utils import read_matsim_events_as_df
import pandas as pd
import geopandas as gpd

def read_travel_events(input_file_dir):
    # Read the travel events
    travel_events = read_matsim_events_as_df(input_file_dir + 'output_events.xml.gz',
                                             event_types='entered link,left link')
    return travel_events

def derive_vehicle_travel_chain(travel_events_df):
    vehicles_travel_chain = {}
    for vehicle in travel_events_df['vehicle'].unique():
        sub_df = travel_events_df.query('vehicle == @vehicle')
        visited_links = []
        for idx, row in sub_df.iterrows():
            if row['link'] not in visited_links:
                visited_links.append(row['link'])
        vehicles_travel_chain[vehicle] = visited_links
    return vehicles_travel_chain

def cal_vkt(v_travel_chain: list, network: gpd.GeoDataFrame):
    # Create a dataframe of chains
    vkt_dict = {}
    for idx, link in enumerate(v_travel_chain):
        vkt_dict[idx] = {'link': link}
    vkt_df = pd.DataFrame.from_dict(vkt_dict, orient='index')
    # Join with network
    vkt_df = vkt_df.merge(network, left_on='link', right_on='link_id', how='left')
    vkt = vkt_df['length'].sum()
    return vkt

def derive_all_vkt_as_dict(travel_events_df, network: gpd.GeoDataFrame):
    vehicles_travel_chain = derive_vehicle_travel_chain(travel_events_df)
    vkt_dict = {}
    for vehicle, v_travel_chain in vehicles_travel_chain.items():
        vkt = cal_vkt(v_travel_chain, network)
        vkt_dict[vehicle] = vkt
    return vkt_dict

def write_vkt_to_csv(vkt_dict, output_dir):
    vkt_df = pd.DataFrame.from_dict(vkt_dict, orient='index', columns=['vkt'])
    vkt_df.reset_index(inplace=True)
    vkt_df.rename(columns={'index': 'vehicle'}, inplace=True)
    vkt_df.to_csv(output_dir+'vkt.csv.gz',
                  index=False,
                  compression='gzip',
                  encoding='utf-8-sig')
    return vkt_df