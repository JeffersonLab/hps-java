basename="lcio_engrun2015_"
if [ -n "$1" ]; then
  basename=$1
fi
for f in $(ls ${basename}*.txt); do
  echo "Making JSON for $f ..."
  python ../make_lcio_sim_json.py ${f} ${f%.*}.json
done
