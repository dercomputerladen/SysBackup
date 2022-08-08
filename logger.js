const ansiColors = require('ansi-colors');

module.exports = {
    info(message) {
        console.log(ansiColors.bold.blue("🛈") + " " + ansiColors.bold(message));
    },
    warn(message) {
        console.log(ansiColors.bold.yellow("⚠") + " " + ansiColors.bold(message));
    },
    error(message) {
        console.log(ansiColors.red.red("✖") + " " + ansiColors.bold(message));
    },
    success(message) {
        console.log(ansiColors.green("✔") + " " + ansiColors.bold(message));
    }
}

