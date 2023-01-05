# Pull Each Iteration
grep Iteration -A 3 client/build/results/jmh/human.txt |\
grep -v Warmup | sed 's/$/,/' | xargs -L 5 | sed 's/,$//' >\
client/build/results/jmh/res-1.txt