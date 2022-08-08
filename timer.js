const prettytime = require('prettytime')
class Timer {
    startTimer() {
        this.start = Date.now();
        return this;
    }

    endTimer() {
        return prettytime(Date.now() - this.start, { decimals: 2 });
    }
}

module.exports = Timer;