#!/bin/bash
#prints the ECal physicalID-daqID conversion table, as found in daqmap/ecal.txt in hps-detectors.
echo "#x	y	crate	slot	channel"
topslots=( 10 13 9 14 8 15 7 16 6 17 5 18 4 3 )
botslots=( 10 13 9 14 8 15 7 16 6 17 5 18 4 19 )
x=-23
y=1
slot=0
channel=0
while true
do
    if [ "$x" -ge 0 ]
    then
    	echo "$((24-x))	$y	1	${topslots[slot]}	$channel"
    else
        echo "$x	$y	1	${topslots[slot]}	$channel"
    fi
    echo "$x	-$y	2	${botslots[slot]}	$channel"
	channel=$((channel+1))
	if [ "$channel" -eq 16 ]
	then
		channel=0
		slot=$((slot+1))
	fi
	y=$((y+1))
	if [ "$y" -eq 6 ]
	then
		y=1
		x=$((x+1))
		if [ "$x" -eq 0 ]
		then
			x=1
		fi
		if [ "$x" -eq 24 ]
		then
			exit
		fi
	fi
	if [ "$y" -eq 1 ] && [ "$x" -le -2 ] && [ "$x" -ge -10 ]
	then
		y=2
	fi
done
