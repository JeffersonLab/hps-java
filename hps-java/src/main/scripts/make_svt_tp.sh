#!/bin/bash
echo "# default values for Monte Carlo"
for fpga in `seq 0 7`
do
	for hyb in `seq 0 2`
	do
			for chan in `seq 0 639`
			do
				echo "$fpga	$hyb	$chan	2500.0	35.0	50.0	289.0"
			done
	done
done
