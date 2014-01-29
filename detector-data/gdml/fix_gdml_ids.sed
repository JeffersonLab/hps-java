#!/bin/sed -f
s/<volume name="\([^"]\+\)"/<volume name="\1_vol"/
s/<volumeref ref="\([^"]\+\)"/<volumeref ref="\1_vol"/

s/<physvol name="\([^"]\+\)"/<physvol name="\1_phys"/
     
#s/<solid name="/<solid name="sol_/
#s/<solidref ref="/<solidref ref="sol_/
#s/<physvol name="/<physvol name="phys_/
#s/position name="[^"]\+"/position/
#s/rotation name="[^"]\+"/rotation/
#s/<materialref ref="[^"]\+"/<materialref ref="Air"/
