import os
import numpy as np
import pandas as pd
import geopandas as gpd
import matsim
import matplotlib.pyplot as plt
import emission_events_anls as emission_anls
import travel_events_anls as travel_anls
import shipment_events_anls as shipment_anls
import tour_events_anls as tour_anls
import seaborn as sns
import utils
import logging
import pollutants

def write_events_stats(scenario_kw: str, iter_idx: int, network_path = 'GemeenteLeuvenWithHbefaType.xml.gz'):
    scenario = utils.set_scenario(scenario_kw)
    input_file_dir, output_dir = utils.get_paths(scenario, iter_idx)
    network = utils.read_matsim_network_as_gdf(utils.freight_emission_input_root+network_path)

    # Emissions
    emission_events_df = emission_anls.read_freight_emission_data(input_file_dir)
    all_pollutants_by_link_dict = emission_anls.get_all_pollutants_by_link_dict(emission_events_df)
    emission_summary_statistics = emission_anls.get_summary_statistics(all_pollutants_by_link_dict)
    emission_summary_statistics.to_csv(output_dir+'emission_summary_statistics.csv.gz',
                                    index=False,
                                    compression='gzip',
                                    encoding='utf-8-sig')

    # Travel VKT
    travel_events_df = travel_anls.read_travel_events(input_file_dir)
    vkt_dict = travel_anls.derive_all_vkt_as_dict(travel_events_df, network)
    vkt_df = travel_anls.write_vkt_to_csv(vkt_dict, output_dir)

    # Tour durations
    tour_events_df = tour_anls.read_tour_events(input_file_dir)
    tour_durations, vehicle_durations = tour_anls.calculate_tour_durations(tour_events_df)
    vehicle_duration_df = tour_anls.write_vehicle_durations_to_csv(vehicle_durations, output_dir)

    # Shipment demand
    shipment_events_df = shipment_anls.read_shipment_events(input_file_dir)
    vehicle_capacity_demand = shipment_anls.aggregate_vehicle_shipment_demand(shipment_events_df)
    vehicle_shipment_df = shipment_anls.write_vehicle_capacity_demand_to_csv(vehicle_capacity_demand, output_dir)

    # Aggregate vkt, durations and shipment dfs
    all_stats_df = vkt_df.merge(vehicle_duration_df, on='vehicle', how='left')
    all_stats_df = all_stats_df.merge(vehicle_shipment_df, on='vehicle', how='left')
    all_stats_df.to_csv(output_dir+'all_stats.csv.gz',
                        index=False,
                        compression='gzip',
                        encoding='utf-8-sig')

def write_all_events_stats(scenario_kw_list: list, iter_idx_list: list):
    for scenario_kw in scenario_kw_list:
        for iter_idx in iter_idx_list:
            logging.info(f'Processing {scenario_kw} iter {iter_idx}')
            write_events_stats(scenario_kw, iter_idx)

def load_single_scenario_stats(scenario_kw: str, iter_list: list):
    scenario = utils.set_scenario(scenario_kw)
    vkt_list = []
    travel_time_per_ton_list = []
    vkt_per_ton_list = []
    for iter_idx in iter_list:
        _, output_dir = utils.get_paths(scenario, iter_idx)
        all_stats_df = pd.read_csv(output_dir+'all_stats.csv.gz')
        # Calculate total VKT
        total_vkt = all_stats_df['vkt'].sum() / 1000  # km
        vkt_list.append(total_vkt)
        # Calculate travel time per ton
        all_stats_df['travel_time_per_ton'] = (all_stats_df['duration']/60)/(all_stats_df['capacityDemand']/1000)  # min/ton
        for _ in all_stats_df['travel_time_per_ton'].tolist():
            travel_time_per_ton_list.append(_)  
        # Calculate VKT per ton
        all_stats_df['vkt_per_ton'] = (all_stats_df['vkt']/1000) /(all_stats_df['capacityDemand']/1000)  # km/ton
        for _ in all_stats_df['vkt_per_ton'].tolist():
            vkt_per_ton_list.append(_)
    return {scenario_kw: {'vkt': vkt_list, 'travel_time_per_ton': travel_time_per_ton_list, 'vkt_per_ton': vkt_per_ton_list}}

def load_all_scenario_stats(scenario_kw_list: list, iter_list: list):
    all_stats_dict = {}
    for scenario_kw in scenario_kw_list:
        all_stats_dict.update(load_single_scenario_stats(scenario_kw, iter_list))
    return all_stats_dict

''' emissions '''
def load_single_scenario_emission_stats(scenario_kw: str, iter_list: list):
    scenario = utils.set_scenario(scenario_kw)
    emissions_dict = {}
    for idx, iter_idx in enumerate(iter_list):
        _, input_dir = utils.get_paths(scenario, iter_idx)
        emission_summary_statistics = pd.read_csv(input_dir+'emission_summary_statistics.csv.gz', index_col=0)
        detail_pollutant_list = []
        # Calculate each emissions
        for pollutant in emission_summary_statistics.index:
            if emissions_dict.get(pollutant) is None:
                emissions_dict[pollutant] = []
            emissions_dict[pollutant].append(emission_summary_statistics.loc[pollutant, 'sum'])
        air_quality_pollutants_sum = sum([emissions_dict.get(p)[idx] for p in pollutants.AIR_QUALITY_POLLUTANTS])
        if emissions_dict.get('air_quality_pollutants') is None:
            emissions_dict['air_quality_pollutants'] = []
        emissions_dict['air_quality_pollutants'].append(air_quality_pollutants_sum)
    return emissions_dict

def load_all_scenario_emission_stats(scenario_kw_list: list, iter_list: list):
    all_emissions_dict = {}
    for scenario_kw in scenario_kw_list:
        all_emissions_dict[scenario_kw]=load_single_scenario_emission_stats(scenario_kw, iter_list)
    return all_emissions_dict
