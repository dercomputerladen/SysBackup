const fs = require('fs');
const path = require('path');
const { EventEmitter } = require('events');

class Archiver {
    constructor(backupPath, backupPaths) {
        this.backupPath = backupPath;
        this.backupPaths = backupPaths;
        this.eventEmitter = new EventEmitter();
        this.archive = require('archiver')('tar', {
            zlib: { level: 9 }
        });
        this.totalFiles = 0;
    }

    start() {
        const EVENT_EMITTER = this.eventEmitter;
        const ARCHIVE_LOCATION = path.join(this.backupPath);
        const OUTPUT = fs.createWriteStream(ARCHIVE_LOCATION);

        OUTPUT.on('close', function () {
            EVENT_EMITTER.emit('finish');
        });

        OUTPUT.on('end', function () {
            console.log('Data has been drained');
        });

        this.archive.on('warning', function (err) {
            if (err.code === 'ENOENT') console.log(err);
            else throw err;
        });

        this.archive.on('error', function (err) {
            throw err;
        });

        this.archive.on('progress', function (progressInfo) {
            EVENT_EMITTER.emit('progress', progressInfo);
        });

        // pipe archive data to the file
        this.archive.pipe(OUTPUT);

        for (const ITEM of this.backupPaths) {
            if (!fs.existsSync(ITEM)) continue;
            const IS_DIR = fs.lstatSync(ITEM).isDirectory();
            if (IS_DIR) {
                this.archive.directory(ITEM, path.basename(ITEM));
                const COUNTED = require('count-files-dirs').countSync(ITEM);
                this.totalFiles += COUNTED.fileCount + COUNTED.dirCount;
            } else {
                this.archive.file(ITEM, { name: path.basename(ITEM) });
                this.totalFiles++;
            }
        }
    }

    add(item) {
        if (!fs.existsSync(item)) return;
        const IS_DIR = fs.lstatSync(item).isDirectory();
        if (IS_DIR) {
            this.archive.directory(item, path.basename(item));
            const COUNTED = require('count-files-dirs').countSync(item);
            this.totalFiles += COUNTED.fileCount + COUNTED.dirCount;
        } else {
            this.archive.file(item, { name: path.basename(item) });
            this.totalFiles++;
        }
    }

    pack() {
        this.archive.finalize();
    }
}

module.exports = Archiver;