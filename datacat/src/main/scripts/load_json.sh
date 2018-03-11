basename="lcio_engrun2015_"
if [ -n "$1" ]; then
  basename=$1
fi
for f in $(ls ${basename}*.txt); do
  echo "Loading JSON for $f ..."
  python ../load_json ${f} ${f%.*}.json
done
