"""
Author: Xander Peng
Date: 2024/11/12
Description: 
"""
import utils
import emission_events_anls as eea
import logging

logging.basicConfig(level=logging.INFO)

scenario = utils.set_scenario('van')
input_file_dir, output_dir = utils.get_paths(scenario, 0)
emission_events_df = eea.read_freight_emission_data(input_file_dir)
all_pollutants_by_link_dict = eea.get_all_pollutants_by_link_dict(emission_events_df)
emission_summary_statistics = eea.get_summary_statistics(all_pollutants_by_link_dict)
emission_summary_statistics.to_csv(output_dir+'emission_summary_statistics.csv.gz',
                                   index=False,
                                   compression='gzip',
                                   encoding='utf-8-sig')



