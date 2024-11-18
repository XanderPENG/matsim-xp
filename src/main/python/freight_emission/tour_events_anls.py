from utils import read_matsim_events_as_df
import pandas as pd
import geopandas as gpd

def read_tour_events(input_file_dir):
    # Read the tour events
    tour_events = read_matsim_events_as_df(input_file_dir + 'output_events.xml.gz',
                                           event_types='Freight tour starts,Freight tour ends')
    return tour_events

def calculate_tour_durations(df):

    tour_durations = {}

    grouped = df.groupby(['vehicle', 'tourId'])


    for (vehicle, tourId), group in grouped:
   
        if 'Freight tour starts' in group['type'].values and 'Freight tour ends' in group['type'].values:
            start_time = group[group['type'] == 'Freight tour starts']['time'].values[0]
            end_time = group[group['type'] == 'Freight tour ends']['time'].values[0]
            duration = end_time - start_time
            
            tour_durations[(vehicle, tourId)] = duration

    # Calculate the total duration of each vehicle
    vehicle_durations = {}
    for (vehicle, tourId), duration in tour_durations.items():
        if vehicle in vehicle_durations:
            vehicle_durations[vehicle] += duration
        else:
            vehicle_durations[vehicle] = duration

    return tour_durations, vehicle_durations

def write_vehicle_durations_to_csv(vehicle_durations, output_dir):
    vehicle_durations_df = pd.DataFrame.from_dict(vehicle_durations, orient='index', columns=['duration'])
    vehicle_durations_df.reset_index(inplace=True)
    vehicle_durations_df.rename(columns={'index': 'vehicle'}, inplace=True)
    vehicle_durations_df.to_csv(output_dir+'vehicle_durations.csv.gz',
                                index=False,
                                compression='gzip',
                                encoding='utf-8-sig')
    return vehicle_durations_df