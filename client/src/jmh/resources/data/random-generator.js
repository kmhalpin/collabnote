const fs = require('fs');
const crypto = require('crypto')

/**
 * 
 * @param {number} index 
 * @param {boolean} isRemove 
 * @param {string} character 
 */
const newOps = (index, isRemove, character) => ({
    index, isRemove, character
})

/**
 * 
 * @param {number} min 
 * @param {number} max 
 */
const randomInt = (min, max) => Math.floor((Math.random()) * (max - min + 1)) + min;

const N = 11000
const insertN = 6000
const data = crypto.randomBytes(insertN).toString('hex').substring(0, insertN).split("");

let inserted = 0;
let operations = [];
for (let i = 0; i < N; i++) {
    if (inserted != 0 && Math.random() < 0.5) {
        operations.push(newOps(randomInt(0, inserted + 1), true, null))
        inserted--;
    } else {
        operations.push(newOps(randomInt(0, inserted + 1), false, data[inserted]))
        inserted++;
    }
}

fs.writeFileSync('client/src/jmh/resources/data/random-data.json', JSON.stringify({ input: operations }))