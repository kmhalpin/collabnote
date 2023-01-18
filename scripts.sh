# Clean Each Iteration
grep -v churn client/build/results/jmh/human.txt | grep Iteration -A 7 |\
grep -v Warmup | xargs -L 8 |\
sed -r 's/Iteration (.*): (.*\..*) ms\/op memory\.total: (.*\..*) B memory\.usage: (.*\..*) B \?gc\.alloc\.rate: (.*\..*) MB\/sec \?gc\.alloc\.rate\.norm: (.*\..*) B\/op \?gc\.count: (.*\..*) counts \?gc\.time: (.*\..*) ms.*/\1, \2, \3, \4, \5, \6, \7, \8/' >\
client/build/results/jmh/res-1.csv