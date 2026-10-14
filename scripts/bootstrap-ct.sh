#!/bin/bash
# Run as root INSIDE the new dedicated Debian CT, never on the PVE host.
set -euo pipefail
if [[ $(hostname) != malha-stream-build ]]; then
    echo 'Refusing bootstrap outside malha-stream-build' >&2
    exit 1
fi
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends openjdk-21-jdk-headless git gh curl ca-certificates unzip zip \
    cmake ninja-build build-essential adb python3 python3-venv python3-pip python3-matplotlib \
    python3-opencv python3-coverage openssh-server
if ! id malha >/dev/null 2>&1; then adduser --disabled-password --gecos '' malha; fi
install -d -m 700 -o malha -g malha /home/malha/.ssh
if [[ -f /root/.ssh/authorized_keys ]]; then
    install -m 600 -o malha -g malha /root/.ssh/authorized_keys /home/malha/.ssh/authorized_keys
fi
cat > /etc/ssh/sshd_config.d/50-malha.conf <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
EOF
sshd -t
systemctl restart ssh
# LXC already supplies /run and /dev. These optional systemd 257 mounts failed
# in this unprivileged CT; /tmp and /run/lock work as directories without nesting.
install -d -m 1777 /tmp /run/lock
systemctl mask tmp.mount run-lock.mount dev-mqueue.mount
systemctl reset-failed
cat > /etc/profile.d/malha-android.sh <<'EOF'
export ANDROID_HOME=/home/malha/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF
install -d -o malha -g malha /home/malha/.gradle
cat > /home/malha/.gradle/gradle.properties <<'EOF'
org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
org.gradle.daemon=false
EOF
chown malha:malha /home/malha/.gradle/gradle.properties
echo 'Native prerequisites installed; user malha has no sudo grants.'
