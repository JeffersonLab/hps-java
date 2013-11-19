#!/bin/bash
echo "# default values for Monte Carlo"
for fpga in `seq 0 7`
do
	for hyb in `seq 0 2`
	do
			for chan in `seq 0 639`
			do
				echo "$fpga	$hyb	$chan	0.09	100.0"
			done
	done
done
