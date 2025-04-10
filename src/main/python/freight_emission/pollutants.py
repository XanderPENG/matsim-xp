
BC_exhaust = 'BC_exhaust'
BC_non_exhaust = 'BC_non_exhaust'
Benzene = 'Benzene'
CH4 = 'CH4'
CO = 'CO'
CO2_TOTAL = 'CO2_TOTAL'
CO2_rep = 'CO2_rep'
CO2e = 'CO2e'
FC = 'FC'
FC_MJ = 'FC_MJ'
HC = 'HC'
N2O = 'N2O'
NH3 = 'NH3'
NMHC = 'NMHC'
NO2 = 'NO2'
NOx = 'NOx'
PM = 'PM'
PM2_5 = 'PM2_5'
PM2_5_non_exhaust = 'PM2_5_non_exhaust'
PM_non_exhaust = 'PM_non_exhaust'
PN = 'PN'
Pb = 'Pb'
SO2 = 'SO2'

POLLUTANTS: set = {'BC_exhaust',
                'BC_non_exhaust',
                'Benzene',
                'CH4',
                 'CO',
                'CO2_TOTAL',
                'CO2_rep',
                'CO2e',
                'FC',
                'FC_MJ',
                'HC',
                'N2O',
                'NH3',
                'NMHC',
                'NO2',
                'NOx',
                'PM',
                'PM2_5',
                'PM2_5_non_exhaust',
                'PM_non_exhaust',
                'PN',
                'Pb',
                'SO2'}

GREENHOUSE_POLLUTANT = CO2e
AIR_QUALITY_POLLUTANTS = {CO, NOx, NO2,
                          PM_non_exhaust, PM2_5, PM2_5_non_exhaust,
                          SO2, NH3, Pb, Benzene,
                          HC, NMHC}
PM_total = {PM_non_exhaust, PM}
PM2_5_total = {PM2_5, PM2_5_non_exhaust}

EPI_POLLUTANTS = {
                CO: 2.9,
                SO2: 2.9,
                NOx: 5.9,
                NO2: 5.9,
                PM2_5: 38.2,
                PM2_5_non_exhaust: 38.2,
            }

WEIGHTED_AQI_POLLUTANTS = { 
                SO2: 1,
                NO2: 2.5,
                PM: 5,
                PM_non_exhaust: 5,
                PM2_5: 10,
                PM2_5_non_exhaust: 10,
            }

