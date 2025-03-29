import logging
import freight_emissions_anls as fea

logging.basicConfig(level=logging.INFO)

scenario_kw_list = [
    # 'basic',
    # 'van', 
    # 'cb'
    ]
# The SA-scenario names must be the same as the folder names.
sa_scenario_kw_list = ['VanSA2t', 'CBSA80kg', 'CBSA100kg', 'CBSA150kg', 'CBSA200kg']  
iter_idx_list = list(range(300, 400))

fea.write_all_events_stats(scenario_kw_list+sa_scenario_kw_list, iter_idx_list)

        








