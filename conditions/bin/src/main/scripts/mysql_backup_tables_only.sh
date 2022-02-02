#!/bin/sh
mysqldump -d -p2jumpinphotons. -h mysql-node03.slac.stanford.edu -P 3306 -u rd_hps_cond_ro rd_hps_cond \
beam_current \
conditions_dev \
ecal_bad_channels \
ecal_calibrations \
ecal_channels \
ecal_gains \
ecal_time_shifts \
svt_bad_channels \
svt_bad_channels_scratch \
svt_calibrations \
svt_channels \
svt_daq_map \
svt_gains \
svt_pulse_parameters \
svt_time_shifts > conditions_database_tables_only.sql 
