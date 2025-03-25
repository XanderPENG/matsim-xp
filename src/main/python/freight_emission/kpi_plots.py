import numpy as np
import matplotlib.pyplot as plt 
import os
import scipy.stats as stats
# Some utils modules
import pollutants
import freight_emissions_anls as fea


''' Plot utils '''
def plot_stat_comparison(
    bike_summary,
    car_summary,
    no_policy_summary,
    xlabel,
    figure_folder,
    filename=None,
    colors=["darkgrey", "steelblue", "#A5D6A7"],
    alphas=[0.8, 0.6, 0.8],
    figure_size=(10, 6),
    is_fitting=False,
    n_bins=60,
):
    fig, ax = plt.subplots(figsize=figure_size, dpi=300)
    min_val = min(bike_summary.min(), car_summary.min(), no_policy_summary.min())
    max_val = max(bike_summary.max(), car_summary.max(), no_policy_summary.max())

    bins = np.linspace(min_val, max_val, n_bins)
    
    # Plot histograms

    # 绘制非归一化直方图（显示频数）
    plt.hist(no_policy_summary, bins=bins, color=colors[0], 
                                alpha=alphas[0], label="Van")
    plt.hist(car_summary, bins=bins, color=colors[1], alpha=alphas[1], 
            label="Van-Circulation")
    plt.hist(bike_summary, bins=bins, color=colors[2], alpha=alphas[2], 
            label="Cargo Bike-Circulation")
    if is_fitting:
        # bin宽度
        bin_width = bins[1] - bins[0]
        xmin, xmax = plt.xlim()
        x = np.linspace(xmin, xmax, 100)

        # 对 no_policy_summary 拟合正态分布，并调整曲线高度
        mu_van, std_van = stats.norm.fit(no_policy_summary)
        p_van = stats.norm.pdf(x, mu_van, std_van) * len(no_policy_summary) * bin_width
        plt.plot(x, p_van, color=colors[0], linewidth=2, label="Van Fitting")

        # 对 car_summary 拟合正态分布，并调整曲线高度
        mu_car, std_car = stats.norm.fit(car_summary)
        p_car = stats.norm.pdf(x, mu_car, std_car) * len(car_summary) * bin_width
        plt.plot(x, p_car, color=colors[1], linewidth=2, label="Van-Circulation Fitting")

        # 对 bike_summary 拟合正态分布，并调整曲线高度
        mu_bike, std_bike = stats.norm.fit(bike_summary)
        p_bike = stats.norm.pdf(x, mu_bike, std_bike) * len(bike_summary) * bin_width
        plt.plot(x, p_bike, color=colors[2], linewidth=2, label="Cargo Bike-Circulation Fitting")

    # Hide the right and top spines
    plt.gca().spines["right"].set_visible(False)
    plt.gca().spines["top"].set_visible(False)

    plt.xlabel(xlabel, fontsize=12, fontweight="bold")
    plt.ylabel("Density", fontsize=12, fontweight="bold")
    plt.xticks(fontsize=10)
    plt.yticks(fontsize=10)
    plt.tight_layout()
    plt.savefig(figure_folder + filename, dpi=350)
    # plt.show()


def plot_stat_comparison_two_groups(
    car_summary,
    no_policy_summary,
    xlabel,
    figure_folder,
    filename=None,
    colors=["darkgrey", "steelblue"],
    alphas=[0.8, 0.6],
    figure_size=(10, 6),
):
    fig, ax = plt.subplots(figsize=figure_size, dpi=300)
    min_val = min(car_summary.min(), no_policy_summary.min())
    max_val = max(car_summary.max(), no_policy_summary.max())

    bins = np.linspace(min_val, max_val, 11)

    plt.hist(
        no_policy_summary, bins=bins, color=colors[0], alpha=alphas[0], label="Van"
    )
    plt.hist(
        car_summary,
        bins=bins,
        color=colors[1],
        alpha=alphas[1],
        label="Van-Circulation",
    )

    # Hide the right and top spines
    plt.gca().spines["right"].set_visible(False)
    plt.gca().spines["top"].set_visible(False)

    plt.xlabel(xlabel, fontsize=12, fontweight="bold")
    plt.ylabel("Density", fontsize=12, fontweight="bold")
    plt.xticks(fontsize=10)
    plt.yticks(fontsize=10)
    plt.tight_layout()
    plt.savefig(figure_folder + filename)
    # plt.show()

if __name__ == '__main__':
    ''' Some settings '''
    iter_list = list(range(300, 400))
    scenario_kw_list = ['basic', 'van', 'cb']
    figure_folder = r'../../../../figures/freightEmissions/KPIs/'
    os.makedirs(figure_folder, exist_ok=True)

    plt.rcParams["font.sans-serif"] = "Arial"

    # Load all the scenario stats
    all_scenario_stats = fea.load_all_scenario_stats(scenario_kw_list=scenario_kw_list, iter_list=iter_list)
    all_scenario_emissions = fea.load_all_scenario_emission_stats(scenario_kw_list=scenario_kw_list, iter_list=iter_list)

    ''' VKT '''
    metric = 'vkt'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        bike_summary=np.array(all_scenario_stats['cb'][metric]),
        figure_folder=figure_folder,
        filename='vkt.png',
        xlabel='VKT (km)',
        figure_size=(3.9, 2.6)
    )

    # Only compare van and no policy
    metric = 'vkt'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        figure_folder=figure_folder,
        filename='vkt_van_no_policy.png',
        xlabel='VKT (km)',
    )

    ''' Transit VKT '''
    metric = 'transit_vkt'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        bike_summary=np.array(all_scenario_stats['cb'][metric]),
        figure_folder=figure_folder,
        filename='transit_vkt.png',
        xlabel='Transit VKT (km)',
        figure_size=(3.9, 2.6)
    )

    # Only compare van and no policy
    metric = 'transit_vkt'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        figure_folder=figure_folder,
        filename='transit_vkt_van_no_policy.png',
        xlabel='Transit VKT (km)',
    )

    ''' Transit Time '''
    metric = 'total_transit_time'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        bike_summary=np.array(all_scenario_stats['cb'][metric]),
        figure_folder=figure_folder,
        filename='transit_time.png',
        xlabel='Transit Time (min)',
        figure_size=(3.9, 2.6)
    )

    # Only compare van and no policy
    metric = 'total_transit_time'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        figure_folder=figure_folder,
        filename='transit_time_van_no_policy.png',
        xlabel='Transit Time (min)',
    )

    ''' Ton-km traveled '''
    metric = 'ton_km_traveled'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        bike_summary=np.array(all_scenario_stats['cb'][metric]),
        figure_folder=figure_folder,
        filename='ton_km_traveled.png',
        xlabel='Ton-km traveled (ton-km)',
        figure_size=(3.9, 2.6)
    )

    # Only compare van and no policy
    metric = 'ton_km_traveled'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        figure_folder=figure_folder,
        filename='ton_km_traveled_van_no_policy.png',
        xlabel='Ton-km traveled (ton-km)',
    )

    ''' Transit Time per Ton '''
    metric = 'transit_vkt_per_ton'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        bike_summary=np.array(all_scenario_stats['cb'][metric]),
        figure_folder=figure_folder,
        filename='transit_vkt_per_ton.png',
        xlabel='Transit VKT per ton (Km/ton)',
    )

    # Only compare van and no policy
    metric = 'transit_vkt_per_ton'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        figure_folder=figure_folder,
        filename='transit_vkt_per_ton_van_no_policy.png',
        xlabel='Transit VKT per ton (Km/ton)',
    )

    ''' Transit Time per Ton '''
    metric = 'transit_time_per_ton'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        bike_summary=np.array(all_scenario_stats['cb'][metric]),
        figure_folder=figure_folder,
        filename='transit_time_per_ton.png',
        xlabel='Transit Time per ton (min/ton)',
    )

    # Only compare van and no policy
    metric = 'transit_time_per_ton'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        figure_folder=figure_folder,
        filename='transit_time_per_ton_van_no_policy.png',
        xlabel='Transit Time per ton (min/ton)',
    )

    ''' Number of vehicles '''
    metric = 'number_of_vehicle'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        bike_summary=np.array(all_scenario_stats['cb'][metric]),
        figure_folder=figure_folder,
        filename='number_of_vehicle.png',
        xlabel='Number of vehicles',
    )

    # Only compare van and no policy
    metric = 'number_of_vehicle'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_stats['basic'][metric]),
        car_summary=np.array(all_scenario_stats['van'][metric]),
        figure_folder=figure_folder,
        filename='number_of_vehicle_van_no_policy.png',
        xlabel='Number of vehicles',
    )

    ''' Pollutants-CO2e'''
    metric = pollutants.CO2e
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        bike_summary=np.array(all_scenario_emissions['cb'][metric]),
        figure_folder=figure_folder,
        filename='CO2e.png',
        xlabel='WTW CO2-eq emissions (g)',
        figure_size=(3.9, 2.6)
    )

    # Only compare van and no policy
    metric = pollutants.CO2e
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        figure_folder=figure_folder,
        filename='CO2e_van_no_policy.png',
        xlabel='WTW CO2-eq emissions (g)',
    )

    ''' Pollutants-air quality '''
    metric = 'air_quality_pollutants'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        bike_summary=np.array(all_scenario_emissions['cb'][metric]),
        figure_folder=figure_folder,
        filename='air_quality.png',
        xlabel='Air quality emissions (g)',
    )

    # Only compare van and no policy
    metric = 'air_quality_pollutants'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        figure_folder=figure_folder,
        filename='air_quality_van_no_policy.png',
        xlabel='Air quality emissions (g)',
    )

    ''' EPI '''
    metric = 'EPI'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        bike_summary=np.array(all_scenario_emissions['cb'][metric]),
        figure_folder=figure_folder,
        filename='EPI.png',
        xlabel='EPI',
    )

    # Only compare van and no policy
    metric = 'EPI'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        figure_folder=figure_folder,
        filename='EPI_van_no_policy.png',
        xlabel='EPI',
    )

    ''' Weighted AQI '''
    metric = 'weighted_AQI'
    plot_stat_comparison(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        bike_summary=np.array(all_scenario_emissions['cb'][metric]),
        figure_folder=figure_folder,
        filename='weighted_AQI.png',
        xlabel='Weighted AQI',
    )

    # Only compare van and no policy
    metric = 'weighted_AQI'
    plot_stat_comparison_two_groups(
        no_policy_summary=np.array(all_scenario_emissions['basic'][metric]),
        car_summary=np.array(all_scenario_emissions['van'][metric]),
        figure_folder=figure_folder,
        filename='weighted_AQI_van_no_policy.png',
        xlabel='Weighted AQI',)

    print('Done!')
