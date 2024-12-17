from utils import read_matsim_events_as_df
import pandas as pd
import geopandas as gpd

def read_travel_events(input_file_dir, is_transit=False):
    # Read the travel events
    if is_transit:
        travel_events = read_matsim_events_as_df(input_file_dir + 'output_events.xml.gz',
                                                 event_types='entered link,left link,Freight shipment delivered ends,Freight shipment pickup starts')
    else: 
        travel_events = read_matsim_events_as_df(input_file_dir + 'output_events.xml.gz',
                                             event_types='entered link,left link')
    return travel_events

def identify_shipment_event_seg(v_event: pd.DataFrame, time_threshold: int = 2 * 3600):
    ''' identify the end of a shipment event segment idx'''
    sub_df = v_event.query("type=='Freight shipment delivered ends'")
    # Copy the time column to a new column and move forward by one row
    sub_df['time_'] = sub_df['time'].shift(-1)
    # Calculate the time difference between the two columns
    sub_df['time_diff'] = sub_df['time_'] - sub_df['time']
    # Identify the index of the rows where the time difference is greater than the threshold
    end_time = sub_df[sub_df['time_diff'] > time_threshold]['time'].tolist()

    ''' identify the last shipment event segment idx'''
    last_shipment_delivered_time = sub_df['time'].tolist()[-1]

    ''' identiry the start of a shipment event segment idx'''
    sub_df = v_event.query("type=='Freight shipment pickup starts'")
    # Copy the time column to a new column and move forward by one row
    sub_df['time_'] = sub_df['time'].shift(1)
    # Calculate the time difference between the two columns
    sub_df['time_diff'] = sub_df['time'] - sub_df['time_']
    # Identify the index of the rows where the time difference is greater than the threshold
    start_time = sub_df[sub_df['time_diff'] > time_threshold]['time'].tolist()


    return end_time, start_time, last_shipment_delivered_time


def derive_vehicle_travel_chain(travel_events_df):
    if 'Freight shipment delivered ends' in travel_events_df['type'].unique():
        is_transit = True
    else:
        is_transit = False
    vehicles_travel_chain = {}
    vehicles_travel_time = {}
    for vehicle in travel_events_df['vehicle'].unique():
        sub_df = travel_events_df.query('vehicle == @vehicle')
        if is_transit:
            end_time, start_time, last_shipment_time = identify_shipment_event_seg(sub_df)
            sub_df = sub_df.query('time <= @last_shipment_time')
            for to_remove_seg_time_pair in zip(end_time, start_time):
                sub_df = sub_df[~sub_df['time'].between(to_remove_seg_time_pair[0], to_remove_seg_time_pair[1])]

        visited_links = []
        travel_time = 0
        prev_time = sub_df.iloc[0].time
        for idx, row in sub_df.iterrows():
            if len(visited_links) == 0:
                visited_links.append(row['link'])
            if row['link'] != visited_links[-1]: 
                visited_links.append(row['link'])
            if row['time'] <= prev_time + (20 * 60):
                travel_time += row['time'] - prev_time
            prev_time = row['time']
        vehicles_travel_chain[vehicle] = visited_links
        vehicles_travel_time[vehicle] = travel_time
    return vehicles_travel_chain, vehicles_travel_time

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
    vehicles_travel_chain, vehicles_travel_time = derive_vehicle_travel_chain(travel_events_df)
    vkt_dict = {}
    vtt_dict = {}
    for vehicle, v_travel_chain in vehicles_travel_chain.items():
        vkt = cal_vkt(v_travel_chain, network)
        vkt_dict[vehicle] = vkt
    for vehicle, v_travel_time in vehicles_travel_time.items():
        vtt_dict[vehicle] = v_travel_time
    return vkt_dict, vtt_dict


def write_vkt_to_csv(vkt_dict, output_dir, file_name='vkt.csv.gz', column_name='vkt'):
    vkt_df = pd.DataFrame.from_dict(vkt_dict, orient='index', columns=[column_name])
    vkt_df.reset_index(inplace=True)
    vkt_df.rename(columns={'index': 'vehicle'}, inplace=True)
    vkt_df.to_csv(output_dir+file_name,
                  index=False,
                  compression='gzip',
                  encoding='utf-8-sig')
    return vkt_df

def write_vtt_to_csv(vtt_dict, output_dir, file_name='vtt.csv.gz', column_name='vtt'):
    vtt_df = pd.DataFrame.from_dict(vtt_dict, orient='index', columns=[column_name])
    vtt_df.reset_index(inplace=True)
    vtt_df.rename(columns={'index': 'vehicle'}, inplace=True)
    vtt_df.to_csv(output_dir+file_name,
                  index=False,
                  compression='gzip',
                  encoding='utf-8-sig')
    return vtt_df
