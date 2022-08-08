# SysBackup

This branch is a rewrite of the [previous in Java Developed branch](https://git.ez-pp.farm/HorizonCode/SysBackup/src/branch/archived), rewritten in NodeJS.

## Setup

### Requirements
- NodeJS >= 14

You can either: [Build a executeable](#build-a-executeable) or [Runing from Source](#running-from-source)

### Build a executeable

#### Extra Requirements
- pkg

#### Aquire SysBackup
```bash
git clone https://git.ez-pp.farm/HorizonCode/SysBackup
```

#### Installing Dependencies
```bash
cd /path/to/sysbackupclone/
# with NPM
npm i
# or with PNPM
pnpm i
# or with Yarn
yarn install

# Install pkg global
npm i pkg -g
```

#### Building the executeable
```bash
# with NPM
npm run pkg
# or with PNPM
pnpm run pkg
# or with Yarn
yarn run pkg
```

Your builded binary executeable should be in bin/

### Running from Source

#### Aquire SysBackup
```bash
git clone https://git.ez-pp.farm/HorizonCode/SysBackup
```

#### Running
```bash
cd /path/to/sysbackupclone/

node index.js
```


## Contributing
Please read our [CONTRIBUTING.md](https://git.ez-pp.farm/HorizonCode/SysBackup/src/branch/master/CONTRIBUTING.md) to read how to contribute!