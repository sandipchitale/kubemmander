<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Kubemmander Changelog

## [Unreleased]

### Changed

- Modernized the build to the current IntelliJ Platform plugin project standard (Gradle 9.5, IntelliJ Platform Gradle Plugin 2.17)
- Raised the minimum supported IDE version to 2025.2
- Upgraded fabric8 kubernetes-client to 7.3.1

## [1.18]

### Added

- Helm Get command
- Helm diff

### Features

- Filter by selected namespaces
- Show Helm releases
- Include instances checkbox
- Support connect, disconnect, reconnect action
- Wait cursor
- Get command
- Describe command
- Load command (get -o yaml)
- Documentation command

### Known Issues

- get and describe commands do not work with the New Terminal (Beta)
