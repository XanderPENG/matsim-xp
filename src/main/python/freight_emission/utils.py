import os

freight_emission_input_root = r'../../../../data/intermediate/test/freightEmissions/'
freight_emission_output_root = r'../../../../data/clean/freightEmissions/'
van_scenario = 'scenarioVan/'
cb_scenario = 'scenarioCB/'

def set_scenario(scenario_kw):
    if scenario_kw == 'van':
        scenario = van_scenario
    elif scenario_kw == 'cb':
        scenario = cb_scenario
    else:
        raise ValueError('scenario_kw must be either "van" or "cb"')
    return scenario

def get_paths(scenario, iter_idx):
    input_file_dir = freight_emission_input_root + scenario + 'iter' + str(iter_idx) + '/outputs/'
    output_dir = freight_emission_output_root + scenario + 'iter' + str(iter_idx) + '//'

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    return input_file_dir, output_dir
