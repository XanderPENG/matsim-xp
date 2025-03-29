import os
import pandas as pd
import matsim


freight_emission_input_root = r'../../../../data/intermediate/test/freightEmissions/'
freight_emission_output_root = r'../../../../data/clean/freightEmissions/'
basic_scenario = 'scenarioBasic/'
van_scenario = 'scenarioVan/'
cb_scenario = 'scenarioCB/'

def set_scenario(scenario_kw: str):
    if scenario_kw.lower() in ['van', 'basic']:
        scenario = f'scenario{scenario_kw.capitalize()}/'
    elif scenario_kw.lower() == 'cb':
        scenario = f'scenario{scenario_kw.upper()}/'
    elif 'sa' in scenario_kw.lower():
        scenario = f'scenario{scenario_kw}/' 
    else:
        raise ValueError('scenario_kw must be either "van" or "cb" or "basic"')
    return scenario

def get_paths(scenario, iter_idx):
    input_file_dir = freight_emission_input_root + scenario + 'iter' + str(iter_idx) + '/outputs/'
    output_dir = freight_emission_output_root + scenario + 'iter' + str(iter_idx) + '//'

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    return input_file_dir, output_dir

def read_matsim_events_as_df(file_path: str, event_types:str):
    events = matsim.event_reader(file_path, types=event_types)
    # Get event keys
    event_keys = set()
    events_list = []
    for event in events:
        events_list.append(event)
        current_keys = list(event.keys())
        event_keys.update(current_keys)
    # Store events in a dict
    events_dict = {}
    for idx, event in enumerate(events_list):
        event_dict = {}
        for key in event_keys:
            if key in event.keys():
                event_dict[key] = event[key]
            else:
                event_dict[key] = None
        events_dict[idx] = event_dict
    # Convert to DataFrame
    events_df = pd.DataFrame.from_dict(events_dict, orient='index')
    # Convert columns which is number-like str into float and ignore the rest
    for column in events_df.columns:
        try:
            events_df[column] = events_df[column].astype(float)
        except:
            pass
    return events_df

def read_matsim_network_as_gdf(file_path: str = freight_emission_input_root+'GemeenteLeuvenWithHbefaType.xml.gz'):
    network = matsim.read_network(file_path)
    return network.as_geo()
