# Deployment Guide

## Table of Contents

1. [Deployment user](#1-deployment-user)
2. [Install quadlets](#2-install-quadlets)
3. [Configure environment](#3-configure-environment)
4. [Start services](#4-start-services)
5. [Update services](#5-update-services)

## 1. Deployment user

Create a dedicated `bookshelf` service account and enable systemd linger so its user services survive logout:

```bash
sudo useradd -m bookshelf
sudo loginctl enable-linger bookshelf
sudo usermod -aG systemd-journal bookshelf
```

Admins can switch to the service account with:

```bash
sudo su - bookshelf
```

Using `sudo su - bookshelf` (rather than `sudo -u bookshelf -s`) gives a full login shell which correctly sets `XDG_RUNTIME_DIR` - required for `systemctl --user` commands to work.
Add it to the bookshelf account's `~/.bashrc` as a safety net:

```bash
echo 'export XDG_RUNTIME_DIR=/run/user/$(id -u)' >> /home/bookshelf/.bashrc
```

______________________________________________________________________

## 2. Install quadlets

Quadlet reads `.container`, `.network`, and `.volume` files from
`~/.config/containers/systemd/` and generates systemd user units automatically.
Copy the quadlet files (found at [repo](https://codefloe.com/buriedincode/Bookshelf/src/branch/main/.podman)) to `/home/bookshelf/.config/containters/systemd`.
After all the files are copied load the reload the systemd daemon

```bash
systemctl --user daemon-reload
```

Verify the units were generated without errors:

```bash
systemctl --user list-unit-files 'bookshelf-*'
```

______________________________________________________________________

## 3. Configure environment

### Settings

Before starting the service, create the `settings.properties` file in the volume's config directory.
First, identify the volume's mount point on the host:

```bash
podman volume inspect bookshelf --format '{{ .Mountpoint }}'
```

Then write your settings into that directory:

```bash
VOLUME_PATH=$(podman volume inspect bookshelf --format '{{ .Mountpoint }}')
echo 'website.host=0.0.0.0' >> "$VOLUME_PATH/settings.properties"
```

> **Timezone:** The container defaults to `TZ=Pacific/Auckland`.
> If you are in a different timezone, update the `Environment=TZ=` line in `bookshelf.container` before starting the service, then run `systemctl --user daemon-reload` to pick up the change.

______________________________________________________________________

## 4. Start services

Start services:

```bash
systemctl --user start bookshelf
```

The container services start automatically on boot without needing `systemctl enable` - the `WantedBy=default.target` line in each `.container` file instructs the Quadlet generator to wire this up at startup.

The first start will pull the image automatically - no manual `podman pull` is needed.
Once running, the app will be available at `http://<host>:25710`.

Verify the service is running:

```bash
systemctl --user status bookshelf
podman logs -f bookshelf
```

______________________________________________________________________

## 5. Update services

```bash
sudo su - bookshelf
podman auto-update
```

`podman auto-update` checks the registry for updated images (enabled via `AutoUpdate=registry` in the container file) and restarts any containers with new versions.
