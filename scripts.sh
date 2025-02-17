# choose one

# Clean Each Iteration with GC
# grep -v churn client/build/results/jmh/human.txt | grep Iteration -A 7 |\
# grep -v Warmup | xargs -L 7 |\
# sed -r 's/Iteration (.*): (.*\..*) ms\/op memory\.total: (.*\..*) B memory\.usage: (.*\..*) B ·gc\.alloc\.rate: (.*\..*) MB\/sec ·gc\.alloc\.rate\.norm: (.*\..*) B\/op ·gc\.count: (.*\..*) counts ·gc\.time: (.*\..*) ms.*/\1, \2, \3, \4, \5, \6, \7, \8/' >\
# client/build/results/jmh/res-1.csv

# without GC
# grep -v churn client/build/results/jmh/human.txt | grep Iteration -A 7 |\
# grep -v Warmup | xargs -L 7 |\
# sed -r 's/Iteration (.*): (.*\..*) ms\/op memory\.total: (.*\..*) B memory\.usage: (.*\..*) B ·gc\.alloc\.rate: (.*\..*) MB\/sec ·gc\.alloc\.rate\.norm: (.*\..*) B\/op ·gc\.count: (.*\..*) counts ·gc\.time: (.*\..*) ms.*/\1, \2, \3, \4, \5, \6, \7, \8/' >\
# client/build/results/jmh/res-1.csv

# delay only
sed 's/≈ 10⁻³/0.001/g' client/build/results/jmh/human.txt > client/build/results/jmh/human-clean.txt
sed 's/≈ 10⁻⁴/0.001/g' client/build/results/jmh/human-clean.txt > client/build/results/jmh/human.txt
grep -v churn client/build/results/jmh/human.txt | grep Iteration -A 2 |\
grep -v Warmup | xargs -L 2 |\
sed -r 's/Iteration (.*): (.*\..*) ms\/op -: (.*) ms/\1, \2/' >\
client/build/results/jmh/res-1.csv