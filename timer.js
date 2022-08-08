const prettytime = require('prettytime')
class Timer {
    startTimer() {
        this.start = Date.now();
        return this;
    }

    endTimer() {
        const ELAPSED = Date.now() - this.start;
        return prettytime(ELAPSED, { decimals: 2 });
    }
}

module.exports = Timer;