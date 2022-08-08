const PROMPTS = require('prompts');
const PATH = require('path');
const FS = require('fs');
const GLOB = require('tiny-glob');
const LOGGER = require("./logger");
const ORA = require('ora');
const BACKUP_ARCHIVE = require("./archiver");
const MYSQLDUMP = require("mysqldump");

const CURRENT_FOLDER = __dirname.startsWith("/snapshot") ? PATH.dirname(process.env._) : __dirname;
const TASKS_DIRECTORY = PATH.join(CURRENT_FOLDER, 'tasks');
const BACKUPS_DIRECTORY = PATH.join(CURRENT_FOLDER, 'backups');
const TEMP_DIR = PATH.join(CURRENT_FOLDER, '.temp');

const run = async (programArgs) => {
    switch (programArgs.action) {
        case "exit":
            LOGGER.info("exiting...");
            break;
        case "backup": {
            let taskName = programArgs.filename ? programArgs.filename + ".json" : "";
            const AUTO_COMPLETE_ARRAY = new Array();
            const FILES = await GLOB(PATH.join('*.json'), {
                filesOnly: true,
                cwd: TASKS_DIRECTORY
            });

            for (const FILE of FILES) {
                AUTO_COMPLETE_ARRAY.push({
                    title: FILE,
                    value: FILE,
                })
            }

            if (taskName === undefined || taskName === "") {
                const PROMPT = await PROMPTS({
                    type: "autocomplete",
                    name: "filename",
                    message: "Select a Task file to use",
                    choices: AUTO_COMPLETE_ARRAY,
                    validate: value => FILES.includes(value + ".json") ? true : "Task file does not exist, please specify a valid task file."
                });

                taskName = PROMPT.filename;
            }

            if (taskName === undefined)
                return;

            if (!FS.existsSync(PATH.join(TASKS_DIRECTORY, taskName))) {
                LOGGER.error("task file does not exist");
                process.exit(1);
            }

            const TASK_FILE_CONTENTS = JSON.parse(FS.readFileSync(PATH.join(TASKS_DIRECTORY, taskName), 'utf8'));
            const VALID_TASK_FILE = validate(TASK_FILE_CONTENTS);
            if (VALID_TASK_FILE.length > 0) {
                LOGGER.error("task file is not valid, " + VALID_TASK_FILE.length + " errors found");
                LOGGER.info("check with '" + process.argv0 + " checkTaskConf " + PATH.parse(taskName).name + "'");
                process.exit(1);
            }
            LOGGER.success("task file is valid");
            const TIMER = new (require('./timer'))().startTimer();
            const SPINNER = ORA('initialization...').start();
            if (!FS.existsSync(PATH.join(BACKUPS_DIRECTORY, PATH.parse(taskName).name)))
                await FS.promises.mkdir(PATH.join(BACKUPS_DIRECTORY, PATH.parse(taskName).name));
            else {
                if (TASK_FILE_CONTENTS.vacuum.enabled) {
                    SPINNER.text = "cleaning up old backups...";
                    const ALL_FILES = FS.readdirSync(PATH.join(BACKUPS_DIRECTORY, PATH.parse(taskName).name));
                    const CURRENT_DATE = Date.now();
                    for (const FILE of ALL_FILES) {
                        const FILE_DATE = new Date(FS.statSync(PATH.join(BACKUPS_DIRECTORY, PATH.parse(taskName).name, FILE)).birthtime).getTime();
                        let timeAdd = 0;
                        switch (TASK_FILE_CONTENTS.vacuum.unit) {
                            case "DAYS":
                                timeAdd = TASK_FILE_CONTENTS.vacuum.time * 24 * 60 * 60 * 1000;
                                break;
                            case "HOURS":
                                timeAdd = TASK_FILE_CONTENTS.vacuum.time * 60 * 60 * 1000;
                                break;
                            case "MINUTES":
                                timeAdd = TASK_FILE_CONTENTS.vacuum.time * 60 * 1000;
                                break;
                        }
                        const DELETE_DATE = new Date(FILE_DATE + timeAdd);
                        if (DELETE_DATE.getTime() < CURRENT_DATE) {
                            await FS.unlinkSync(PATH.join(BACKUPS_DIRECTORY, PATH.parse(taskName).name, FILE));
                        }
                    }
                }
            }

            const UNREPLACED_FILENAME = TASK_FILE_CONTENTS.general.outputFile;
            const REPLACED_FILENAME = UNREPLACED_FILENAME.replace("{date}", require('moment')().format(TASK_FILE_CONTENTS.general.dateFormat))
                .replace("{taskName}", PATH.parse(taskName).name);

            const DB_FILE = PATH.join(TEMP_DIR, require('crypto').randomBytes(16).toString('hex') + ".sql");
            const ARCHIVE = new BACKUP_ARCHIVE(PATH.join(BACKUPS_DIRECTORY, PATH.parse(taskName).name,
                REPLACED_FILENAME + ".tar.gz"),
                TASK_FILE_CONTENTS.filesystem.targets,
                TASK_FILE_CONTENTS.general.gzip,
                TASK_FILE_CONTENTS.general.gzipLevel);

            ARCHIVE.eventEmitter.on('progress', (progressInfo) => {
                const TOTAL = ARCHIVE.totalFiles;
                const PROCESSED = progressInfo.entries.processed;
                const PERCENTAGE = Math.round((PROCESSED / TOTAL) * 100);
                SPINNER.text = `Compressing files... ${PROCESSED}/${TOTAL}(${PERCENTAGE}%)`;
            });

            ARCHIVE.eventEmitter.on('finish', async () => {
                if (TASK_FILE_CONTENTS.mysql.enabled) {
                    if (DB_FILE !== undefined && FS.existsSync(DB_FILE))
                        await FS.unlinkSync(DB_FILE);
                }
                SPINNER.succeed("backup complete, took " + TIMER.endTimer());
            });

            if (TASK_FILE_CONTENTS.mysql.enabled) {
                SPINNER.text = `Dumping Database "${TASK_FILE_CONTENTS.mysql.database}"...`;
                try {
                    await MYSQLDUMP({
                        connection: {
                            host: TASK_FILE_CONTENTS.mysql.host,
                            port: TASK_FILE_CONTENTS.mysql.port,
                            user: TASK_FILE_CONTENTS.mysql.user,
                            password: TASK_FILE_CONTENTS.mysql.password,
                            database: TASK_FILE_CONTENTS.mysql.database
                        },
                        dumpToFile: DB_FILE
                    });
                } catch (err) {
                    LOGGER.error(err);
                    await FS.unlinkSync(DB_FILE);
                }
            }

            SPINNER.text = "Adding Files...";
            await ARCHIVE.start();

            if (TASK_FILE_CONTENTS.mysql.enabled) {
                if (FS.existsSync(DB_FILE)) {
                    SPINNER.text = "Adding Database...";
                    await ARCHIVE.add(DB_FILE);
                }
            }

            ARCHIVE.pack();
            break;
        }
        case "checkTaskConf": {
            let taskName = programArgs.filename ? programArgs.filename + ".json" : "";
            const AUTO_COMPLETE_ARRAY = new Array();
            const FILES = await GLOB(PATH.join('*.json'), {
                filesOnly: true,
                cwd: TASKS_DIRECTORY
            });

            for (const FILE of FILES) {
                AUTO_COMPLETE_ARRAY.push({
                    title: FILE,
                    value: FILE,
                })
            }

            if (taskName === undefined || taskName === "") {
                const PROMPT = await PROMPTS({
                    type: "autocomplete",
                    name: "filename",
                    message: "Select a Task file to check",
                    choices: AUTO_COMPLETE_ARRAY,
                    validate: value => FILES.includes(value + ".json") ? true : "Task file does not exist, please specify a valid task file."
                });

                taskName = PROMPT.filename;
            }

            if (taskName === undefined)
                return;

            const TASK_FILE = PATH.join(TASKS_DIRECTORY, taskName);
            if (!FS.existsSync(TASK_FILE)) {
                LOGGER.error("Task file does not exist");
                return;
            }
            const TASK_FILE_CONTENTS = await FS.readFileSync(TASK_FILE, 'utf8');
            const TASK_FILE_TO_CHECK = JSON.parse(TASK_FILE_CONTENTS);

            const VALIDATION_ERRORS = validate(TASK_FILE_TO_CHECK);
            if (VALIDATION_ERRORS.length > 0) {
                LOGGER.error("task file is not valid, " + VALIDATION_ERRORS.length + " errors found");
                for (const ERROR of VALIDATION_ERRORS) LOGGER.error(ERROR);
            } else {
                LOGGER.success("Task file is valid!");
            }

            break;
        }
        case "generateTaskConf": {
            let taskName = programArgs.filename ? programArgs.filename : "";

            if (taskName === undefined || taskName === "") {
                const PROMPT = await PROMPTS({
                    type: "text",
                    name: "filename",
                    message: "Define a name for the new task file",
                    validate: value => value.length <= 0 || value == "" ? "Please specify a valid file name" : true
                });

                taskName = PROMPT.filename;

                const TASK_FILE_PATH = PATH.join(TASKS_DIRECTORY, taskName + ".json");
                if (await FS.existsSync(TASK_FILE_PATH)) {
                    const PROMPT_2 = await PROMPTS({
                        type: "toggle",
                        name: "overwrite",
                        message: "A task file with the same name already exists. Do you want to overwrite it?",
                        active: 'yes',
                        inactive: 'no',
                        initial: false
                    });

                    if (!PROMPT_2.overwrite) {
                        LOGGER.info("exiting...");
                        return;
                    }
                }
            }

            if (taskName === undefined)
                return;

            const TASK_FILE_PATH = PATH.join(TASKS_DIRECTORY, taskName + ".json");

            const TASK_CONFIG = {
                "general": {
                    "dateFormat": "yyyy-MM-DD HH-mm-ss",
                    "outputFile": "{date} - {taskName}",
                    "gzip": true,
                    "gzipLevel": 6
                },
                "vacuum": {
                    "enabled": true,
                    "unit": "DAYS",
                    "time": 7
                },
                "mysql": {
                    "enabled": true,
                    "host": "localhost",
                    "port": 3306,
                    "user": "",
                    "password": "",
                    "database": "",
                },
                "filesystem": {
                    "enabled": true,
                    "targets": [
                        "/home/magento/",
                        "/home/test/testfile.txt"
                    ]
                }
            };
            try {
                const SAVE_FILE = FS.createWriteStream(TASK_FILE_PATH);
                SAVE_FILE.write(JSON.stringify(TASK_CONFIG, null, 4));
                SAVE_FILE.end();
                LOGGER.success("Task file \"" + PATH.basename(TASK_FILE_PATH) + "\" saved successfully");
            } catch (err) {
                LOGGER.error(err);
            }
            break;
        }
        default:
            LOGGER.warn("Unknown action.");
            cli(true);
            return;
    }
}

const validate = (taskConfig) => {
    const ERRORS = new Array();

    if (taskConfig.general && typeof taskConfig.general === "object") {
        if (taskConfig.general.dateFormat && typeof taskConfig.general.dateFormat === "string") {
            const DATE_FORMAT = taskConfig.general.dateFormat;
            const DATE_FORMAT_REGEX = /d{1,4}|D{3,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|W{1,2}|[LlopSZN]|"[^"]*"|'[^']*'/g;
            if (!DATE_FORMAT_REGEX.test(DATE_FORMAT)) {
                ERRORS.push("general.dateFormat is not a valid date format");
            }
        } else {
            ERRORS.push("general.dateFormat is not defined or is not a string");
        }
        if (taskConfig.general.outputFile && typeof taskConfig.general.outputFile === "string") {
            const OUTPUT_FILE = taskConfig.general.outputFile;
            const OUTPUT_FILE_REGEX = /^([^.]+)$/g;
            if (!OUTPUT_FILE_REGEX.test(OUTPUT_FILE)) {
                ERRORS.push("general.outputFile is not a valid output file name, maybe you added a file extension?");
            }
        } else {
            ERRORS.push("general.outputFile is not defined or is not a string");
        }
        if (typeof taskConfig.general.gzip === "boolean") {
            if (taskConfig.general.gzip) {
                if (!taskConfig.general.gzipLevel && typeof taskConfig.general.gzipLevel !== "number") {
                    ERRORS.push("general.gzipLevel is not defined or is not a number");
                }
            }
        } else {
            ERRORS.push("general.gzip is not defined or is not a boolean");
            ERRORS.push("general.gzipLevel is not defined or is not a number");
        }
    } else {
        ERRORS.push("general section is missing or not an object");
        ERRORS.push("general.dateFormat is not defined or is not a string");
        ERRORS.push("general.outputFile is not defined or is not a string");
    }

    if (taskConfig.vacuum && typeof taskConfig.vacuum === "object") {
        if (typeof taskConfig.vacuum.enabled !== "boolean") {
            ERRORS.push("vacuum.enabled is not defined or is not a boolean");
        } else if (taskConfig.vacuum.enabled) {

            if (taskConfig.vacuum.unit && typeof taskConfig.vacuum.unit === "string") {
                const UNIT = taskConfig.vacuum.unit;
                const UNIT_REGEX = /^(DAYS|HOURS|MINUTES)$/g;
                if (!UNIT_REGEX.test(UNIT)) {
                    ERRORS.push("vacuum.unit is not a valid unit, please use DAYS, HOURS or MINUTES");
                }
            } else {
                ERRORS.push("vacuum.unit is not defined or is not a string");
            }
            if (taskConfig.vacuum.time && typeof taskConfig.vacuum.time === "number") {
                const TIME = taskConfig.vacuum.time;
                if (TIME < 1) {
                    ERRORS.push("vacuum.time is not a valid time, please use a number greater than 0");
                }
            } else {
                ERRORS.push("vacuum.time is not defined or is not a number");
            }
        }
    } else {
        ERRORS.push("vacuum section is missing or not an object");
        ERRORS.push("vacuum.enabled is not defined or is not a boolean");
        ERRORS.push("vacuum.unit is not defined or is not a string");
        ERRORS.push("vacuum.time is not defined or is not a number");
    }

    if (taskConfig.mysql && typeof taskConfig.mysql === "object") {
        if (typeof taskConfig.mysql.enabled !== "boolean") {
            ERRORS.push("mysql.enabled is not defined or is not a boolean");
        } else if (taskConfig.mysql.enabled) {
            if (!taskConfig.mysql.host || typeof taskConfig.mysql.host !== "string") {
                ERRORS.push("mysql.host is not defined or is not a string");
            }
            if (!taskConfig.mysql.port || typeof taskConfig.mysql.port !== "number") {
                ERRORS.push("mysql.port is not defined or is not a number");
            }
            if (!taskConfig.mysql.user || typeof taskConfig.mysql.user !== "string") {
                ERRORS.push("mysql.user is not defined or is not a string");
            }
            if (typeof taskConfig.mysql.password !== "string") {
                ERRORS.push("mysql.password is not defined or is not a string");
            }
            if (!taskConfig.mysql.database || typeof taskConfig.mysql.database !== "string") {
                ERRORS.push("mysql.database is not defined or is not a string");
            }
        }
    } else {
        ERRORS.push("mysql section is missing or not an object");
        ERRORS.push("mysql.host is not defined or is not a string");
        ERRORS.push("mysql.port is not defined or is not a number");
        ERRORS.push("mysql.user is not defined or is not a string");
        ERRORS.push("mysql.password is not defined or is not a string");
        ERRORS.push("mysql.database is not defined or is not a string");
    }

    if (taskConfig.filesystem && typeof taskConfig.filesystem === "object") {
        if (typeof taskConfig.filesystem.enabled !== "boolean") {
            ERRORS.push("filesystem.enabled is not defined or is not a boolean");
        } else if (taskConfig.filesystem.enabled) {
            if (taskConfig.filesystem.targets && typeof taskConfig.filesystem.targets === "object") {
                const TARGETS = taskConfig.filesystem.targets;
                if (TARGETS.length < 1) {
                    ERRORS.push("filesystem.targets is not defined or is not an array");
                } else {
                    for (const target of TARGETS) {
                        if (!target || typeof target !== "string") {
                            ERRORS.push("filesystem.targets[] is not defined or is not a valid path");
                        }
                    }
                }
            } else {
                ERRORS.push("filesystem.targets is not defined or is not an array");
            }
        }
    } else {
        ERRORS.push("filesystem section is missing or not an object");
        ERRORS.push("filesystem.enabled is not defined or is not a boolean");
        ERRORS.push("filesystem.targets is not defined or is not an array");
    }

    return ERRORS;
}

const cli = async (forcePrompt) => {
    const FIRST_RUN = !FS.existsSync(TASKS_DIRECTORY);
    try {
        if (!FS.existsSync(TASKS_DIRECTORY)) {
            LOGGER.info("tasks directory does not exist, creating it...");
            await FS.mkdirSync(TASKS_DIRECTORY);
        }
        if (!FS.existsSync(BACKUPS_DIRECTORY)) {
            LOGGER.info("backups directory does not exist, creating it...");
            await FS.mkdirSync(BACKUPS_DIRECTORY);
        }
        if (!FS.existsSync(TEMP_DIR)) {
            await FS.mkdirSync(TEMP_DIR);
        }
    } catch (err) {
        LOGGER.error(err);
    }

    if (FIRST_RUN) {
        const PROMPT = await PROMPTS({
            type: "toggle",
            name: "createTask",
            message: "This is the first time you run this script, do you want to create a new task?",
            active: 'yes',
            inactive: 'no',
            initial: false
        });

        if (PROMPT.createTask) {
            run({ action: "generateTaskConf" });
        } else cli(true);


    } else {
        const ARGS = process.argv.slice(2);
        let action = ARGS[0];
        let fileName = ARGS[1];

        if (action === undefined || forcePrompt) {
            const PROMPT = await PROMPTS({
                type: "select",
                name: "action",
                message: "What would you like to do?",
                choices: [
                    { title: "Create a new task file", value: "generateTaskConf" },
                    { title: "Check a task file", value: "checkTaskConf" },
                    { title: "Perform a Backup", value: "backup" },
                    { title: "Exit", value: "exit" }
                ],
            });
            action = PROMPT.action;
        }

        const programArgs = {
            action,
            filename: fileName
        }

        run(programArgs);
    }
}

cli(false);

