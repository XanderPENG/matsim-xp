import os
import numpy as np
import pandas as pd

''' Some utils '''
def check_and_fill_warm_emission_factors(df, vehicle_type='eBike', field_name='V_100%'):

    sub_df = df[df['Subsegment'] == vehicle_type]

    full_ef = df['Component'].unique()
    full_traffic_sit = df['TrafficSit'].unique()

    full_fields_list = df.columns.tolist()
    idx_V = full_fields_list.index(field_name)
    post_fields_list = full_fields_list[idx_V:]

    if sub_df.empty:
        raise ValueError(f"No records found for vehicle type '{vehicle_type}'")
    sample_eBike_record = sub_df.iloc[0]

    # create new records
    new_records = []
    for ef in full_ef:
        for ts in full_traffic_sit:
            if not ((sub_df['Component'] == ef) & (sub_df['TrafficSit'] == ts)).any():
                
                new_record = sample_eBike_record.to_dict()
                new_record['Component'] = ef
                new_record['TrafficSit'] = ts
                for field in post_fields_list:
                    new_record[field] = 0
                new_records.append(new_record)

    # Add new records to the dataframe
    if new_records:
        new_records_df = pd.DataFrame(new_records)
        df = pd.concat([df, new_records_df], ignore_index=True)

    return df

def check_and_fill_cold_emission_factors(df, vehicle_type='LCV petrol N1-III Euro-6c', field_name='%OfSubsegment'):

    sub_df = df[df['Subsegment'] == vehicle_type]

    full_ef = df['Component'].unique()
    full_traffic_sit = df['AmbientCondPattern'].unique()

    full_fields_list = df.columns.tolist()
    idx_V = full_fields_list.index(field_name)
    post_fields_list = full_fields_list[idx_V:]

    if sub_df.empty:
        raise ValueError(f"No records found for vehicle type '{vehicle_type}'")
    sample_eBike_record = sub_df.iloc[0]

    # create new records
    new_records = []
    for ef in full_ef:
        for ts in full_traffic_sit:
            if not ((sub_df['Component'] == ef) & (sub_df['AmbientCondPattern'] == ts)).any():
                
                new_record = sample_eBike_record.to_dict()
                new_record['Component'] = ef
                new_record['AmbientCondPattern'] = ts
                for field in post_fields_list:
                    new_record[field] = 0
                new_records.append(new_record)

    # Add new records to the dataframe
    if new_records:
        new_records_df = pd.DataFrame(new_records)
        df = pd.concat([df, new_records_df], ignore_index=True)

    return df

def check_and_fill_cold_emission_factorsV2(df, vehicle_type='LCV petrol N1-III Euro-6c', field_name='%OfSubsegment'):
    
    sub_df = df[df['Subsegment'] == vehicle_type]

    full_ef = df['Component'].unique()
    full_traffic_sit = df['AmbientCondPattern'].unique()

    full_fields_list = df.columns.tolist()
    idx_V = full_fields_list.index(field_name)
    post_fields_list = full_fields_list[idx_V:]

    if sub_df.empty:
        raise ValueError(f"No records found for vehicle type '{vehicle_type}'")
    sample_eBike_record = sub_df.iloc[0]

    # create new records
    new_records = []
    for ef in full_ef:
        for ts in full_traffic_sit:
            if not ((sub_df['Component'] == ef) & (sub_df['AmbientCondPattern'] == ts)).any():
                
                new_record = sample_eBike_record.to_dict()
                new_record['Component'] = ef
                new_record['AmbientCondPattern'] = ts
                for field in post_fields_list:
                    new_record[field] = 0
                new_records.append(new_record)
            

    # Add new records to the dataframe
    if new_records:
        new_records_df = pd.DataFrame(new_records)
        df = pd.concat([df, new_records_df], ignore_index=True)

    # Add records with lack of hours
    new_records = []
    sub_df = df[df['Subsegment'] == vehicle_type]
    lack_hours = [6, 7, 8, 9, 10, 11, 12]
    for ef in full_ef:
        ## Sample records
        distance_01 = sub_df[sub_df['AmbientCondPattern'].str.contains('4-5h,0-1km') & (sub_df['Component'] == ef)]
        assert len(distance_01) == 1
        distance_12 = sub_df[sub_df['AmbientCondPattern'].str.contains('4-5h,1-2km') & (sub_df['Component'] == ef)]
        assert len(distance_12) == 1
        sample_prefix = sub_df.iloc[0]['AmbientCondPattern'].split(',')[0]
        for h in lack_hours:
            if not ((sub_df['Component'] == ef) & (sub_df['AmbientCondPattern'] == f'{sample_prefix},{h-1}-{h}h,0-1km')).any():
                new_record = distance_01.iloc[0].to_dict()
                new_record['AmbientCondPattern'] = f'{sample_prefix},{h-1}-{h}h,0-1km'
                new_records.append(new_record)
            if not ((sub_df['Component'] == ef) & (sub_df['AmbientCondPattern'] == f'{sample_prefix},{h-1}-{h}h,1-2km')).any():
                new_record = distance_12.iloc[0].to_dict()
                new_record['AmbientCondPattern'] = f'{sample_prefix},{h-1}-{h}h,1-2km'
                new_records.append(new_record)
    if new_records:
        new_records_df = pd.DataFrame(new_records)
        df = pd.concat([df, new_records_df], ignore_index=True)
    return df

def convert_eBike_sizeClass(row: pd.Series, subsegment='eBike'):
    if row['SizeClasse'] == 'not specified' and row['Subsegment'] == subsegment:
        return 'NA' 
    else:
        return row['SizeClasse']       

def replace_efa4co2e(row: pd.Series):
    if row['Component'] == 'CO2e':
        return row['EFA_WTW']
    else:
        return row['EFA']

''' Main '''
if __name__ == '__main__':
    raw_data_path = r'../../../../data/raw/freight_emission/'
    processed_data_path = r'../../../../data/intermediate/test/freightEmissions/'
    
    LCV_MODEL = 'LCV petrol N1-III Euro-6c'
    CB_MODEL = 'eScooter'

    filenames = list(filter(lambda x: '.XLSX' in x, os.listdir(raw_data_path)))

    ''' Add lack hours for cold emission factors '''
    for filename in filenames:
        df = pd.read_excel(raw_data_path + filename)
        # Remove rows with error region/country info
        df = df.query("AmbientCondPattern != 'ØGermany'")
        if df.empty:
            pass
        else:
            print(f"Processing file {filename}")
            # Fill missing emission factors
            if 'Cold' in filename and 'MC' in filename:
                df = check_and_fill_cold_emission_factorsV2(df, vehicle_type=CB_MODEL)
            elif 'Cold' in filename and 'LCV' in filename:
                df = check_and_fill_cold_emission_factorsV2(df, vehicle_type=LCV_MODEL)
            elif 'HOT' in filename and 'MC' in filename:
                df = check_and_fill_warm_emission_factors(df, vehicle_type=CB_MODEL)
            elif 'HOT' in filename and 'LCV' in filename:
                df = check_and_fill_warm_emission_factors(df, vehicle_type=LCV_MODEL)
            else:
                # Average table
                pass
            
        if 'average' not in filename:
            # replace the value of 'EFA' with 'EFA_WTW' for emission factor - CO2e
            df['EFA'] = df.apply(lambda x: replace_efa4co2e(x), axis=1)

        if 'MC' in filename and not df.query("Subsegment == @CB_MODEL").empty:
            # Convert the SizeClasse for CB
            df['SizeClasse'] = df.apply(lambda x: convert_eBike_sizeClass(x, CB_MODEL), axis=1)

        # check the consistency of the emission factors, and then fill the missing factors.
        df.to_csv(processed_data_path + filename[:-5] + "V1.csv.gz", sep=';', index=False, compression='gzip', encoding='utf-8-sig')
        print(f"File {filename} processed and saved to {processed_data_path + filename[:-5] + 'V1.csv.gz'}")