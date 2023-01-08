# Clean Each Iteration
grep Iteration -A 3 client/build/results/jmh/human.txt |\
grep -v Warmup | xargs -L 4 |\
sed -r 's/Iteration (.*): (.*\..*) ms\/op memory\.total: (.*\..*) B memory\.usage: (.*\..*) B.*/\1, \2, \3, \4/' >\
client/build/results/jmh/res-1.csv