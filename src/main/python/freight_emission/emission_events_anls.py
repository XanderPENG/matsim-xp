import pandas as pd
import matsim
import pollutants
import logging

def read_freight_emission_data(input_file_dir, filter_threshold=3*60):
    logging.info("Reading the freight emission data...")
    # Read the freight emission events
    freight_emission_events = matsim.event_reader(input_file_dir + 'output_events.xml.gz',
                                                  types='coldEmissionEvent,warmEmissionEvent')
    # Convert to DataFrame
    ## Get all the emission event keys
    event_keys = set()
    events_list = []
    for emission_event in freight_emission_events:
        events_list.append(emission_event)
        current_keys = list(emission_event.keys())
        event_keys.update(current_keys)

    events_dict = {}
    for idx, event in enumerate(events_list):
        event_dict = {}
        for key in event_keys:
            if key in event.keys():
                event_dict[key] = event[key]
            else:
                event_dict[key] = None
        events_dict[idx] = event_dict

    events_df = pd.DataFrame.from_dict(events_dict, orient='index')

    # Convert columns which is number-like str into float and ignore the rest
    for column in events_df.columns:
        try:
            events_df[column] = events_df[column].astype(float)
        except:
            pass
    logging.info("Data reading completed.")
    events_df = filter_cold_emission_events(events_df, filter_threshold)
    return events_df

def aggregate_pollutant_by_link(events_df, pollutant: str):
    assert pollutant in pollutants.POLLUTANTS, f"Pollutant {pollutant} is not supported."
    logging.info(f"Aggregating pollutant {pollutant} by link...")

    # Aggregate the pollutant by link
    if pollutant in events_df.columns:
        pollutant_by_link = events_df.pivot_table(index='linkId',
                                                  columns='type',
                                                  values=pollutant,
                                                  aggfunc='sum')
        pollutant_by_link = pollutant_by_link.fillna(0)
        pollutant_by_link['sum'] = pollutant_by_link[['coldEmissionEvent', 'warmEmissionEvent']].sum(axis=1)
        return pollutant_by_link

def get_all_pollutants_by_link_dict(events_df):
    logging.info("Getting all pollutants by link...")
    pollutants_by_link_dict = {}
    for pollutant in pollutants.POLLUTANTS:
        pollutant_by_link = aggregate_pollutant_by_link(events_df, pollutant)
        pollutants_by_link_dict[pollutant] = pollutant_by_link
    return pollutants_by_link_dict

def get_summary_statistics(pollutants_by_link_dict) -> pd.DataFrame:
    logging.info("Getting summary statistics...")
    summary_statistics = {}
    for pollutant, pollutant_by_link in pollutants_by_link_dict.items():
        summary_statistics[pollutant] = pollutant_by_link['sum'].sum() if pollutant_by_link is not None else 0
    summary_statistics_df = pd.DataFrame.from_dict(summary_statistics, orient='index', columns=['sum'])
    summary_statistics_df.reset_index(inplace=True)
    summary_statistics_df.rename(columns={'index': 'pollutant'}, inplace=True)
    return summary_statistics_df


# Some utils for the analysis
def filter_incorrect_cold_emission_events(cold_emission_events_df, threshold=5*60):
    incorrect_records_idx = []
    previous_time = 0
    for idx, row in cold_emission_events_df.iterrows():
        if idx == 0:
            previous_time = row['time']
            continue
        else:
            current_time = row['time']
            if current_time-previous_time <= threshold:
                incorrect_records_idx.append(idx)
            else:
                pass
            previous_time = current_time
    return cold_emission_events_df.query('index not in @incorrect_records_idx')

def filter_cold_emission_events(events_df, threshold=5*60):
    origin_cold_events = events_df.query('type == "coldEmissionEvent"')
    origin_warm_events = events_df.query('type == "warmEmissionEvent"')
    vehicle_list = origin_cold_events['vehicleId'].unique().tolist()
    filtered_cold_events = []
    for vehicle in vehicle_list:
        sub_df = origin_cold_events.query('vehicleId == @vehicle')
        filtered_sub_df = filter_incorrect_cold_emission_events(sub_df, threshold)
        filtered_cold_events.append(filtered_sub_df)
    filtered_cold_events.append(origin_warm_events)
    return pd.concat(filtered_cold_events)
