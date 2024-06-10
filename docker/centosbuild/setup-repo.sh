#!/bin/bash

(cat <<EOF
[base]
name=CentOS-\$releasever - Base
baseurl=http://archive.kernel.org/centos-vault/6.10/os/\$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6

[updates]
name=CentOS-\$releasever - Updates
baseurl=http://archive.kernel.org/centos-vault/6.10/os/\$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6

[extras]
name=CentOS-\$releasever - Extras
baseurl=http://archive.kernel.org/centos-vault/6.10/os/\$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6
EOF
) | tee /etc/yum.repos.d/CentOS-Base.repo