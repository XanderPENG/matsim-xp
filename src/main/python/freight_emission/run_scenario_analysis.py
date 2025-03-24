import logging
import freight_emissions_anls as fea

logging.basicConfig(level=logging.INFO)

scenario_kw_list = ['basic','van', 'cb']
iter_idx_list = list(range(300, 400))

fea.write_all_events_stats(scenario_kw_list, iter_idx_list)

        








