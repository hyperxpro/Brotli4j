#!/bin/bash

sudo dpkg --add-architecture "$1"
sudo rm -f /etc/apt/sources.list /etc/apt/sources.list.d/ubuntu.sources
if [[ $2 != true ]]; then
    {
        echo "deb [arch=amd64,$1] http://azure.archive.ubuntu.com/ubuntu/ $(lsb_release -sc) main restricted universe multiverse" ;
        echo "deb [arch=amd64,$1] http://azure.archive.ubuntu.com/ubuntu/ $(lsb_release -sc)-updates main restricted universe multiverse" ;
        echo "deb [arch=amd64,$1] http://azure.archive.ubuntu.com/ubuntu/ $(lsb_release -sc)-security main restricted universe multiverse"
    } | sudo tee /etc/apt/sources.list.d/ubuntu.list
else
    {
        echo "deb [arch=amd64] http://azure.archive.ubuntu.com/ubuntu/ $(lsb_release -sc) main restricted universe multiverse" ;
        echo "deb [arch=amd64] http://azure.archive.ubuntu.com/ubuntu/ $(lsb_release -sc)-updates main restricted universe multiverse" ;
        echo "deb [arch=amd64] http://azure.archive.ubuntu.com/ubuntu/ $(lsb_release -sc)-security main restricted universe multiverse"
    } | sudo tee /etc/apt/sources.list.d/ubuntu.list
fi
if [[ $2 == true ]]; then
    {
        echo "deb [arch=$1] http://azure.ports.ubuntu.com/ubuntu-ports/ $(lsb_release -sc) main restricted universe multiverse" ;
        echo "deb [arch=$1] http://azure.ports.ubuntu.com/ubuntu-ports/ $(lsb_release -sc)-updates main restricted universe multiverse" ;
        echo "deb [arch=$1] http://azure.ports.ubuntu.com/ubuntu-ports/ $(lsb_release -sc)-security main restricted universe multiverse"
    } | sudo tee /etc/apt/sources.list.d/ubuntu-ports.list
fi

sudo apt-get update -y
sudo apt-get --fix-broken install

sudo NEEDRESTART_SUSPEND=true apt-get install -y "qemu-system-$3" "qemu-user" "qemu-utils" \
    "libc6:amd64" "libc6:$1" "libstdc++6:amd64" "libstdc++6:$1" "libtinfo6:amd64" "libtinfo6:$1" \
    "libatomic1:amd64" "libatomic1:$1" "zlib1g:amd64" "zlib1g:$1"

cd /opt
sudo apt-get download "bash:$1"
sudo mkdir -p bashdata
sudo ar x bash_*.deb --output=bashdata
sudo tar xf bashdata/data.tar.* -C bashdata
if [[ -f bashdata/usr/bin/bash ]]; then
    sudo mv bashdata/usr/bin/bash /opt/bash
elif [[ -f bashdata/bin/bash ]]; then
    sudo mv bashdata/bin/bash /opt/bash
fi
sudo rm -r bash_*.deb bashdata